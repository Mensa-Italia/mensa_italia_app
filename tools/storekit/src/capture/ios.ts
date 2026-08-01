import { existsSync } from "node:fs";
import { readFile, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { join } from "node:path";
import { copyFor, regionalTag } from "../config.ts";
import { credentials, gradleEnv } from "../env.ts";
import { log } from "../log.ts";
import { REPO_ROOT, WORK_ROOT, ensureDir, ensureParent, rawPath, removeDir } from "../paths.ts";
import { run, runOrThrow, sleep, waitFor } from "../proc.ts";
import type { IosDevice, Scene, StorekitConfig } from "../types.ts";
import type { CaptureTiming } from "./android.ts";
import { CAPTURE_FREE_MB, IOS_BUILD_FREE_MB, assertFreeSpace } from "../disk.ts";

const SIMCTL = ["simctl"];
const xcrun = (args: string[], opts = {}) => runOrThrow("xcrun", args, opts);

interface SimDevice {
  udid: string;
  name: string;
  state: string;
  isAvailable: boolean;
  runtime: string;
}

// ─── Simulator inventory ─────────────────────────────────────────────────────

async function listDevices(): Promise<SimDevice[]> {
  const { stdout } = await xcrun([...SIMCTL, "list", "devices", "--json"]);
  const parsed = JSON.parse(stdout) as {
    devices: Record<string, Array<Omit<SimDevice, "runtime">>>;
  };
  return Object.entries(parsed.devices).flatMap(([runtime, devices]) =>
    devices.map((d) => ({ ...d, runtime })),
  );
}

async function resolveRuntime(name?: string): Promise<string> {
  const { stdout } = await xcrun([...SIMCTL, "list", "runtimes", "--json"]);
  const parsed = JSON.parse(stdout) as {
    runtimes: Array<{ identifier: string; name: string; version: string; isAvailable: boolean }>;
  };
  const ios = parsed.runtimes
    .filter((r) => r.isAvailable && r.identifier.includes("SimRuntime.iOS"))
    .sort((a, b) => compareVersions(b.version, a.version));
  if (!ios.length) throw new Error("No available iOS simulator runtime found.");
  if (name) {
    const exact = ios.find((r) => r.name === name || r.version === name);
    if (exact) return exact.identifier;
    log.warn(`iOS runtime "${name}" not installed, falling back to ${ios[0]!.name}`);
  }
  return ios[0]!.identifier;
}

function compareVersions(a: string, b: string): number {
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const d = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (d !== 0) return d;
  }
  return 0;
}

async function resolveDeviceType(name: string): Promise<string> {
  const { stdout } = await xcrun([...SIMCTL, "list", "devicetypes", "--json"]);
  const parsed = JSON.parse(stdout) as {
    devicetypes: Array<{ name: string; identifier: string }>;
  };
  const exact = parsed.devicetypes.find((d) => d.name === name);
  if (exact) return exact.identifier;
  // Tolerate Apple's parenthetical RAM suffixes, e.g. "iPad Pro 13-inch (M4) (16GB)".
  const prefixed = parsed.devicetypes.find((d) => d.name.startsWith(name));
  if (prefixed) {
    log.info(`device type "${name}" matched "${prefixed.name}"`);
    return prefixed.identifier;
  }
  throw new Error(
    `Simulator device type "${name}" not available. Run \`xcrun simctl list devicetypes\`.`,
  );
}

/** Returns the UDID of the storekit simulator for `device`, creating it if needed. */
export async function ensureSimulator(device: IosDevice): Promise<string> {
  const existing = (await listDevices()).find(
    (d) => d.name === device.simulatorName && d.isAvailable,
  );
  if (existing) return existing.udid;

  log.info(`creating simulator "${device.simulatorName}"`);
  const [deviceType, runtime] = await Promise.all([
    resolveDeviceType(device.deviceType),
    resolveRuntime(device.runtime),
  ]);
  const { stdout } = await xcrun([
    ...SIMCTL,
    "create",
    device.simulatorName,
    deviceType,
    runtime,
  ]);
  return stdout.trim();
}

async function deviceState(udid: string): Promise<string> {
  const devices = await listDevices();
  return devices.find((d) => d.udid === udid)?.state ?? "Unknown";
}

export async function bootSimulator(udid: string): Promise<void> {
  if ((await deviceState(udid)) === "Booted") return;
  await run("xcrun", [...SIMCTL, "boot", udid]);
  await runOrThrow("xcrun", [...SIMCTL, "bootstatus", udid, "-b"], {
    timeoutMs: 300_000,
  });
  await waitFor(`simulator ${udid} to settle`, async () => (await deviceState(udid)) === "Booted", {
    timeoutMs: 120_000,
  });
}

