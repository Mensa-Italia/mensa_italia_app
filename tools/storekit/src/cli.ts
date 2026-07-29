#!/usr/bin/env bun
import { existsSync } from "node:fs";
import { copyFile } from "node:fs/promises";
import { homedir } from "node:os";
import { join, relative } from "node:path";
import { devicesFor, loadConfig, scenesFor } from "./config.ts";
import { buildAndroidDelivery, buildIosDelivery } from "./deliver.ts";
import {
  androidTools,
  chromeExecutable,
  credentials,
  javaHome,
  loadDotEnv,
} from "./env.ts";
import { c, humanMs, log } from "./log.ts";
import {
  OUT_ROOT,
  RAW_ROOT,
  REPO_ROOT,
  STORE_ROOT,
  WORK_ROOT,
  ensureDir,
  ensureParent,
  rawPath,
  removeDir,
} from "./paths.ts";
import { run } from "./proc.ts";
import { freeMb } from "./disk.ts";
import { buildAndroidApk, captureAndroidDevice } from "./capture/android.ts";
import { buildIosApp, captureIosDevice } from "./capture/ios.ts";
import { closeBrowser } from "./render/browser.ts";
import { renderDeviceLocale } from "./render/compose.ts";
import { renderFeatureGraphic, renderPlayIcon } from "./render/extras.ts";
import { renderMockScreen } from "./render/mock.ts";
import { THEMES } from "./render/theme.ts";
import type { Platform, StorekitConfig } from "./types.ts";

// ─── Arguments ───────────────────────────────────────────────────────────────

interface Args {
  command: string;
  flags: Record<string, string | boolean>;
}

function parseArgs(argv: string[]): Args {
  const [command = "help", ...rest] = argv;
  const flags: Record<string, string | boolean> = {};
  for (let i = 0; i < rest.length; i++) {
    const token = rest[i]!;
    if (!token.startsWith("--")) continue;
    const key = token.slice(2);
    const next = rest[i + 1];
    if (next && !next.startsWith("--")) {
      flags[key] = next;
      i += 1;
    } else {
      flags[key] = true;
    }
  }
  return { command, flags };
}

const list = (value: string | boolean | undefined): string[] | undefined =>
  typeof value === "string" ? value.split(",").map((s) => s.trim()).filter(Boolean) : undefined;

const num = (value: string | boolean | undefined, fallback: number): number =>
  typeof value === "string" && Number.isFinite(Number(value)) ? Number(value) : fallback;

function platformsFrom(flags: Args["flags"]): Platform[] {
  const value = flags.platform;
  if (value === "ios") return ["ios"];
  if (value === "android") return ["android"];
  return ["ios", "android"];
}

function rel(path: string): string {
  return relative(REPO_ROOT, path) || path;
}

// ─── doctor ──────────────────────────────────────────────────────────────────

async function doctor(config: StorekitConfig): Promise<void> {
  log.title("Ambiente");

  const checks: Array<[string, () => Promise<string>]> = [
    [
      "Xcode / simctl",
      async () => {
        const { code, stdout } = await run("xcrun", ["simctl", "help"]);
        if (code !== 0) throw new Error("simctl non disponibile");
        void stdout;
        const version = await run("xcodebuild", ["-version"]);
        return version.stdout.split("\n")[0] ?? "ok";
      },
    ],
    [
      "xcodegen",
      async () => (await run("xcodegen", ["--version"])).stdout.trim() || "ok",
    ],
    [
      "JDK (Gradle)",
      async () => {
        const home = javaHome();
        if (!home) {
          throw new Error(
            "nessun JDK 17/21 trovato: Gradle 8.10 non parte su Java 24+ " +
              "(`brew install openjdk@17`)",
          );
        }
        const { stderr, stdout } = await run(join(home, "bin", "java"), ["-version"]);
        return `${(stderr || stdout).split("\n")[0] ?? ""} (${home})`;
      },
    ],
    [
      "Android SDK",
      async () => {
        const tools = androidTools();
        const { stdout } = await run(tools.adb, ["--version"], { env: tools.env });
        return `${tools.sdk} (${stdout.split("\n")[0] ?? "adb"})`;
      },
    ],
    [
      "emulator",
      async () => {
        const tools = androidTools();
        const { stdout } = await run(tools.emulator, ["-list-avds"], { env: tools.env });
        const avds = stdout.split("\n").map((s) => s.trim()).filter(Boolean);
        return avds.length ? avds.join(", ") : "nessun AVD (verrà creato)";
      },
    ],
    [
      "Spazio disco",
      async () => {
        const mb = Math.round((await freeMb(join(homedir(), ".android"))) ?? 0);
        const free = `${(mb / 1024).toFixed(1)} GB liberi`;
        // The emulator allocates a 6 GiB userdata partition + 20% headroom,
        // but only the first time an AVD boots. Afterwards it reuses the
        // existing image. So this is a caveat, not a hard failure.
        return mb < 7380
          ? `${free}, sotto i 7,4 GB: il primo boot di un AVD nuovo fallirà`
          : free;
      },
    ],
    ["Chromium (framing)", async () => chromeExecutable()],
    [
      "fastlane",
      async () => (await run("fastlane", ["--version"])).stdout.trim().split("\n").pop() ?? "ok",
    ],
  ];

  for (const [label, check] of checks) {
    try {
      log.ok(`${label.padEnd(20)} ${c.dim(await check())}`);
    } catch (error) {
      log.error(`${label.padEnd(20)} ${(error as Error).message}`);
    }
  }

  log.title("Configurazione");
  log.info(`tema            ${config.theme}`);
  log.info(`lingue          ${config.locales.join(", ")}`);
  log.info(`scene           ${config.scenes.map((s) => s.id).join(", ")}`);
  log.info(
    `device iOS      ${devicesFor(config, "ios").map((d) => `${d.id} (${d.store.width}×${d.store.height})`).join(", ") || "nessuno"}`,
  );
  log.info(
    `device Android  ${devicesFor(config, "android").map((d) => `${d.id} (${d.store.width}×${d.store.height})`).join(", ") || "nessuno"}`,
  );
  log.info(
    credentials()
      ? "credenziali     MENSA_STOREKIT_EMAIL/PASSWORD presenti"
      : "credenziali     assenti: le catture partiranno da app non autenticata",
  );
}

