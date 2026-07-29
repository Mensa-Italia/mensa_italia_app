import { writeFile } from "node:fs/promises";
import { ensureParent } from "../paths.ts";
import { renderToPngs } from "./browser.ts";
import { loadAssets } from "./assets.ts";
import { BRAND, escapeHtml, htmlDocument } from "./theme.ts";

/**
 * Synthetic app screens.
 *
 * `storekit preview` uses these so the artwork direction can be reviewed and
 * tuned without building the apps or booting a device. They approximate the
 * real M3 / SwiftUI layouts. They are never used by `capture` or `render`,
 * which only ever read genuine device captures.
 */

interface MockCopy {
  title: string;
  rows: string[];
  hero?: string;
}

const SCREENS: Record<string, MockCopy> = {
  today: {
    title: "Oggi",
    hero: "MIG Parma · 6–8 settembre",
    rows: ["Assemblea nazionale", "SIG Arte · visita guidata", "Quid n. 214 è online"],
  },
  card: {
    title: "Tessera",
    hero: "Socio dal 2016 · valida al 31/12",
    rows: ["Aggiungi a Wallet", "Mostra QR", "Ricevute e pagamenti"],
  },
  events: {
    title: "Eventi",
    rows: [
      "Aperitivo Mensa Milano",
      "SIG Acquariofilia · incontro",
      "Test di ammissione · Roma",
      "Cena sociale · Napoli",
    ],
  },
  discover: {
    title: "Scopri",
    rows: ["Sedi locali", "Convenzioni", "Documenti ufficiali", "Podcast Quid"],
  },
  search: {
    title: "Cerca",
    rows: ["Soci", "Eventi", "Documenti", "Convenzioni"],
  },
  profile: {
    title: "Profilo",
    rows: ["Notifiche", "Lingua", "Privacy", "Dispositivi collegati"],
  },
};

const NAV = ["Oggi", "Scopri", "Cerca", "Tessera", "Profilo"];

