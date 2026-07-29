import { existsSync } from "node:fs";
import { writeFile } from "node:fs/promises";
import { copyFor } from "../config.ts";
import { log } from "../log.ts";
import { ensureParent, rawPath, storePath } from "../paths.ts";
import type {
  AndroidDevice,
  IosDevice,
  Platform,
  Scene,
  StorekitConfig,
} from "../types.ts";
import { imageDataUri, loadAssets } from "./assets.ts";
import { renderToPngs, type ClipRegion } from "./browser.ts";
import { htmlDocument, themeById, type RenderFrame, type Theme } from "./theme.ts";

type AnyDevice = IosDevice | AndroidDevice;

const isTabletDevice = (device: AnyDevice) =>
  device.frame.style === "ipad" || device.frame.style === "pixel-tablet";

/** `01_today.png`, prefixed with the device id on iOS so `deliver` stays sorted. */
function outputName(platform: Platform, device: AnyDevice, index: number, scene: Scene) {
  const ordinal = String(index + 1).padStart(2, "0");
  return platform === "ios"
    ? `${device.id}_${ordinal}_${scene.id}.png`
    : `${ordinal}_${scene.id}.png`;
}

function chunk<T>(items: T[], size: number | "all"): T[][] {
  if (size === "all") return [items];
  const out: T[][] = [];
  for (let i = 0; i < items.length; i += size) out.push(items.slice(i, i + size));
  return out;
}

/**
 * Frames every raw capture of one device × locale into store-ready artwork.
 * Returns the written file paths, in store order.
 */
export async function renderDeviceLocale(
  config: StorekitConfig,
  platform: Platform,
  device: AnyDevice,
  scenes: Scene[],
  locale: string,
  themeId: string,
): Promise<string[]> {
  const theme: Theme = themeById(themeId);
  const assets = await loadAssets();
  const { width, height } = device.store;
  const isTablet = isTabletDevice(device);

  const available: Array<{ scene: Scene; frame: RenderFrame }> = [];
  for (const scene of scenes) {
    const raw = rawPath(platform, device.id, locale, scene.id);
    if (!existsSync(raw)) {
      log.warn(`missing capture ${platform}/${device.id}/${locale}/${scene.id}, skipped`);
      continue;
    }
    const image = await imageDataUri(raw);
    const copy = copyFor(config, scene, locale);
    available.push({
      scene,
      frame: {
        sceneId: scene.id,
        image: image.uri,
        aspect: image.height / image.width,
        kicker: copy.kicker,
        headline: copy.headline,
      },
    });
  }
  if (!available.length) return [];

  const written: string[] = [];
  let ordinal = 0;

  for (const group of chunk(available, theme.group)) {
    const { css, body } = theme.render({
      width,
      height,
      frames: group.map((g) => g.frame),
      device: device.frame,
      isTablet,
      assets,
    });

    const canvasWidth = width * group.length;
    const clips: ClipRegion[] = group.map((_, i) => ({
      x: i * width,
      y: 0,
      width,
      height,
    }));

    const pngs = await renderToPngs(htmlDocument(css, body), canvasWidth, height, clips);

    for (let i = 0; i < group.length; i++) {
      const entry = group[i]!;
      const file = storePath(
        platform,
        device.id,
        locale,
        outputName(platform, device, ordinal, entry.scene),
      );
      await ensureParent(file);
      await writeFile(file, pngs[i]!);
      written.push(file);
      ordinal += 1;
    }
  }

  log.ok(`${device.id}/${locale} → ${written.length} immagini ${width}×${height}`);
  return written;
}