export async function shutdownSimulator(udid: string): Promise<void> {
  await run("xcrun", [...SIMCTL, "shutdown", udid]);
}

// ─── Build ───────────────────────────────────────────────────────────────────

const DERIVED_DATA = join(WORK_ROOT, "ios-derived-data");
const APP_PATH = join(DERIVED_DATA, "Build/Products/Debug-iphonesimulator/iosApp.app");

/**
 * Regenerates the Xcode project, builds the shared KMP XCFramework and then
 * the simulator .app. Returns the path of the built bundle.
 */
export async function buildIosApp(config: StorekitConfig, force: boolean): Promise<string> {
  if (!force && existsSync(APP_PATH)) {
    log.info(`reusing ${APP_PATH.replace(REPO_ROOT + "/", "")}`);
    return APP_PATH;
  }
  await ensureDir(DERIVED_DATA);
  await assertFreeSpace(WORK_ROOT, IOS_BUILD_FREE_MB, "a clean iOS build");

  // The Gradle build configures every module, including :androidApp, so the
  // Android SDK has to be resolvable even for the iOS-only XCFramework task,
  // and Gradle needs a JDK it actually supports.
  const buildEnv = gradleEnv();
  if (!buildEnv.ANDROID_HOME) log.warn("Android SDK not found, Gradle configuration may fail");
  if (!buildEnv.JAVA_HOME) log.warn("no supported JDK found, Gradle may refuse to start");

  await log.group("shared XCFramework", async () => {
    await runOrThrow(join(REPO_ROOT, "gradlew"), [":shared:assembleSharedDebugXCFramework"], {
      cwd: REPO_ROOT,
      env: buildEnv,
      inherit: true,
      timeoutMs: 1_800_000,
    });
  });

  await log.group("xcodegen", async () => {
    await runOrThrow("xcodegen", ["generate", "--spec", "project.yml"], {
      cwd: join(REPO_ROOT, "iosApp"),
    });
  });

  await log.group("xcodebuild (simulator)", async () => {
    await runOrThrow(
      "xcodebuild",
      [
        "build",
        "-project",
        join(REPO_ROOT, "iosApp/iosApp.xcodeproj"),
        "-scheme",
        config.app.iosScheme,
        "-configuration",
        "Debug",
        "-sdk",
        "iphonesimulator",
        "-destination",
        "generic/platform=iOS Simulator",
        "-derivedDataPath",
        DERIVED_DATA,
        // Ad-hoc signing, NOT `CODE_SIGNING_ALLOWED=NO`.
        //
        // An unsigned simulator build gets no keychain-access-group
        // entitlement, so every Keychain call fails with
        // `errSecMissingEntitlement (-34018)`. `TokenStore` is backed by
        // `KeychainSettings`, so the session can never be persisted:
        // `AuthRepository.init()` catches the failure and falls back to
        // anonymous (its own doc comment calls this exact scenario out). The
        // login then succeeds in memory, but `RootView` is keyed on
        // `.id(locale.version)`: it re-mounts once the i18n catalog loads,
        // re-runs `doInit()`, and drops straight back to the login screen.
        //
        // `-` is ad-hoc signing: no provisioning profile, no developer
        // portal round-trip, but entitlements are applied.
        "CODE_SIGN_IDENTITY=-",
        "CODE_SIGN_STYLE=Manual",
        "PROVISIONING_PROFILE_SPECIFIER=",
        "DEVELOPMENT_TEAM=",
        // The `.app` is only ever installed on this machine's simulator, so
        // building the other slice doubles build time and disk for nothing.
        // `ONLY_ACTIVE_ARCH` alone does nothing against a *generic* destination
        // (there is no active arch to pick), so pin ARCHS to the host.
        `ARCHS=${process.arch === "x64" ? "x86_64" : "arm64"}`,
      ],
      { cwd: REPO_ROOT, env: buildEnv, inherit: true, timeoutMs: 3_600_000 },
    );
  });

  if (!existsSync(APP_PATH)) {
    throw new Error(`Build finished but ${APP_PATH} is missing.`);
  }
  await pruneBuildCache();
  return APP_PATH;
}

/**
 * Drops the Xcode intermediates once the `.app` exists.
 *
 * They are worth well over a gigabyte and are only useful for an *incremental*
 * rebuild, which storekit never does: `capture` reuses the built bundle, and
 * `--rebuild` starts from scratch anyway. Keeping them around is what filled
 * the volume mid-capture, taking `simctl io screenshot` down with it.
 */
export async function pruneBuildCache(): Promise<void> {
  for (const dir of ["Build/Intermediates.noindex", "Logs", "ModuleCache.noindex"]) {
    await removeDir(join(DERIVED_DATA, dir));
  }
}

