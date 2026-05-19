import { defineMiddleware } from "astro:middleware";

const PROTECTED_PREFIXES = [
  "/keystatic",
  "/api/keystatic",
  "/console",
];

const LIGHT_MODE_SCRIPT = `<script>
  try { localStorage.setItem("keystatic-color-scheme", "light"); } catch (_) {}
</script>
<style>
  html.kui-theme { color-scheme: light !important; }
  @media (prefers-color-scheme: dark) {
    html.kui-scheme--auto {
      color-scheme: light !important;
    }
  }
  #mensa-back-btn {
    position: fixed;
    bottom: 20px;
    right: 20px;
    z-index: 99999;
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 8px 14px;
    background: #ffffff;
    color: #1d2f6f;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    font-size: 13px;
    font-weight: 600;
    border: 1.5px solid #c8d0e8;
    border-radius: 20px;
    text-decoration: none;
    box-shadow: 0 2px 8px rgba(29,47,111,0.10);
    transition: border-color 0.15s, box-shadow 0.15s;
  }
  #mensa-back-btn:hover {
    border-color: #1d2f6f;
    box-shadow: 0 3px 12px rgba(29,47,111,0.18);
  }
</style>
<a id="mensa-back-btn" href="/today" aria-label="Torna alla home dell'area soci">← Torna all'area soci</a>`;

export const onRequest = defineMiddleware(async (context, next) => {
  const path = context.url.pathname;
  const isProtected = PROTECTED_PREFIXES.some(
    (p) => path === p || path.startsWith(p + "/"),
  );

  if (isProtected) {
    const cookie = context.request.headers.get("cookie") ?? "";
    const hasSession = /\bmensa_session=1\b/.test(cookie);
    if (!hasSession) {
      return context.redirect(`/login?next=${encodeURIComponent(path)}`);
    }
  }

  const response = await next();

  if (
    (path === "/keystatic" || path.startsWith("/keystatic/")) &&
    response.headers.get("content-type")?.includes("text/html")
  ) {
    const html = await response.text();
    let patched: string;
    if (html.includes("</head>")) {
      patched = html.replace("</head>", `${LIGHT_MODE_SCRIPT}</head>`);
    } else if (html.includes("<!DOCTYPE html>")) {
      patched = html.replace("<!DOCTYPE html>", `<!DOCTYPE html>${LIGHT_MODE_SCRIPT}`);
    } else {
      patched = LIGHT_MODE_SCRIPT + html;
    }
    const headers = new Headers(response.headers);
    headers.delete("content-length");
    return new Response(patched, {
      status: response.status,
      statusText: response.statusText,
      headers,
    });
  }

  return response;
});
