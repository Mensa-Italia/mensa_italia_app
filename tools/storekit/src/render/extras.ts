import { writeFile } from "node:fs/promises";
import { join } from "node:path";
import { log } from "../log.ts";
import { STORE_ROOT, ensureParent } from "../paths.ts";
import type { StorekitConfig } from "../types.ts";
import { loadAssets } from "./assets.ts";
import { renderToPngs } from "./browser.ts";
import { BRAND, escapeHtml, htmlDocument } from "./theme.ts";

/**
 * Google Play feature graphic, 1024×500. Shown at the top of the listing and
 * in editorial placements. No small text: Play crops and overlays it.
 */
export async function renderFeatureGraphic(
  config: StorekitConfig,
  locale: string,
): Promise<string> {
  const W = 1024;
  const H = 500;
  const assets = await loadAssets();
  const text = config.storeText[locale] ?? config.storeText[config.defaultLocale]!;
  const tagline = text.featureGraphicTagline ?? text.subtitle ?? "";

  const css = `
${assets.gothamSrc ? `@font-face { font-family: "Gotham"; src: ${assets.gothamSrc}; font-weight: 700; font-display: block; }` : ""}
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: ${W}px; height: ${H}px; overflow: hidden; }
.fg {
  width: ${W}px;
  height: ${H}px;
  position: relative;
  display: flex;
  align-items: center;
  gap: 46px;
  padding: 0 86px;
  font-family: "Gotham", "Avenir Next", "Helvetica Neue", Arial, sans-serif;
  font-weight: 700;
  color: #fff;
  -webkit-font-smoothing: antialiased;
  background:
    radial-gradient(720px 720px at 6% -20%, rgba(106,201,240,0.42), transparent 62%),
    radial-gradient(820px 620px at 96% 118%, rgba(4,18,28,0.85), transparent 66%),
    linear-gradient(118deg, ${BRAND.blue} 0%, ${BRAND.blueDeep} 52%, ${BRAND.night} 100%);
}
/* Faint concentric rings, echoing the globe in the mark. */
.fg::before {
  content: "";
  position: absolute;
  right: -120px;
  top: 50%;
  width: 620px;
  height: 620px;
  transform: translateY(-50%);
  border-radius: 50%;
  background:
    repeating-radial-gradient(circle, rgba(255,255,255,0.07) 0 1px, transparent 1px 58px);
  opacity: 0.7;
}
.fg__mark { width: 152px; height: 152px; color: #fff; flex: none; position: relative; }
.fg__mark svg { width: 100%; height: 100%; display: block; }
.fg__text { position: relative; }
.fg__name { font-size: 78px; letter-spacing: -2px; line-height: 1; }
.fg__tagline {
  font-size: 27px;
  letter-spacing: 0.4px;
  margin-top: 20px;
  color: rgba(255,255,255,0.82);
}
.fg__rule {
  width: 96px;
  height: 6px;
  border-radius: 6px;
  margin-top: 28px;
  background: linear-gradient(90deg, ${BRAND.cyan}, rgba(106,201,240,0));
}
`;

  const body = `
<div class="fg">
  ${assets.logoSvg ? `<span class="fg__mark">${assets.logoSvg}</span>` : ""}
  <div class="fg__text">
    <div class="fg__name">${escapeHtml(config.app.name)}</div>
    <div class="fg__tagline">${escapeHtml(tagline)}</div>
    <div class="fg__rule"></div>
  </div>
</div>`;

  const [png] = await renderToPngs(htmlDocument(css, body), W, H);
  const file = join(STORE_ROOT, "android", "_assets", locale, "featureGraphic.png");
  await ensureParent(file);
  await writeFile(file, png!);
  log.ok(`feature graphic ${locale} → 1024×500`);
  return file;
}

/**
 * Google Play high-res icon, 512×512. Rebuilt from the same mark and brand
 * blue the adaptive launcher icon uses, so the store and the launcher match.
 */
export async function renderPlayIcon(): Promise<string> {
  const S = 512;
  const assets = await loadAssets();

  const css = `
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: ${S}px; height: ${S}px; overflow: hidden; }
.icon {
  width: ${S}px;
  height: ${S}px;
  position: relative;
  display: grid;
  place-items: center;
  background: linear-gradient(160deg, #22539F 0%, ${BRAND.blue} 46%, ${BRAND.blueDeep} 100%);
}
.icon__mark { width: 300px; height: 300px; color: #fff; }
.icon__mark svg { width: 100%; height: 100%; display: block; }
`;

  const body = `<div class="icon"><span class="icon__mark">${assets.logoSvg}</span></div>`;
  const [png] = await renderToPngs(htmlDocument(css, body), S, S);
  const file = join(STORE_ROOT, "android", "_assets", "icon.png");
  await ensureParent(file);
  await writeFile(file, png!);
  log.ok("play icon → 512×512");
  return file;
}
