import type { FrameSpec } from "../types.ts";

/**
 * Procedural device bezels.
 *
 * Drawn in CSS rather than composited from vendor mock-up images: it scales to
 * any output resolution without artefacts, keeps the repo free of third-party
 * device renders, and lets the bezel palette track the theme.
 */

export interface DeviceRenderOptions {
  frame: FrameSpec;
  /** Screenshot as a data URI. */
  image: string;
  /** Outer width of the framed device, in canvas pixels. */
  width: number;
  /** height / width of the raw capture. */
  aspect: number;
  /** Extra inline styles for positioning (`position`, `left`, `transform`, …). */
  style?: string;
  /** Extra class names, e.g. a theme-specific shadow. */
  className?: string;
}

const BEZEL_PALETTE: Record<FrameSpec["style"], string> = {
  // Brushed titanium, lit from the top-left.
  iphone:
    "linear-gradient(145deg,#8d9198 0%,#e6e8ec 12%,#6f747c 34%,#c6c9cf 56%,#585d65 78%,#9fa3ab 100%)",
  ipad:
    "linear-gradient(150deg,#8b8f96 0%,#dcdfe4 14%,#70757d 40%,#bcbfc6 64%,#5c6169 100%)",
  // Matte anodised aluminium.
  pixel:
    "linear-gradient(150deg,#54585f 0%,#2a2d33 28%,#43474e 58%,#22252a 82%,#3b3f45 100%)",
  "pixel-tablet":
    "linear-gradient(150deg,#4e525a 0%,#292c32 32%,#3f434a 62%,#232629 100%)",
};

/** Renders one framed device. Returns the markup; pair it with {@link deviceCss}. */
export function deviceHtml(opts: DeviceRenderOptions): string {
  const { frame, image, width, aspect } = opts;
  const bezel = width * frame.bezel;
  const outerRadius = width * frame.radius;
  const innerRadius = Math.max(outerRadius - bezel, outerRadius * 0.72);
  const screenWidth = width - bezel * 2;
  const screenHeight = screenWidth * aspect;
  const height = screenHeight + bezel * 2;

  const vars = [
    `--dev-w:${width.toFixed(2)}px`,
    `--dev-h:${height.toFixed(2)}px`,
    `--dev-bezel:${bezel.toFixed(2)}px`,
    `--dev-radius:${outerRadius.toFixed(2)}px`,
    `--dev-inner-radius:${innerRadius.toFixed(2)}px`,
    `--dev-bezel-fill:${BEZEL_PALETTE[frame.style]}`,
  ].join(";");

  return `<div class="device device--${frame.style} ${opts.className ?? ""}" style="${vars};${opts.style ?? ""}">
  <div class="device__body">
    <div class="device__screen">
      <img class="device__shot" src="${image}" alt="" />
      ${cameraHtml(frame, screenWidth)}
      <div class="device__glare"></div>
    </div>
  </div>
  ${buttonsHtml(frame)}
</div>`;
}

function cameraHtml(frame: FrameSpec, screenWidth: number): string {
  if (frame.camera === "island") {
    const w = screenWidth * 0.3;
    const h = w * 0.3;
    const top = screenWidth * 0.032;
    return `<div class="device__island" style="width:${w.toFixed(2)}px;height:${h.toFixed(
      2,
    )}px;top:${top.toFixed(2)}px"></div>`;
  }
  if (frame.camera === "punch-hole") {
    const d = screenWidth * 0.055;
    const top = screenWidth * 0.026;
    return `<div class="device__punch" style="width:${d.toFixed(2)}px;height:${d.toFixed(
      2,
    )}px;top:${top.toFixed(2)}px"></div>`;
  }
  return "";
}

function buttonsHtml(frame: FrameSpec): string {
  if (frame.style === "iphone") {
    return `
  <span class="device__btn device__btn--left" style="top:19%;height:4.2%"></span>
  <span class="device__btn device__btn--left" style="top:26%;height:7%"></span>
  <span class="device__btn device__btn--left" style="top:35%;height:7%"></span>
  <span class="device__btn device__btn--right" style="top:29%;height:11%"></span>`;
  }
  if (frame.style === "pixel") {
    return `
  <span class="device__btn device__btn--right" style="top:20%;height:6%"></span>
  <span class="device__btn device__btn--right" style="top:28%;height:10%"></span>`;
  }
  return "";
}

/** Stylesheet shared by every device instance on the page. */
export function deviceCss(): string {
  return `
.device {
  width: var(--dev-w);
  height: var(--dev-h);
  position: absolute;
  transform-origin: 50% 50%;
}
.device__body {
  width: 100%;
  height: 100%;
  padding: var(--dev-bezel);
  border-radius: var(--dev-radius);
  background: var(--dev-bezel-fill);
  box-sizing: border-box;
  position: relative;
}
/* Hairline that separates the bezel from the glass. */
.device__body::after {
  content: "";
  position: absolute;
  inset: calc(var(--dev-bezel) * 0.42);
  border-radius: calc(var(--dev-radius) - var(--dev-bezel) * 0.42);
  box-shadow: inset 0 0 0 calc(var(--dev-bezel) * 0.16) rgba(0, 0, 0, 0.55);
  pointer-events: none;
}
.device__screen {
  width: 100%;
  height: 100%;
  border-radius: var(--dev-inner-radius);
  overflow: hidden;
  background: #000;
  position: relative;
}
.device__shot {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: top center;
  display: block;
}
.device__island {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  background: #000;
  border-radius: 999px;
}
.device__punch {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  background: #0a0a0a;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}
/* Very restrained diagonal sheen: enough to read as glass, not a gimmick. */
.device__glare {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    118deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0.03) 18%,
    rgba(255, 255, 255, 0) 42%
  );
  pointer-events: none;
}
.device__btn {
  position: absolute;
  width: calc(var(--dev-bezel) * 0.45);
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255,255,255,0.35), rgba(0,0,0,0.35));
}
.device__btn--left { left: calc(var(--dev-bezel) * -0.28); }
.device__btn--right { right: calc(var(--dev-bezel) * -0.28); }
`;
}