// ─── Device preparation ──────────────────────────────────────────────────────

/** Applies appearance, a frozen status bar and pre-granted privacy consents. */
export async function prepareSimulator(
  udid: string,
  config: StorekitConfig,
): Promise<void> {
  await run("xcrun", [...SIMCTL, "ui", udid, "appearance", config.appearance]);

  await run("xcrun", [
    ...SIMCTL,
    "status_bar",
    udid,
    "override",
    "--time",
    config.statusBar.time,
    "--dataNetwork",
    "wifi",
    "--wifiMode",
    "active",
    "--wifiBars",
    String(config.statusBar.wifiBars),
    "--cellularMode",
    "active",
    "--cellularBars",
    String(config.statusBar.cellularBars),
    // `charged` draws the green plugged-in glyph; store artwork wants the
    // plain full battery, which is `discharging` at 100%.
    "--batteryState",
    "discharging",
    "--batteryLevel",
    String(config.statusBar.batteryPercent),
  ]);

  // Anything that could raise a consent alert mid-capture.
  for (const service of ["location-always", "photos", "camera", "calendar", "contacts"]) {
    await run("xcrun", [...SIMCTL, "privacy", udid, "grant", service, config.app.bundleId]);
  }
}

export async function installApp(udid: string, appPath: string): Promise<void> {
  await runOrThrow("xcrun", [...SIMCTL, "install", udid, appPath]);
}

// ─── Launch + capture ────────────────────────────────────────────────────────

function launchEnv(scene: Scene | null, warmup = false): Record<string, string> {
  const creds = credentials();
  const env: Record<string, string> = {
    SIMCTL_CHILD_MENSA_SUPPRESS_PERMISSION_PROMPTS: "1",
    // In DEBUG builds `RootViewModel.evaluatePhase` sends every *fresh* login
    // to onboarding so the flow can be iterated on. Screenshots want the tab
    // shell, and the app already exposes this exact escape hatch.
    SIMCTL_CHILD_MENSA_SKIP_ONBOARDING: "1",
    // Nome, foto e numero tessera del socio diventano un segnaposto: le
    // immagini finiscono pubbliche sugli store. Vedi `DemoIdentity` nel
    // modulo condiviso.
    SIMCTL_CHILD_MENSA_DEMO_IDENTITY: "1",
  };
  if (creds) {
    // The app has two separate autologin hooks with different env names.
    //
    // `MENSA_AUTOLOGIN` + `MENSA_EMAIL`/`MENSA_PASSWORD` drives
    // `LoginViewModel.autoLoginIfRequested()`: it submits the real login form
    // once `LoginView` appears, which is the path that actually works for the
    // tab scenes. Do not be tempted to call `koin.auth.login` earlier instead:
    // it returns a Kotlin `Result` built by `runCatching`, so it never throws
    // across the Swift bridge and a failure is silently indistinguishable from
    // success.
    env.SIMCTL_CHILD_MENSA_AUTOLOGIN = "1";
    env.SIMCTL_CHILD_MENSA_EMAIL = creds.email;
    env.SIMCTL_CHILD_MENSA_PASSWORD = creds.password;
    // `MENSA_AUTOLOGIN_EMAIL`/`_PWD` are what `DebugLaunchSelector` reads for
    // the `MENSA_LAUNCH_SCREEN` scenes, which bypass `RootView` entirely.
    env.SIMCTL_CHILD_MENSA_AUTOLOGIN_EMAIL = creds.email;
    env.SIMCTL_CHILD_MENSA_AUTOLOGIN_PWD = creds.password;
    // `DebugRefreshHarness` refreshes every repository, which is what fills
    // the local cache. Useful once, during the warm-up; running it on every
    // scene would keep the frame moving while thirteen requests land.
    if (warmup) env.SIMCTL_CHILD_MENSA_REFRESH_ALL = "1";
  }
  if (scene?.ios?.launchScreen) {
    env.SIMCTL_CHILD_MENSA_LAUNCH_SCREEN = scene.ios.launchScreen;
  }
  return env;
}

function launchArgs(scene: Scene | null, locale: string): string[] {
  const args: string[] = [];
  if (scene?.ios?.tab) args.push("--initial-tab", scene.ios.tab);
  // Apple's own overrides: the app reads Locale.preferredLanguages.
  args.push("-AppleLanguages", `(${locale})`);
  args.push("-AppleLocale", regionalTag(locale).replace("-", "_"));
  return args;
}

async function terminateApp(udid: string, bundleId: string): Promise<void> {
  await run("xcrun", [...SIMCTL, "terminate", udid, bundleId]);
}

