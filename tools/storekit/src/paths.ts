import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { mkdir, rm } from "node:fs/promises";

/** `tools/storekit` */
export const TOOL_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
/** Repository root. */
export const REPO_ROOT = resolve(TOOL_ROOT, "..", "..");

export const OUT_ROOT = join(TOOL_ROOT, "out");
/** Untouched device captures, kept so `render` can re-run without a device. */
export const RAW_ROOT = join(OUT_ROOT, "raw");
/** Framed, store-ready artwork. */
export const STORE_ROOT = join(OUT_ROOT, "store");
/** fastlane `deliver` / `supply` trees. */
export const DELIVERY_ROOT = join(OUT_ROOT, "fastlane");
/** Scratch space: built .app bundles, emulator logs. */
export const WORK_ROOT = join(OUT_ROOT, "work");

export const FONT_GOTHAM = join(
  REPO_ROOT,
  "androidApp/src/main/res/font/gotham_bold.otf",
);
export const LOGO_MARK_SVG = join(REPO_ROOT, "iosApp/icon-source/foreground-mark.svg");

/** Path of a raw capture. */
export function rawPath(
  platform: string,
  deviceId: string,
  locale: string,
  sceneId: string,
): string {
  return join(RAW_ROOT, platform, deviceId, locale, `${sceneId}.png`);
}

/** Path of a finished store asset. */
export function storePath(
  platform: string,
  deviceId: string,
  locale: string,
  filename: string,
): string {
  return join(STORE_ROOT, platform, deviceId, locale, filename);
}

export async function ensureDir(path: string): Promise<void> {
  await mkdir(path, { recursive: true });
}

export async function ensureParent(filePath: string): Promise<void> {
  await ensureDir(dirname(filePath));
}

export async function removeDir(path: string): Promise<void> {
  await rm(path, { recursive: true, force: true });
}