// ─── capture ─────────────────────────────────────────────────────────────────

async function capture(config: StorekitConfig, flags: Args["flags"]): Promise<void> {
  const platforms = platformsFrom(flags);
  const locales = list(flags.locales) ?? config.locales;
  const onlyScenes = list(flags.scenes);
  const onlyDevices = list(flags.devices);
  const rebuild = flags.rebuild === true;

  const timing = {
    settleMs: num(flags["settle-ms"], 1_400),
    maxWaitMs: num(flags["max-wait-ms"], 40_000),
    minDwellMs: num(flags["min-dwell-ms"], 7_000),
    warmupMs: num(flags["warmup-ms"], 30_000),
    keepBooted: flags["keep-booted"] === true,
  };

  await ensureDir(WORK_ROOT);

  const scenesFiltered = (platform: Platform) =>
    scenesFor(config, platform).filter((s) => !onlyScenes || onlyScenes.includes(s.id));

  if (platforms.includes("ios")) {
    const devices = config.devices.ios.filter((d) =>
      onlyDevices?.length ? onlyDevices.includes(d.id) : d.enabled,
    );
    const scenes = scenesFiltered("ios");
    if (!devices.length || !scenes.length) {
      log.warn("iOS: nessun device o nessuna scena, salto");
    } else {
      const appPath = await log.group("iOS: build", () => buildIosApp(config, rebuild));
      for (const device of devices) {
        await log.group(`iOS: ${device.label}`, () =>
          captureIosDevice(config, device, scenes, locales, appPath, {
            ...timing,
            rebuild,
          }),
        );
      }
    }
  }

  if (platforms.includes("android")) {
    const devices = config.devices.android.filter((d) =>
      onlyDevices?.length ? onlyDevices.includes(d.id) : d.enabled,
    );
    const scenes = scenesFiltered("android");
    if (!devices.length || !scenes.length) {
      log.warn("Android: nessun device o nessuna scena, salto");
    } else {
      const apk = await log.group("Android: build", () =>
        buildAndroidApk(config, rebuild),
      );
      for (const [index, device] of devices.entries()) {
        await log.group(`Android: ${device.label}`, () =>
          captureAndroidDevice(config, device, index, scenes, locales, apk, {
            ...timing,
            headless: flags.headed !== true,
          }),
        );
      }
    }
  }
}

// ─── render ──────────────────────────────────────────────────────────────────

async function render(config: StorekitConfig, flags: Args["flags"]): Promise<void> {
  const themeId = typeof flags.theme === "string" ? flags.theme : config.theme;
  const platforms = platformsFrom(flags);
  const locales = list(flags.locales) ?? config.locales;

  for (const platform of platforms) {
    for (const device of devicesFor(config, platform, list(flags.devices))) {
      const scenes = scenesFor(config, platform);
      for (const locale of locales) {
        await renderDeviceLocale(config, platform, device, scenes, locale, themeId);
      }
    }
  }
}

// ─── store assets ────────────────────────────────────────────────────────────

async function storeAssets(config: StorekitConfig): Promise<void> {
  for (const locale of config.locales) {
    await renderFeatureGraphic(config, locale);
  }
  await renderPlayIcon();
}

// ─── preview ─────────────────────────────────────────────────────────────────

async function cloneRawCaptures(
  fromDeviceId: string,
  toDeviceId: string,
  locale: string,
  sceneIds: string[],
): Promise<void> {
  for (const sceneId of sceneIds) {
    const from = rawPath("ios", fromDeviceId, locale, sceneId);
    const to = rawPath("ios", toDeviceId, locale, sceneId);
    await ensureParent(to);
    await copyFile(from, to);
  }
}

