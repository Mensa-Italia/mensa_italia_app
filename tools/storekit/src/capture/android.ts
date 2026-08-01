import { existsSync } from "node:fs";
import { readFile, statfs, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { homedir } from "node:os";
import { join } from "node:path";
import { regionalTag } from "../config.ts";
import { androidTools, credentials, gradleEnv, type AndroidTools } from "../env.ts";
import { log } from "../log.ts";
import { REPO_ROOT, WORK_ROOT, ensureParent, rawPath } from "../paths.ts";
import { run, runBinary, runOrThrow, sleep, spawnBackground, waitFor } from "../proc.ts";
import type { AndroidDevice, Scene, StorekitConfig } from "../types.ts";
import { CAPTURE_FREE_MB, assertFreeSpace } from "../disk.ts";

/** First emulator console port storekit uses. Must be even and ≥ 5554. */
const BASE_PORT = 5584;

interface Emulator {
  serial: string;
  port: number;
  stop: () => Promise<void>;
}

// ─── AVD lifecycle ───────────────────────────────────────────────────────────

async function listAvds(tools: AndroidTools): Promise<string[]> {
  const { stdout } = await run(tools.emulator, ["-list-avds"], { env: tools.env });
  return stdout
    .split("\n")
    .map((s) => s.trim())
    .filter((s) => s && !s.startsWith("INFO"));
}

async function ensureSystemImage(tools: AndroidTools, pkg: string): Promise<void> {
  const relative = pkg.replace(/;/g, "/");
  if (existsSync(join(tools.sdk, relative))) return;
  log.info(`installing ${pkg} (this downloads a few hundred MB)`);
  await runOrThrow(tools.sdkmanager, [`--sdk_root=${tools.sdk}`, pkg], {
    env: tools.env,
    inherit: true,
    input: "y\n".repeat(40),
    timeoutMs: 3_600_000,
  });
}

/** Creates the AVD for `device` when it does not exist yet. */
export async function ensureAvd(device: AndroidDevice): Promise<void> {
  const tools = androidTools();
  if ((await listAvds(tools)).includes(device.avd)) return;

  log.info(`creating AVD "${device.avd}" (${device.avdDevice})`);
  await ensureSystemImage(tools, device.systemImage);
  await runOrThrow(
    tools.avdmanager,
    [
      "--silent",
      "create",
      "avd",
      "--name",
      device.avd,
      "--package",
      device.systemImage,
      "--device",
      device.avdDevice,
      "--force",
    ],
    { env: tools.env, input: "no\n", timeoutMs: 300_000 },
  );
}

/**
 * Free space the emulator insists on before creating a userdata partition:
 * 6 GiB plus the 20% headroom it reserves on top.
 *
 * Not negotiable from our side: emulator 36.x ignores both
 * `disk.dataPartition.size` in `config.ini` (it rewrites the file from the
 * hardware profile at startup) and the `-partition-size` flag.
 */
const EMULATOR_FIRST_BOOT_FREE_MB = 7_380;

async function assertEnoughDisk(device: AndroidDevice): Promise<void> {
  const avdRoot = join(homedir(), ".android", "avd");

  // Once the AVD has booted at least once its userdata image already exists,
  // and the emulator reuses it instead of allocating a fresh one. Blocking
  // that case would reject a setup that actually works, and a boot failure is
  // now reported immediately anyway, so let the emulator decide.
  if (existsSync(join(avdRoot, `${device.avd}.avd`, "userdata-qemu.img"))) return;

  await assertFreeSpace(
    avdRoot,
    EMULATOR_FIRST_BOOT_FREE_MB,
    `the first boot of ${device.avd}`,
  );
}

async function bootCompleted(tools: AndroidTools, serial: string): Promise<boolean> {
  const { stdout, code } = await run(
    tools.adb,
    ["-s", serial, "shell", "getprop", "sys.boot_completed"],
    { env: tools.env },
  );
  return code === 0 && stdout.trim() === "1";
}

/** Boots the emulator for `device` and waits until the launcher is usable. */
export async function startEmulator(
  device: AndroidDevice,
  index: number,
  headless: boolean,
): Promise<Emulator> {
  const tools = androidTools();
  await ensureAvd(device);
  await assertEnoughDisk(device);

  const port = BASE_PORT + index * 2;
  const serial = `emulator-${port}`;

  // Reuse an emulator already listening on that port (e.g. a re-run).
  if (await bootCompleted(tools, serial)) {
    log.info(`reusing running emulator ${serial}`);
    return { serial, port, stop: async () => {} };
  }

  const args = [
    "-avd",
    device.avd,
    "-port",
    String(port),
    "-no-snapshot",
    "-no-boot-anim",
    "-netdelay",
    "none",
    "-netspeed",
    "full",
    "-gpu",
    "swiftshader_indirect",
    "-camera-back",
    "none",
    "-camera-front",
    "none",
  ];
  if (headless) args.push("-no-window");

  const logFile = join(WORK_ROOT, `emulator-${device.avd}.log`);
  await ensureParent(logFile);

  log.info(`booting ${device.avd} on port ${port}${headless ? " (headless)" : ""}`);
  const proc = spawnBackground(tools.emulator, args, { env: tools.env, logFile });

  // The emulator can die during startup (no disk for the userdata partition,
  // missing system image, HAXM/HVF trouble). Without this the only symptom is
  // `wait-for-device` blocking for its full timeout, so surface its own log.
  const failIfDead = async () => {
    if (proc.alive()) return;
    const detail = existsSync(logFile)
      ? (await readFile(logFile, "utf8")).split("\n").filter(Boolean).slice(-6).join("\n  ")
      : "(nessun log)";
    throw new Error(`Emulator ${device.avd} exited during boot:\n  ${detail}`);
  };

  await waitFor(
    `${serial} to appear`,
    async () => {
      await failIfDead();
      const { stdout } = await run(tools.adb, ["devices"], { env: tools.env });
      return stdout.includes(serial);
    },
    { timeoutMs: 300_000, intervalMs: 2_000 },
  );
  await waitFor(
    `${serial} to finish booting`,
    async () => {
      await failIfDead();
      return bootCompleted(tools, serial);
    },
    { timeoutMs: 420_000, intervalMs: 2_000 },
  );
  // Dismiss the (unset) keyguard so the launcher is actually on screen.
  await run(tools.adb, ["-s", serial, "shell", "wm", "dismiss-keyguard"], { env: tools.env });

  return {
    serial,
    port,
    stop: async () => {
      await run(tools.adb, ["-s", serial, "emu", "kill"], { env: tools.env });
      proc.kill();
    },
  };
}

// ─── Device preparation ──────────────────────────────────────────────────────

async function adb(serial: string, args: string[]) {
  const tools = androidTools();
  return run(tools.adb, ["-s", serial, ...args], { env: tools.env });
}

async function shell(serial: string, command: string) {
  return adb(serial, ["shell", command]);
}

async function demo(serial: string, params: string[]) {
  await adb(serial, [
    "shell",
    "am",
    "broadcast",
    "-a",
    "com.android.systemui.demo",
    ...params,
  ]);
}

/** Freezes the status bar, kills animations and pre-grants runtime permissions. */
export async function prepareEmulator(
  serial: string,
  config: StorekitConfig,
): Promise<void> {
  for (const scale of [
    "window_animation_scale",
    "transition_animation_scale",
    "animator_duration_scale",
  ]) {
    await shell(serial, `settings put global ${scale} 0.0`);
  }
  await shell(serial, "settings put secure show_ime_with_hard_keyboard 0");

  // Gesture navigation reads cleaner than the 3-button bar in store artwork.
  await shell(
    serial,
    "cmd overlay enable com.android.internal.systemui.navbar.gestural",
  );

  await shell(serial, `cmd uimode night ${config.appearance === "dark" ? "yes" : "no"}`);

  // SystemUI demo mode: the Android equivalent of `simctl status_bar override`.
  await shell(serial, "settings put global sysui_demo_allowed 1");
  await demo(serial, ["-e", "command", "enter"]);
  await demo(serial, [
    "-e",
    "command",
    "clock",
    "-e",
    "hhmm",
    config.statusBar.time.replace(":", "").padStart(4, "0"),
  ]);
  await demo(serial, [
    "-e",
    "command",
    "battery",
    "-e",
    "level",
    String(config.statusBar.batteryPercent),
    "-e",
    "plugged",
    "false",
  ]);
  await demo(serial, [
    "-e",
    "command",
    "network",
    "-e",
    "wifi",
    "show",
    "-e",
    "level",
    String(config.statusBar.wifiBars),
  ]);
  // La radio mobile disegna l'etichetta del tipo di rete ("3G" sull'emulatore),
  // che su un'immagine store legge come datata: teniamo solo il wifi.
  await demo(serial, ["-e", "command", "network", "-e", "mobile", "hide"]);
  await demo(serial, ["-e", "command", "notifications", "-e", "visible", "false"]);

  for (const permission of [
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.CAMERA",
    "android.permission.READ_CALENDAR",
    "android.permission.WRITE_CALENDAR",
  ]) {
    await shell(serial, `pm grant ${config.app.applicationId} ${permission}`);
  }
}

/** Leaves the emulator's status bar back under system control. */
export async function releaseEmulator(serial: string): Promise<void> {
  await demo(serial, ["-e", "command", "exit"]);
}

// ─── Build + install ─────────────────────────────────────────────────────────

export async function buildAndroidApk(
  config: StorekitConfig,
  force: boolean,
): Promise<string> {
  const apk = join(REPO_ROOT, config.app.androidApkPath);
  if (!force && existsSync(apk)) {
    log.info(`reusing ${config.app.androidApkPath}`);
    return apk;
  }
  await log.group("gradle assembleDebug", async () => {
    await runOrThrow(join(REPO_ROOT, "gradlew"), [config.app.androidAssembleTask], {
      cwd: REPO_ROOT,
      env: gradleEnv(),
      inherit: true,
      timeoutMs: 1_800_000,
    });
  });
  if (!existsSync(apk)) {
    throw new Error(`Build finished but ${config.app.androidApkPath} is missing.`);
  }
  return apk;
}

export async function installApk(serial: string, apk: string): Promise<void> {
  const tools = androidTools();
  await runOrThrow(tools.adb, ["-s", serial, "install", "-r", "-t", "-g", apk], {
    env: tools.env,
    timeoutMs: 600_000,
  });
}

// ─── Launch + capture ────────────────────────────────────────────────────────

async function setAppLocale(
  serial: string,
  applicationId: string,
  locale: string,
): Promise<void> {
  const tag = regionalTag(locale);
  const { code } = await shell(
    serial,
    `cmd locale set-app-locales ${applicationId} --user 0 --locales ${tag}`,
  );
  if (code === 0) return;
  // Pre-API-33 fallback: change the system locale and restart the framework.
  log.warn(`per-app locale unsupported, switching the system locale to ${tag}`);
  await adb(serial, ["root"]);
  await shell(serial, `setprop persist.sys.locale ${tag}`);
  await shell(serial, "setprop ctl.restart zygote");
  await sleep(8_000);
  await waitFor(
    "framework restart",
    async () =>
      (await shell(serial, "getprop sys.boot_completed")).stdout.trim() === "1",
    { timeoutMs: 180_000, intervalMs: 2_000 },
  );
}

function launchExtras(scene: Scene | null): string[] {
  // Segnaposto al posto dei dati del socio: le immagini finiscono pubbliche
  // sugli store. Vedi `DemoIdentity` nel modulo condiviso.
  const extras: string[] = ["--es", "mensa_demo_identity", "1"];
  if (scene?.android?.screen) extras.push("--es", "mensa_screen", scene.android.screen);
  const creds = credentials();
  if (creds) {
    extras.push("--es", "mensa_autologin_email", creds.email);
    extras.push("--es", "mensa_autologin_pwd", creds.password);
  }
  return extras;
}

async function launchApp(
  serial: string,
  config: StorekitConfig,
  scene: Scene | null,
): Promise<void> {
  const tools = androidTools();
  await runOrThrow(
    tools.adb,
    [
      "-s",
      serial,
      "shell",
      "am",
      "start",
      "-S",
      "-W",
      "-n",
      `${config.app.applicationId}/it.mensa.app.MainActivity`,
      ...launchExtras(scene),
    ],
    { env: tools.env, timeoutMs: 120_000 },
  );
}

async function stopApp(serial: string, applicationId: string): Promise<void> {
  await shell(serial, `am force-stop ${applicationId}`);
}

async function screencap(serial: string): Promise<Buffer> {
  const tools = androidTools();
  const { code, stdout, stderr } = await runBinary(
    tools.adb,
    ["-s", serial, "exec-out", "screencap", "-p"],
    { env: tools.env },
  );
  if (code !== 0 || stdout.length === 0) {
    throw new Error(`screencap failed on ${serial}: ${stderr.trim() || "empty output"}`);
  }
  return stdout;
}

/** Screenshots until two consecutive frames match, then writes `target`. */
async function captureWhenStable(
  serial: string,
  target: string,
  { settleMs, maxWaitMs, minDwellMs }: CaptureTiming,
): Promise<void> {
  const started = Date.now();
  const deadline = started + maxWaitMs;
  let previous = "";
  let last: Buffer = Buffer.alloc(0);

  while (Date.now() < deadline) {
    await sleep(settleMs);
    last = await screencap(serial);
    const hash = createHash("sha1").update(last).digest("hex");
    // Stability alone is not enough: with animations disabled a splash or a
    // spinner is byte-identical between frames, so an early capture would
    // happily immortalise the loading screen. Hold until the screen has had
    // `minDwellMs` to reach its real content.
    if (hash === previous && Date.now() - started >= minDwellMs) {
      await ensureParent(target);
      await writeFile(target, last);
      return;
    }
    previous = hash;
  }
  log.warn(`frame never stabilised within ${maxWaitMs}ms, using the last one`);
  await ensureParent(target);
  await writeFile(target, last);
}

/** Shared frame-settling knobs. */
export interface CaptureTiming {
  settleMs: number;
  maxWaitMs: number;
  minDwellMs: number;
}

export interface AndroidCaptureOptions extends CaptureTiming {
  warmupMs: number;
  headless: boolean;
  keepBooted: boolean;
}

/** Captures every scene × locale for one Android device. Returns file paths. */
export async function captureAndroidDevice(
  config: StorekitConfig,
  device: AndroidDevice,
  deviceIndex: number,
  scenes: Scene[],
  locales: string[],
  apk: string,
  opts: AndroidCaptureOptions,
): Promise<string[]> {
  const written: string[] = [];
  const failures: string[] = [];
  const emulator = await startEmulator(device, deviceIndex, opts.headless);

  try {
    await installApk(emulator.serial, apk);
    await prepareEmulator(emulator.serial, config);

    for (const locale of locales) {
      await log.group(`locale ${locale}`, async () => {
        await setAppLocale(emulator.serial, config.app.applicationId, locale);
        // Demo mode is dropped when the framework restarts on the locale
        // fallback path, so re-assert it before capturing.
        await prepareEmulator(emulator.serial, config);

        if (credentials()) {
          await stopApp(emulator.serial, config.app.applicationId);
          await launchApp(emulator.serial, config, null);
          log.info(`warming session + cache (${Math.round(opts.warmupMs / 1000)}s)`);
          await sleep(opts.warmupMs);
        } else {
          log.warn("no MENSA_STOREKIT_EMAIL/PASSWORD, capturing the signed-out app");
        }

        for (const scene of scenes) {
          await assertFreeSpace(WORK_ROOT, CAPTURE_FREE_MB, "capturing screenshots");

          const target = rawPath("android", device.id, locale, scene.id);
          try {
            await stopApp(emulator.serial, config.app.applicationId);
            await launchApp(emulator.serial, config, scene);
            await captureWhenStable(emulator.serial, target, {
              settleMs: scene.settleMs ?? opts.settleMs,
              maxWaitMs: opts.maxWaitMs,
              minDwellMs: opts.minDwellMs,
            });
            written.push(target);
            log.ok(`${scene.id}`);
          } catch (error) {
            // One bad scene should not throw away the ones already captured.
            failures.push(
              `android/${device.id}/${locale}/${scene.id}: ${(error as Error).message}`,
            );
            log.error(`${scene.id}: ${(error as Error).message.split("\n")[0]}`);
          }
        }
        await stopApp(emulator.serial, config.app.applicationId);
      });
    }
    await releaseEmulator(emulator.serial);
  } finally {
    if (!opts.keepBooted) await emulator.stop();
  }

  if (failures.length) {
    log.warn(`${failures.length} scene non catturate:\n  ${failures.join("\n  ")}`);
  }
  return written;
}
