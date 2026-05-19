// @ts-check
import { defineConfig } from "astro/config";
import react from "@astrojs/react";
import markdoc from "@astrojs/markdoc";
import keystatic from "@keystatic/astro";
import tailwindcss from "@tailwindcss/vite";
import node from "@astrojs/node";
import { fileURLToPath } from "node:url";

export default defineConfig({
  // Canonical site URL used to compute absolute URLs (canonical link, sitemap,
  // OG meta). Override at build time via `ASTRO_SITE` if deploying to staging.
  // (cache-bust: write-ops wave)
  site: process.env.ASTRO_SITE ?? "https://web.svc.mensa.it",
  // Server-side rendering with Node so dynamic routes like /soci/[id] /eventi/[id]
  // /notifiche/[id] etc. work without per-id getStaticPaths. Static pages
  // (landing, /tessera, /today shell, the placeholder pages) are still
  // pre-rendered via `export const prerender = true` in their frontmatter
  // when we want them static — most pages stay SSR for now.
  output: "server",
  adapter: node({ mode: "standalone" }),
  integrations: [
    react(),
    markdoc(),
    keystatic(),
    {
      name: "keystatic-light-mode",
      hooks: {
        "astro:config:setup": ({ injectScript }) => {
          injectScript(
            "page",
            `if (window.location.pathname.startsWith("/keystatic")) {
              try { localStorage.setItem("keystatic-color-scheme", "light"); } catch (_) {}
              var forceLight = function () {
                var root = document.documentElement;
                if (!root) return;
                root.classList.remove("kui-scheme--auto", "kui-scheme--dark");
                if (!root.classList.contains("kui-scheme--light")) root.classList.add("kui-scheme--light");
                root.style.colorScheme = "light";
              };
              forceLight();
              try {
                var mo = new MutationObserver(forceLight);
                mo.observe(document.documentElement, { attributes: true, attributeFilter: ["class", "style"] });
              } catch (_) {}
            }`,
          );
        },
      },
    },
  ],
  vite: {
    plugins: [tailwindcss()],
    resolve: {
      // bun installs `mensa-shared` as a symlink to ../mensa-kmp/shared/build/.../
      // By default Node resolves `require()` calls from the symlink's REAL path
      // (the KMP build dir), which can't find @js-joda/core / format-util that
      // live in web/node_modules. `preserveSymlinks: true` makes resolution use
      // the symlink path instead, so transitive deps resolve against web's
      // node_modules tree.
      preserveSymlinks: true,
      alias: {
        // The Kotlin/JS shared library declares `ws` as a runtime dep but only
        // uses it under a Node guard. In the browser, the native WebSocket is
        // used. Alias `ws` to a browser stub so Vite's import scan succeeds.
        ws: fileURLToPath(new URL("./src/shims/ws.js", import.meta.url)),
      },
    },
    optimizeDeps: {
      // Pre-bundle ONLY the npm peers the KMP runtime needs at module load.
      // Do NOT pre-bundle `mensa-shared` itself: esbuild's identifier-mangling
      // pass breaks Kotlin/JS's already-mangled method names (e.g. `wi` on
      // SupervisorJob), causing runtime "context.vi is not a function". Let
      // Vite serve the shared.mjs untouched and only the transitive npm deps
      // get the pre-bundle treatment.
      include: ["@js-joda/core", "format-util"],
      exclude: ["mensa-shared"],
    },
    ssr: {
      // The KMP bridge is symlinked from ../mensa-kmp/shared/build/.../, whose
      // internal `require('@js-joda/core')` resolves from the build dir (no
      // node_modules there) and fails Astro SSR. Forcing Vite to bundle it
      // during SSR lets Vite resolve the transitive deps from web/node_modules.
      noExternal: ["mensa-shared", "@js-joda/core", "format-util"],
    },
    server: {
      host: "0.0.0.0", // accessible via Tailscale and LAN
    },
  },
  server: {
    host: "0.0.0.0",
    port: 4321,
  },
});
