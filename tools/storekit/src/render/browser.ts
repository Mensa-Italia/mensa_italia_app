import puppeteer, { type Browser, type Page } from "puppeteer-core";
import { chromeExecutable } from "../env.ts";

let browser: Browser | null = null;

/** Lazily launches (and reuses) a headless Chromium for the whole run. */
export async function getBrowser(): Promise<Browser> {
  if (browser) return browser;
  browser = await puppeteer.launch({
    executablePath: chromeExecutable(),
    headless: true,
    args: [
      "--no-sandbox",
      "--disable-dev-shm-usage",
      "--hide-scrollbars",
      "--force-color-profile=srgb",
      // Deterministic text metrics across machines.
      "--font-render-hinting=none",
      "--disable-lcd-text",
      "--allow-file-access-from-files",
    ],
  });
  return browser;
}

export async function closeBrowser(): Promise<void> {
  await browser?.close();
  browser = null;
}

export interface ClipRegion {
  x: number;
  y: number;
  width: number;
  height: number;
}

/**
 * Renders `html` on a `width × height` canvas and returns one PNG per clip
 * region (or a single full-canvas PNG when no regions are given).
 */
export async function renderToPngs(
  html: string,
  width: number,
  height: number,
  clips?: ClipRegion[],
): Promise<Buffer[]> {
  const page: Page = await (await getBrowser()).newPage();
  try {
    await page.setViewport({ width, height, deviceScaleFactor: 1 });
    await page.setContent(html, { waitUntil: "load" });
    // Guarantee webfonts are decoded before the first paint we keep.
    await page.evaluate(() => document.fonts.ready);

    if (!clips?.length) {
      const buffer = await page.screenshot({ type: "png" });
      return [Buffer.from(buffer)];
    }
    const out: Buffer[] = [];
    for (const clip of clips) {
      const buffer = await page.screenshot({ type: "png", clip, captureBeyondViewport: false });
      out.push(Buffer.from(buffer));
    }
    return out;
  } finally {
    await page.close();
  }
}
