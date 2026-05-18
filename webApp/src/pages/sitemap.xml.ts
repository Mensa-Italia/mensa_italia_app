/**
 * Dynamic sitemap.xml.
 *
 * Include solo le pagine pubbliche (sito marketing) — no dashboard.
 * I dettagli dei gruppi locali sono enumerati a runtime via `view_local_office`.
 */
import type { APIRoute } from "astro";
import { listPublicLocalOffices } from "../lib/publicApi";

const STATIC_ROUTES: Array<{ path: string; priority: number; changefreq: string }> = [
  { path: "/", priority: 1.0, changefreq: "weekly" },
  { path: "/public/about", priority: 0.9, changefreq: "monthly" },
  { path: "/public/iq-test", priority: 0.9, changefreq: "monthly" },
  { path: "/public/events", priority: 0.8, changefreq: "weekly" },
  { path: "/public/chapters", priority: 0.8, changefreq: "weekly" },
  { path: "/public/podcasts", priority: 0.7, changefreq: "weekly" },
  { path: "/public/quid", priority: 0.7, changefreq: "weekly" },
  { path: "/login", priority: 0.4, changefreq: "yearly" },
];

function xmlEscape(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export const GET: APIRoute = async ({ site }) => {
  const base = site?.toString().replace(/\/$/, "") ?? "https://app.mensa.it";
  const today = new Date().toISOString().slice(0, 10);

  let offices: Array<{ slug: string }> = [];
  try {
    offices = (await listPublicLocalOffices()).map((o) => ({ slug: o.slug }));
  } catch {
    // Sitemap fallback: omit dynamic entries if PB is down.
  }

  const urls = [
    ...STATIC_ROUTES.map((r) => ({
      loc: `${base}${r.path}`,
      lastmod: today,
      priority: r.priority,
      changefreq: r.changefreq,
    })),
    ...offices.map((o) => ({
      loc: `${base}/public/chapters/${o.slug}`,
      lastmod: today,
      priority: 0.6,
      changefreq: "monthly",
    })),
  ];

  const body = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls
  .map(
    (u) => `  <url>
    <loc>${xmlEscape(u.loc)}</loc>
    <lastmod>${u.lastmod}</lastmod>
    <changefreq>${u.changefreq}</changefreq>
    <priority>${u.priority.toFixed(1)}</priority>
  </url>`,
  )
  .join("\n")}
</urlset>
`;

  return new Response(body, {
    headers: { "Content-Type": "application/xml; charset=utf-8" },
  });
};
