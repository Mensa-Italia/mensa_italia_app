import type { FrameSpec } from "../types.ts";
import type { Assets } from "./assets.ts";
import { deviceCss, deviceHtml } from "./frame.ts";

// ─── Brand tokens (Brandbook 2020, mirrored from AppTheme.swift / Color.kt) ──

export const BRAND = {
  blue: "#184295",
  blueDeep: "#0D2E6B",
  cyan: "#6AC9F0",
  ink: "#575656",
  night: "#061F2E",
  nightDeep: "#04121C",
  parchment: "#FCFBF7",
} as const;

// ─── Theme contract ──────────────────────────────────────────────────────────

export interface RenderFrame {
  sceneId: string;
  /** Raw capture as a data URI. */
  image: string;
  /** height / width of the raw capture. */
  aspect: number;
  kicker?: string;
  headline: string[];
}

export interface ThemeContext {
  /** Width of a single finished store asset. */
  width: number;
  height: number;
  frames: RenderFrame[];
  device: FrameSpec;
  isTablet: boolean;
  assets: Assets;
}

export interface Theme {
  id: string;
  label: string;
  /** Frames composed on one canvas. `"all"` renders the whole set as a strip. */
  group: number | "all";
  render(ctx: ThemeContext): { css: string; body: string };
}

// ─── Shared helpers ──────────────────────────────────────────────────────────

const escapeHtml = (s: string) =>
  s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

/**
 * Picks a headline size that fits `maxWidth`. Gotham Bold averages ≈ 0.55em of
 * advance per character, which is close enough to keep long Italian strings
 * from overflowing without measuring in the browser.
 */
function fitHeadline(lines: string[], maxWidth: number, preferred: number): number {
  const longest = lines.reduce((n, l) => Math.max(n, l.length), 1);
  return Math.min(preferred, maxWidth / (longest * 0.55));
}

function headlineHtml(lines: string[]): string {
  return lines.map((l) => `<span>${escapeHtml(l)}</span>`).join("");
}

/** Fine film grain: keeps large flat gradients from banding. */
const GRAIN_URI =
  "data:image/svg+xml;base64," +
  Buffer.from(
    `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="160"><filter id="n"><feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="3" stitchTiles="stitch"/></filter><rect width="160" height="160" filter="url(#n)" opacity="0.5"/></svg>`,
  ).toString("base64");

function baseCss(assets: Assets, canvasWidth: number, canvasHeight: number): string {
  return `
${assets.gothamSrc ? `@font-face { font-family: "Gotham"; src: ${assets.gothamSrc}; font-weight: 700; font-display: block; }` : ""}
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: ${canvasWidth}px; height: ${canvasHeight}px; overflow: hidden; }
.canvas {
  position: relative;
  width: ${canvasWidth}px;
  height: ${canvasHeight}px;
  overflow: hidden;
  font-family: "Gotham", "Avenir Next", "Helvetica Neue", Arial, sans-serif;
  font-weight: 700;
  -webkit-font-smoothing: antialiased;
  text-rendering: geometricPrecision;
}
.grain {
  position: absolute;
  inset: 0;
  background-image: url(${GRAIN_URI});
  background-size: 160px 160px;
  mix-blend-mode: overlay;
  pointer-events: none;
}
.copy { position: absolute; display: flex; flex-direction: column; }
.copy__kicker { text-transform: uppercase; }
.copy__headline { display: flex; flex-direction: column; }
.mark { display: block; }
.mark svg { width: 100%; height: 100%; display: block; }
${deviceCss()}
`;
}

// ─── aurora: deep brand night, the default ───────────────────────────────────

