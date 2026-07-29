import { copyFile, readdir, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { log } from "./log.ts";
import { DELIVERY_ROOT, STORE_ROOT, ensureDir, ensureParent, removeDir } from "./paths.ts";
import type { StorekitConfig } from "./types.ts";

/**
 * Lays the finished artwork out exactly how `deliver` (App Store Connect) and
 * `supply` (Google Play) expect to find it, together with the listing text.
 * Everything lands under `out/fastlane/`. Nothing is uploaded here.
 */

async function copyDir(from: string, to: string): Promise<number> {
  if (!existsSync(from)) return 0;
  await ensureDir(to);
  let n = 0;
  for (const entry of await readdir(from, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      n += await copyDir(join(from, entry.name), join(to, entry.name));
    } else if (entry.name.endsWith(".png")) {
      await copyFile(join(from, entry.name), join(to, entry.name));
      n += 1;
    }
  }
  return n;
}

async function writeText(file: string, content: string): Promise<void> {
  await ensureParent(file);
  await writeFile(file, content.endsWith("\n") ? content : `${content}\n`, "utf8");
}

export const IOS_DELIVERY = join(DELIVERY_ROOT, "ios");
export const ANDROID_DELIVERY = join(DELIVERY_ROOT, "android");

/** Builds `out/fastlane/ios/{screenshots,metadata}`. */
export async function buildIosDelivery(config: StorekitConfig): Promise<void> {
  await removeDir(IOS_DELIVERY);
  const devices = config.devices.ios.filter((d) => d.enabled);

  for (const locale of config.locales) {
    const folder = config.storeLocaleFolders.ios[locale]!;
    let copied = 0;
    for (const device of devices) {
      copied += await copyDir(
        join(STORE_ROOT, "ios", device.id, locale),
        join(IOS_DELIVERY, "screenshots", folder),
      );
    }

    const text = config.storeText[locale]!;
    const meta = join(IOS_DELIVERY, "metadata", folder);
    await writeText(join(meta, "name.txt"), text.name);
    if (text.subtitle) await writeText(join(meta, "subtitle.txt"), text.subtitle);
    await writeText(join(meta, "description.txt"), text.description);
    if (text.keywords) await writeText(join(meta, "keywords.txt"), text.keywords);
    if (text.promotionalText) {
      await writeText(join(meta, "promotional_text.txt"), text.promotionalText);
    }
    if (text.releaseNotes) {
      await writeText(join(meta, "release_notes.txt"), text.releaseNotes);
    }

    log.ok(`deliver ${folder} → ${copied} screenshot, metadata scritti`);
  }

  // `deliver` refuses to run without this marker next to the screenshots.
  await writeText(
    join(IOS_DELIVERY, "screenshots", "README.txt"),
    "Generato da tools/storekit, non modificare a mano.",
  );
}

/** Builds `out/fastlane/android/metadata/android/<locale>/…`. */
export async function buildAndroidDelivery(config: StorekitConfig): Promise<void> {
  await removeDir(ANDROID_DELIVERY);
  const devices = config.devices.android.filter((d) => d.enabled);

  for (const locale of config.locales) {
    const folder = config.storeLocaleFolders.android[locale]!;
    const base = join(ANDROID_DELIVERY, "metadata", "android", folder);
    let copied = 0;
    for (const device of devices) {
      copied += await copyDir(
        join(STORE_ROOT, "android", device.id, locale),
        join(base, "images", device.supplyFolder),
      );
    }

    const featureGraphic = join(STORE_ROOT, "android", "_assets", locale, "featureGraphic.png");
    if (existsSync(featureGraphic)) {
      await ensureDir(join(base, "images"));
      await copyFile(featureGraphic, join(base, "images", "featureGraphic.png"));
    }
    const icon = join(STORE_ROOT, "android", "_assets", "icon.png");
    if (existsSync(icon)) {
      await ensureDir(join(base, "images"));
      await copyFile(icon, join(base, "images", "icon.png"));
    }

    const text = config.storeText[locale]!;
    await writeText(join(base, "title.txt"), text.name);
    await writeText(
      join(base, "short_description.txt"),
      (text.shortDescription ?? text.subtitle ?? "").slice(0, 80),
    );
    await writeText(join(base, "full_description.txt"), text.description);
    if (text.releaseNotes) {
      await writeText(join(base, "changelogs", "default.txt"), text.releaseNotes);
    }

    log.ok(`supply ${folder} → ${copied} screenshot, metadata scritti`);
  }
}
