/**
 * Dynamic robots.txt.
 *
 * Disallow /api/, /login, /today, e tutta la dashboard (rotte autenticate)
 * dall'indicizzazione. Permettiamo solo le pagine pubbliche di marketing.
 */
import type { APIRoute } from "astro";

export const GET: APIRoute = ({ site }) => {
  const base = site?.toString().replace(/\/$/, "") ?? "https://app.mensa.it";
  const body = `User-agent: *
Allow: /
Disallow: /api/
Disallow: /login
Disallow: /today
Disallow: /card
Disallow: /tickets
Disallow: /receipts
Disallow: /notifications
Disallow: /profile
Disallow: /members
Disallow: /sigs
Disallow: /deals
Disallow: /chapters
Disallow: /documents
Disallow: /podcasts
Disallow: /quid
Disallow: /boutique
Disallow: /addons
Disallow: /tableport
Disallow: /search

Sitemap: ${base}/sitemap.xml
`;
  return new Response(body, {
    headers: { "Content-Type": "text/plain; charset=utf-8" },
  });
};