/**
 * Renders every theme against synthetic app screens so the art direction can
 * be judged (and tuned) without building the apps or booting a device.
 */
async function preview(config: StorekitConfig, flags: Args["flags"]): Promise<void> {
  const device = devicesFor(config, "ios")[0] ?? config.devices.ios[0]!;
  const scenes = config.scenes.slice(0, num(flags.count, 3));
  const locale = typeof flags.locale === "string" ? flags.locale : config.defaultLocale;
  const themeIds = list(flags.theme) ?? THEMES.map((t) => t.id);

  log.step("genero schermate sintetiche");
  // Mock captures use the raw tree of a reserved pseudo-device so a preview
  // never overwrites genuine captures.
  const previewDevice = { ...device, id: "_preview" };
  for (const scene of scenes) {
    await renderMockScreen(
      scene.id,
      Math.round(device.store.width * 0.82),
      Math.round(device.store.height * 0.82),
      rawPath("ios", previewDevice.id, locale, scene.id),
    );
  }

  for (const themeId of themeIds) {
    await log.group(`tema ${themeId}`, async () => {
      // Raw captures live under `_preview`; each theme writes its framed output
      // to its own pseudo-device folder so the variants sit side by side.
      const themed = { ...previewDevice, id: `_preview-${themeId}` };
      await cloneRawCaptures(previewDevice.id, themed.id, locale, scenes.map((s) => s.id));
      const files = await renderDeviceLocale(config, "ios", themed, scenes, locale, themeId);
      for (const file of files) log.info(rel(file));
    });
  }
  log.info(`anteprime in ${rel(join(STORE_ROOT, "ios"))}`);
}

// ─── help ────────────────────────────────────────────────────────────────────

function help(): void {
  console.log(`
${c.bold("storekit")}: screenshot e asset store per Mensa Italia

  ${c.cyan("bun run storekit <comando> [opzioni]")}

${c.bold("Comandi")}
  doctor      verifica toolchain e configurazione
  preview     rende ogni tema su schermate sintetiche (nessun device richiesto)
  capture     costruisce le app, avvia simulatore/emulatore e cattura le scene
  render      incornicia le catture nelle misure richieste dagli store
  assets      feature graphic Play (1024×500) e icona 512×512
  deliver     impagina tutto in out/fastlane per deliver e supply
  prune       svuota la build cache tenendo catture e artwork
  all         capture → render → assets → deliver
  clean       svuota out/

${c.bold("Opzioni")}
  --platform ios|android      limita la piattaforma (default: entrambe)
  --devices  id,id            limita i device (default: quelli enabled)
  --locales  it,en            limita le lingue
  --scenes   today,card       limita le scene (solo capture)
  --theme    aurora|parchment|panorama
  --rebuild                   forza la ricompilazione delle app
  --headed                    mostra la finestra dell'emulatore Android
  --keep-booted               lascia simulatore/emulatore accesi a fine run
  --settle-ms / --max-wait-ms / --min-dwell-ms / --warmup-ms
                              tempistiche di cattura

${c.bold("Temi")}
${THEMES.map((t) => `  ${t.id.padEnd(11)} ${t.label}`).join("\n")}
`);
}

// ─── main ────────────────────────────────────────────────────────────────────

async function main(): Promise<void> {
  loadDotEnv();
  const { command, flags } = parseArgs(process.argv.slice(2));

  if (command === "help" || flags.help) {
    help();
    return;
  }

  const started = Date.now();
  const config = await loadConfig();

  switch (command) {
    case "doctor":
      await doctor(config);
      break;
    case "capture":
      await capture(config, flags);
      break;
    case "render":
      await render(config, flags);
      break;
    case "assets":
      await storeAssets(config);
      break;
    case "preview":
      await preview(config, flags);
      break;
    case "deliver":
      await buildIosDelivery(config);
      await buildAndroidDelivery(config);
      break;
    case "all":
      await capture(config, flags);
      await render(config, flags);
      await storeAssets(config);
      await buildIosDelivery(config);
      await buildAndroidDelivery(config);
      break;
    case "prune":
      await removeDir(WORK_ROOT);
      log.ok(`rimossa la build cache in ${rel(WORK_ROOT)}; catture e artwork intatti`);
      break;
    case "clean":
      await removeDir(OUT_ROOT);
      log.ok(`rimosso ${rel(OUT_ROOT)}`);
      break;
    default:
      log.error(`comando sconosciuto: ${command}`);
      help();
      process.exitCode = 1;
      return;
  }

  await closeBrowser();

  if (command !== "doctor" && command !== "clean") {
    const where = existsSync(STORE_ROOT) ? rel(STORE_ROOT) : rel(RAW_ROOT);
    log.title(`Fatto in ${humanMs(Date.now() - started)}, output in ${where}`);
  }
}

main().catch(async (error) => {
  await closeBrowser().catch(() => {});
  log.error((error as Error).message);
  if (process.env.STOREKIT_DEBUG) console.error(error);
  process.exit(1);
});