/**
 * Signs in once and lets `DebugRefreshHarness` fill the local SQLDelight cache,
 * so every later scene launch renders populated content immediately.
 */
export async function warmUp(
  udid: string,
  config: StorekitConfig,
  locale: string,
  warmupMs: number,
): Promise<void> {
  if (!credentials()) {
    log.warn("no MENSA_STOREKIT_EMAIL/PASSWORD, capturing the signed-out app");
    return;
  }
  await terminateApp(udid, config.app.bundleId);
  await runOrThrow(
    "xcrun",
    [...SIMCTL, "launch", udid, config.app.bundleId, ...launchArgs(null, locale)],
    { env: launchEnv(null, true) },
  );
  log.info(`warming session + cache (${Math.round(warmupMs / 1000)}s)`);
  await sleep(warmupMs);
  await terminateApp(udid, config.app.bundleId);
}

async function screenshotTo(udid: string, file: string): Promise<void> {
  await ensureParent(file);
  await runOrThrow("xcrun", [...SIMCTL, "io", udid, "screenshot", "--type", "png", file], {
    timeoutMs: 60_000,
  });
}

/**
 * Screenshots repeatedly until two consecutive frames are byte-identical, so
 * captures never land mid-animation or on a loading spinner.
 */
async function captureWhenStable(
  udid: string,
  target: string,
  { settleMs, maxWaitMs, minDwellMs }: CaptureTiming,
): Promise<void> {
  const probe = join(WORK_ROOT, "probe.png");
  const started = Date.now();
  const deadline = started + maxWaitMs;
  let previous = "";

  while (Date.now() < deadline) {
    await sleep(settleMs);
    await screenshotTo(udid, probe);
    const frame = await readFile(probe);
    const hash = createHash("sha1").update(frame).digest("hex");
    // A splash or a spinner mid-fetch is perfectly stable between frames, so
    // stability alone would immortalise the loading state. Hold for
    // `minDwellMs` first.
    if (hash === previous && Date.now() - started >= minDwellMs) {
      await ensureParent(target);
      await writeFile(target, frame);
      return;
    }
    previous = hash;
  }
  // Settled or not, ship the last frame rather than failing the whole run.
  log.warn(`frame never stabilised within ${maxWaitMs}ms, using the last one`);
  await ensureParent(target);
  await writeFile(target, await readFile(probe));
}

export interface IosCaptureOptions extends CaptureTiming {
  warmupMs: number;
  rebuild: boolean;
  keepBooted: boolean;
}

/** Captures every scene × locale for one iOS device. Returns the file paths. */
export async function captureIosDevice(
  config: StorekitConfig,
  device: IosDevice,
  scenes: Scene[],
  locales: string[],
  appPath: string,
  opts: IosCaptureOptions,
): Promise<string[]> {
  const written: string[] = [];
  const failures: string[] = [];
  const udid = await ensureSimulator(device);
  log.info(`simulator ${device.simulatorName} → ${udid}`);

  await bootSimulator(udid);
  await prepareSimulator(udid, config);
  await installApp(udid, appPath);

  for (const locale of locales) {
    await log.group(`locale ${locale}`, async () => {
      await warmUp(udid, config, locale, opts.warmupMs);

      for (const scene of scenes) {
        // A full volume takes `simctl io screenshot` down with an opaque
        // exit 128, so stop before that rather than after.
        await assertFreeSpace(WORK_ROOT, CAPTURE_FREE_MB, "capturing screenshots");

        const target = rawPath("ios", device.id, locale, scene.id);
        try {
          await terminateApp(udid, config.app.bundleId);
          await runOrThrow(
            "xcrun",
            [
              ...SIMCTL,
              "launch",
              udid,
              config.app.bundleId,
              ...launchArgs(scene, locale),
            ],
            { env: launchEnv(scene) },
          );
          await captureWhenStable(udid, target, {
            settleMs: scene.settleMs ?? opts.settleMs,
            maxWaitMs: opts.maxWaitMs,
            minDwellMs: opts.minDwellMs,
          });
          written.push(target);
          log.ok(`${scene.id}`);
        } catch (error) {
          // One bad scene should not throw away the ones already captured.
          failures.push(`ios/${device.id}/${locale}/${scene.id}: ${(error as Error).message}`);
          log.error(`${scene.id}: ${(error as Error).message.split("\n")[0]}`);
        }
      }
      await terminateApp(udid, config.app.bundleId);
    });
  }

  if (!opts.keepBooted) await shutdownSimulator(udid);
  if (failures.length) {
    log.warn(`${failures.length} scene non catturate:\n  ${failures.join("\n  ")}`);
  }
  return written;
}
