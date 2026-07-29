import { readFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { FONT_GOTHAM, LOGO_MARK_SVG } from "../paths.ts";

export interface Assets {
  /** `@font-face` src for Gotham Bold, or "" when the font is unavailable. */
  gothamSrc: string;
  /** The Mensa mark as an inline SVG string (fill is set via `currentColor`). */
  logoSvg: string;
}

let cached: Assets | null = null;

/** Loads the brand font and mark straight out of the app sources. */
export async function loadAssets(): Promise<Assets> {
  if (cached) return cached;

  let gothamSrc = "";
  if (existsSync(FONT_GOTHAM)) {
    const font = await readFile(FONT_GOTHAM);
    gothamSrc = `url(data:font/otf;base64,${font.toString("base64")}) format("opentype")`;
  }

  let logoSvg = "";
  if (existsSync(LOGO_MARK_SVG)) {
    logoSvg = (await readFile(LOGO_MARK_SVG, "utf8"))
      // Drop the XML prolog so it can be inlined in HTML.
      .replace(/<\?xml[^>]*\?>/g, "")
      // Let CSS drive the colour.
      .replace(/fill:\s*#000;?/g, "fill: currentColor;")
      .replace(/<svg /, '<svg fill="currentColor" ');
  }

  cached = { gothamSrc, logoSvg };
  return cached;
}

/** Reads width/height out of a PNG's IHDR chunk. */
export function pngSize(buffer: Buffer): { width: number; height: number } {
  const signature = buffer.subarray(0, 8).toString("hex");
  if (signature !== "89504e470d0a1a0a") {
    throw new Error("Not a PNG file, cannot read its dimensions.");
  }
  return { width: buffer.readUInt32BE(16), height: buffer.readUInt32BE(20) };
}

export async function imageDataUri(path: string): Promise<{
  uri: string;
  width: number;
  height: number;
}> {
  const buffer = await readFile(path);
  const { width, height } = pngSize(buffer);
  return { uri: `data:image/png;base64,${buffer.toString("base64")}`, width, height };
}
