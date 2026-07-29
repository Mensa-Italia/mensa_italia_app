import { join } from "node:path";
import { TOOL_ROOT } from "./paths.ts";
import type { Platform, Scene, SceneCopy, StorekitConfig } from "./types.ts";

let cached: StorekitConfig | null = null;

/** Loads and validates `storekit.config.ts`. */
export async function loadConfig(): Promise<StorekitConfig> {
  if (cached) return cached;
  const mod = (await import(join(TOOL_ROOT, "storekit.config.ts"))) as {
    default: StorekitConfig;
  };
  const config = mod.default;
  validate(config);
  cached = config;
  return config;
}

function validate(config: StorekitConfig): void {
  const problems: string[] = [];

  if (!config.locales.includes(config.defaultLocale)) {
    problems.push(`defaultLocale "${config.defaultLocale}" is not in locales`);
  }
  for (const locale of config.locales) {
    if (!config.storeLocaleFolders.ios[locale]) {
      problems.push(`storeLocaleFolders.ios is missing "${locale}"`);
    }
    if (!config.storeLocaleFolders.android[locale]) {
      problems.push(`storeLocaleFolders.android is missing "${locale}"`);
    }
    if (!config.storeText[locale]) {
      problems.push(`storeText is missing "${locale}"`);
    }
  }

  const sceneIds = new Set<string>();
  for (const scene of config.scenes) {
    if (sceneIds.has(scene.id)) problems.push(`duplicate scene id "${scene.id}"`);
    sceneIds.add(scene.id);
    if (!scene.ios && !scene.android) {
      problems.push(`scene "${scene.id}" targets neither platform`);
    }
    if (scene.ios && !scene.ios.tab && !scene.ios.launchScreen) {
      problems.push(`scene "${scene.id}" has an empty ios destination`);
    }
    if (!scene.copy[config.defaultLocale]) {
      problems.push(`scene "${scene.id}" has no copy for the default locale`);
    }
  }

  const deviceIds = new Set<string>();
  for (const device of [...config.devices.ios, ...config.devices.android]) {
    if (deviceIds.has(device.id)) problems.push(`duplicate device id "${device.id}"`);
    deviceIds.add(device.id);
    if (device.store.width <= 0 || device.store.height <= 0) {
      problems.push(`device "${device.id}" has a non-positive store size`);
    }
  }

  // Apple rejects listings with more than 10 screenshots per display target;
  // Google Play caps every image type at 8.
  if (config.scenes.length > 10) {
    problems.push(`${config.scenes.length} scenes, the App Store accepts at most 10`);
  }
  if (config.scenes.length > 8) {
    problems.push(`${config.scenes.length} scenes, Google Play accepts at most 8`);
  }

  // Store-side length limits. Cheaper to catch here than on a rejected upload.
  const limits: Array<[keyof (typeof config.storeText)[string], number, string]> = [
    ["name", 30, "App Store name / Play title"],
    ["subtitle", 30, "App Store subtitle"],
    ["promotionalText", 170, "App Store promotional text"],
    ["keywords", 100, "App Store keywords"],
    ["shortDescription", 80, "Play short description"],
    ["description", 4000, "description"],
  ];
  for (const locale of config.locales) {
    const text = config.storeText[locale];
    if (!text) continue;
    for (const [field, max, label] of limits) {
      const value = text[field];
      if (typeof value === "string" && value.length > max) {
        problems.push(
          `storeText.${locale}.${String(field)} is ${value.length} chars, ${label} allows ${max}`,
        );
      }
    }
  }

  if (problems.length) {
    throw new Error(`Invalid storekit.config.ts:\n  - ${problems.join("\n  - ")}`);
  }
}

/** Scenes that actually have a destination on `platform`. */
export function scenesFor(config: StorekitConfig, platform: Platform): Scene[] {
  return config.scenes.filter((s) => (platform === "ios" ? s.ios : s.android));
}

/** Copy for a scene in `locale`, falling back to the default locale. */
export function copyFor(
  config: StorekitConfig,
  scene: Scene,
  locale: string,
): SceneCopy {
  const entry = scene.copy[locale] ?? scene.copy[config.defaultLocale];
  if (!entry) {
    throw new Error(`Scene "${scene.id}" has no usable copy for "${locale}"`);
  }
  return entry;
}

/** Enabled devices for a platform, narrowed by an optional id filter. */
export function devicesFor(
  config: StorekitConfig,
  platform: Platform,
  onlyIds?: string[],
) {
  const all = platform === "ios" ? config.devices.ios : config.devices.android;
  return all.filter((d) => (onlyIds?.length ? onlyIds.includes(d.id) : d.enabled));
}

/**
 * Maps a storekit locale to the `-AppleLanguages` / `--locales` tag the
 * platform expects. `it` → `it-IT`, `en` → `en-US`, and anything already
 * regionalised is passed through.
 */
export function regionalTag(locale: string): string {
  if (locale.includes("-")) return locale;
  const map: Record<string, string> = {
    it: "it-IT",
    en: "en-US",
    de: "de-DE",
    fr: "fr-FR",
    es: "es-ES",
  };
  return map[locale] ?? `${locale}-${locale.toUpperCase()}`;
}