const aurora: Theme = {
  id: "aurora",
  label: "Aurora: notte brand, testo chiaro",
  group: 1,
  render(ctx) {
    const { width: W, height: H, isTablet, assets } = ctx;
    const frame = ctx.frames[0]!;
    const u = W / 100;

    const padX = W * 0.085;
    const contentWidth = W - padX * 2;
    const kickerSize = u * (isTablet ? 1.9 : 2.7);
    const headlineSize = fitHeadline(
      frame.headline,
      contentWidth,
      u * (isTablet ? 5.3 : 8.0),
    );
    const lineHeight = 1.07;
    const markSize = u * (isTablet ? 5.2 : 7.4);

    const copyTop = H * 0.072;
    const copyHeight =
      markSize + u * 3.4 + kickerSize * 1.6 + frame.headline.length * headlineSize * lineHeight;
    const deviceTop = copyTop + copyHeight + H * 0.045;

    // Let the device run past the bottom edge by ~12% for a cinematic crop,
    // but never make it so wide that it hits the side padding.
    const available = H * 1.12 - deviceTop;
    const deviceWidth = Math.min(available / frame.aspect, W * (isTablet ? 0.78 : 0.86));

    return {
      css: `
${baseCss(assets, W, H)}
.canvas {
  background:
    radial-gradient(${W * 0.9}px ${W * 0.9}px at 8% 2%, rgba(24,66,149,0.92), transparent 62%),
    radial-gradient(${W * 0.7}px ${W * 0.7}px at 99% 22%, rgba(106,201,240,0.30), transparent 60%),
    radial-gradient(${W * 1.3}px ${W * 1.0}px at 52% 106%, rgba(13,46,107,0.98), transparent 66%),
    linear-gradient(178deg, ${BRAND.nightDeep} 0%, ${BRAND.night} 100%);
}
.grain { opacity: 0.05; }
.copy { left: ${padX}px; top: ${copyTop}px; width: ${contentWidth}px; }
.mark { width: ${markSize}px; height: ${markSize}px; color: ${BRAND.cyan}; opacity: 0.95; margin-bottom: ${u * 3.4}px; }
.copy__kicker {
  font-size: ${kickerSize}px;
  letter-spacing: ${kickerSize * 0.2}px;
  color: ${BRAND.cyan};
  margin-bottom: ${u * 1.8}px;
}
.copy__headline {
  font-size: ${headlineSize}px;
  line-height: ${lineHeight};
  letter-spacing: ${headlineSize * -0.025}px;
  color: #F4F8FF;
}
.device--hero {
  left: 50%;
  top: ${deviceTop}px;
  transform: translateX(-50%);
  filter: drop-shadow(0 ${u * 5}px ${u * 9}px rgba(0,0,0,0.55));
}
`,
      body: `
<div class="canvas">
  <div class="copy">
    ${assets.logoSvg ? `<span class="mark">${assets.logoSvg}</span>` : ""}
    ${frame.kicker ? `<span class="copy__kicker">${escapeHtml(frame.kicker)}</span>` : ""}
    <span class="copy__headline">${headlineHtml(frame.headline)}</span>
  </div>
  ${deviceHtml({
    frame: ctx.device,
    image: frame.image,
    width: deviceWidth,
    aspect: frame.aspect,
    className: "device--hero",
  })}
  <div class="grain"></div>
</div>`,
    };
  },
};

// ─── parchment: light editorial ──────────────────────────────────────────────

const parchment: Theme = {
  id: "parchment",
  label: "Parchment: chiaro editoriale, inchiostro su carta",
  group: 1,
  render(ctx) {
    const { width: W, height: H, isTablet, assets } = ctx;
    const frame = ctx.frames[0]!;
    const u = W / 100;

    const padX = W * 0.09;
    const contentWidth = W - padX * 2;
    const kickerSize = u * (isTablet ? 1.8 : 2.6);
    const headlineSize = fitHeadline(
      frame.headline,
      contentWidth,
      u * (isTablet ? 5.0 : 7.6),
    );
    const lineHeight = 1.08;
    const rule = u * 0.55;

    const copyTop = H * 0.085;
    const copyHeight =
      kickerSize * 1.7 + frame.headline.length * headlineSize * lineHeight + rule + u * 4;
    const deviceTop = copyTop + copyHeight + H * 0.05;
    const available = H * 1.1 - deviceTop;
    const deviceWidth = Math.min(available / frame.aspect, W * (isTablet ? 0.76 : 0.84));
    const grid = u * 5;

    return {
      css: `
${baseCss(assets, W, H)}
.canvas {
  background:
    radial-gradient(${W * 1.2}px ${W * 0.9}px at 50% 104%, rgba(24,66,149,0.20), transparent 68%),
    radial-gradient(${W * 0.8}px ${W * 0.8}px at 96% 6%, rgba(106,201,240,0.16), transparent 62%),
    ${BRAND.parchment};
}
.blueprint {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(24,66,149,0.055) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(24,66,149,0.055) 1px, transparent 1px);
  background-size: ${grid}px ${grid}px;
  mask-image: linear-gradient(180deg, rgba(0,0,0,0.9), rgba(0,0,0,0.12) 62%, transparent);
}
.grain { opacity: 0.035; mix-blend-mode: multiply; }
.copy { left: ${padX}px; top: ${copyTop}px; width: ${contentWidth}px; }
.copy__kicker {
  font-size: ${kickerSize}px;
  letter-spacing: ${kickerSize * 0.2}px;
  color: ${BRAND.blue};
  margin-bottom: ${u * 1.6}px;
}
.copy__headline {
  font-size: ${headlineSize}px;
  line-height: ${lineHeight};
  letter-spacing: ${headlineSize * -0.028}px;
  color: #10141B;
}
.copy__rule {
  width: ${u * 14}px;
  height: ${rule}px;
  border-radius: ${rule}px;
  background: linear-gradient(90deg, ${BRAND.blue}, ${BRAND.cyan});
  margin-top: ${u * 3.6}px;
}
.device--hero {
  left: 50%;
  top: ${deviceTop}px;
  transform: translateX(-50%);
  filter: drop-shadow(0 ${u * 4}px ${u * 7}px rgba(16,24,52,0.26));
}
`,
      body: `
<div class="canvas">
  <div class="blueprint"></div>
  <div class="copy">
    ${frame.kicker ? `<span class="copy__kicker">${escapeHtml(frame.kicker)}</span>` : ""}
    <span class="copy__headline">${headlineHtml(frame.headline)}</span>
    <span class="copy__rule"></span>
  </div>
  ${deviceHtml({
    frame: ctx.device,
    image: frame.image,
    width: deviceWidth,
    aspect: frame.aspect,
    className: "device--hero",
  })}
  <div class="grain"></div>
</div>`,
    };
  },
};