/** Renders a placeholder app screen at `width × height` and writes it to `file`. */
export async function renderMockScreen(
  sceneId: string,
  width: number,
  height: number,
  file: string,
): Promise<string> {
  const assets = await loadAssets();
  const screen = SCREENS[sceneId] ?? {
    title: sceneId,
    rows: ["Elemento uno", "Elemento due", "Elemento tre"],
  };
  const u = width / 100;

  const css = `
${assets.gothamSrc ? `@font-face { font-family: "Gotham"; src: ${assets.gothamSrc}; font-weight: 700; font-display: block; }` : ""}
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: ${width}px; height: ${height}px; overflow: hidden; }
.app {
  width: ${width}px; height: ${height}px; position: relative;
  background: ${BRAND.parchment};
  font-family: "Gotham", "Avenir Next", "Helvetica Neue", Arial, sans-serif;
  font-weight: 700; color: #1A1C22;
  display: flex; flex-direction: column;
}
.status {
  height: ${u * 12}px; flex: none; display: flex; align-items: center;
  justify-content: space-between; padding: ${u * 4}px ${u * 8}px 0;
  font-size: ${u * 4}px; letter-spacing: -0.2px;
}
.status__icons { display: flex; gap: ${u * 1.6}px; align-items: center; }
.status__bar { width: ${u * 1.2}px; border-radius: 2px; background: #1A1C22; }
.status__batt {
  width: ${u * 6.4}px; height: ${u * 3.1}px; border-radius: ${u * 1}px;
  border: ${Math.max(1, u * 0.35)}px solid #1A1C22; position: relative;
}
.status__batt::after {
  content: ""; position: absolute; inset: ${u * 0.6}px; border-radius: ${u * 0.5}px; background: #1A1C22;
}
.body { flex: 1; padding: ${u * 5}px ${u * 6}px 0; overflow: hidden; }
.title { font-size: ${u * 9.5}px; letter-spacing: ${u * -0.22}px; margin-bottom: ${u * 4}px; }
.hero {
  height: ${u * 40}px; border-radius: ${u * 6}px; margin-bottom: ${u * 5}px;
  background:
    radial-gradient(120% 130% at 88% 8%, rgba(106,201,240,0.55), transparent 60%),
    linear-gradient(140deg, ${BRAND.blue}, ${BRAND.blueDeep});
  color: #fff; padding: ${u * 5}px; display: flex; flex-direction: column;
  justify-content: flex-end; position: relative; overflow: hidden;
}
.hero__mark { position: absolute; right: ${u * 5}px; top: ${u * 5}px; width: ${u * 12}px; height: ${u * 12}px; color: rgba(255,255,255,0.35); }
.hero__mark svg { width: 100%; height: 100%; }
.hero__label { font-size: ${u * 3.1}px; letter-spacing: ${u * 0.5}px; text-transform: uppercase; opacity: 0.72; }
.hero__value { font-size: ${u * 5.2}px; margin-top: ${u * 1.4}px; letter-spacing: ${u * -0.1}px; }
.section { font-size: ${u * 3.4}px; text-transform: uppercase; letter-spacing: ${u * 0.5}px; color: #6A6E78; margin-bottom: ${u * 3}px; }
.row {
  display: flex; align-items: center; gap: ${u * 4}px;
  background: #fff; border-radius: ${u * 4.5}px; padding: ${u * 4}px;
  margin-bottom: ${u * 3}px; box-shadow: 0 ${u * 0.6}px ${u * 2}px rgba(16,24,52,0.06);
}
.row__icon {
  width: ${u * 11}px; height: ${u * 11}px; border-radius: ${u * 3.4}px; flex: none;
  background: linear-gradient(150deg, rgba(24,66,149,0.14), rgba(106,201,240,0.22));
}
.row__text { flex: 1; }
.row__title { font-size: ${u * 4.1}px; letter-spacing: ${u * -0.05}px; }
.row__sub { font-size: ${u * 3.2}px; color: #7A7E88; margin-top: ${u * 1.2}px; }
.nav {
  flex: none; height: ${u * 17}px; display: flex; align-items: center;
  justify-content: space-around; background: rgba(255,255,255,0.86);
  border-top: 1px solid rgba(16,24,52,0.07); backdrop-filter: blur(20px);
  padding-bottom: ${u * 3}px;
}
.nav__item { display: flex; flex-direction: column; align-items: center; gap: ${u * 1.4}px; color: #8A8E98; font-size: ${u * 2.7}px; }
.nav__item--on { color: ${BRAND.blue}; }
.nav__dot { width: ${u * 5.5}px; height: ${u * 5.5}px; border-radius: ${u * 2}px; background: currentColor; opacity: 0.9; }
`;

  const activeIndex = Math.max(
    0,
    ["today", "discover", "search", "card", "profile"].indexOf(sceneId),
  );

  const body = `
<div class="app">
  <div class="status">
    <span>9:41</span>
    <span class="status__icons">
      ${[3, 4.4, 5.8, 7.2]
        .map((h) => `<span class="status__bar" style="height:${(u * h).toFixed(2)}px"></span>`)
        .join("")}
      <span class="status__batt"></span>
    </span>
  </div>
  <div class="body">
    <div class="title">${escapeHtml(screen.title)}</div>
    ${
      screen.hero
        ? `<div class="hero">
             ${assets.logoSvg ? `<span class="hero__mark">${assets.logoSvg}</span>` : ""}
             <span class="hero__label">In evidenza</span>
             <span class="hero__value">${escapeHtml(screen.hero)}</span>
           </div>`
        : ""
    }
    <div class="section">${screen.hero ? "Prossimi" : "Tutto"}</div>
    ${screen.rows
      .map(
        (row) => `<div class="row">
        <span class="row__icon"></span>
        <span class="row__text">
          <span class="row__title">${escapeHtml(row)}</span>
          <div class="row__sub">Mensa Italia</div>
        </span>
      </div>`,
      )
      .join("")}
  </div>
  <div class="nav">
    ${NAV.map(
      (label, i) =>
        `<span class="nav__item ${i === activeIndex ? "nav__item--on" : ""}">
           <span class="nav__dot"></span>${escapeHtml(label)}
         </span>`,
    ).join("")}
  </div>
</div>`;

  const [png] = await renderToPngs(htmlDocument(css, body), width, height);
  await ensureParent(file);
  await writeFile(file, png!);
  return file;
}