// ─── panorama: one continuous tilted band across the whole set ───────────────

const panorama: Theme = {
  id: "panorama",
  label: "Panorama: banda continua inclinata (evoluzione del set attuale)",
  group: "all",
  render(ctx) {
    const { width: W, height: H, isTablet, assets } = ctx;
    const n = ctx.frames.length;
    const canvasWidth = W * n;
    const u = W / 100;

    const padX = W * 0.075;
    const copyWidth = W * (isTablet ? 0.52 : 0.56);
    const kickerSize = u * (isTablet ? 1.8 : 2.6);
    const headlineSize = fitHeadline(
      ctx.frames.flatMap((f) => f.headline),
      copyWidth,
      u * (isTablet ? 4.4 : 6.6),
    );
    const markSize = u * (isTablet ? 4.6 : 6.6);
    const tilt = -17;
    const deviceWidth = W * (isTablet ? 0.70 : 0.80);
    // Devices sit low and right of centre: the tilt frees the top-left wedge
    // for the copy while the body runs past the frame seam into the next shot.
    const deviceCenterY = H * (isTablet ? 0.64 : 0.60);
    const deviceCenterX = W * 0.86;

    const devices = ctx.frames
      .map((f, i) =>
        deviceHtml({
          frame: ctx.device,
          image: f.image,
          width: deviceWidth,
          aspect: f.aspect,
          className: "device--band",
          style: `left:${(i * W + deviceCenterX).toFixed(2)}px;top:${deviceCenterY.toFixed(2)}px;transform:translate(-50%,-50%) rotate(${tilt}deg)`,
        }),
      )
      .join("\n");

    const copies = ctx.frames
      .map(
        (f, i) => `
  <div class="copy" style="left:${(i * W + padX).toFixed(2)}px">
    ${i === 0 && assets.logoSvg ? `<span class="mark">${assets.logoSvg}</span>` : ""}
    ${f.kicker ? `<span class="copy__kicker">${escapeHtml(f.kicker)}</span>` : ""}
    <span class="copy__headline">${headlineHtml(f.headline)}</span>
  </div>`,
      )
      .join("\n");

    return {
      css: `
${baseCss(assets, canvasWidth, H)}
.canvas {
  background:
    radial-gradient(${W * 1.1}px ${W * 1.1}px at 6% -6%, rgba(255,255,255,0.95), transparent 58%),
    radial-gradient(${W * 1.4}px ${W * 1.1}px at 78% 108%, rgba(24,66,149,0.42), transparent 66%),
    radial-gradient(${W * 0.9}px ${W * 0.9}px at 40% 42%, rgba(106,201,240,0.20), transparent 62%),
    linear-gradient(148deg, #FBF7FA 0%, #E9EAF8 38%, #C7D3F2 72%, #A9BCEA 100%);
}
.grain { opacity: 0.04; mix-blend-mode: multiply; }
.copy {
  top: ${(H * 0.135).toFixed(2)}px;
  width: ${copyWidth.toFixed(2)}px;
}
.mark { width: ${markSize}px; height: ${markSize}px; color: #0F1730; margin-bottom: ${u * 3}px; }
.copy__kicker {
  font-size: ${kickerSize}px;
  letter-spacing: ${kickerSize * 0.2}px;
  color: ${BRAND.blue};
  margin-bottom: ${u * 1.5}px;
}
.copy__headline {
  font-size: ${headlineSize}px;
  line-height: 1.08;
  letter-spacing: ${headlineSize * -0.028}px;
  color: #0F1730;
}
.device--band {
  filter: drop-shadow(0 ${u * 4}px ${u * 8}px rgba(15,23,48,0.30));
}
`,
      body: `
<div class="canvas">
  ${devices}
  ${copies}
  <div class="grain"></div>
</div>`,
    };
  },
};

export const THEMES: Theme[] = [aurora, parchment, panorama];

export function themeById(id: string): Theme {
  const theme = THEMES.find((t) => t.id === id);
  if (!theme) {
    throw new Error(
      `Unknown theme "${id}". Available: ${THEMES.map((t) => t.id).join(", ")}`,
    );
  }
  return theme;
}

export function htmlDocument(css: string, body: string): string {
  return `<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${body}</body></html>`;
}

export { escapeHtml };
