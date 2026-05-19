# Mensa Italia — App

[![Release](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/release.yml)
[![Quality](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/quality.yml/badge.svg?branch=master)](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/quality.yml)
[![Latest release](https://img.shields.io/github/v/release/Mensa-Italia/mensa_italia_app?label=release&color=blue)](https://github.com/Mensa-Italia/mensa_italia_app/releases/latest)
[![Web image](https://img.shields.io/badge/ghcr.io-mensa--web-2496ED?logo=docker&logoColor=white)](https://github.com/Mensa-Italia/mensa_italia_app/pkgs/container/mensa-web)

App ufficiale di **Mensa Italia**: tessera digitale, eventi, sedi locali, notifiche, area pubblica, console editoriale, autenticazione e onboarding nuovi soci.

Tre client nativi che condividono lo stesso core di business logic scritto in **Kotlin Multiplatform**.

---

## Architettura

```
┌──────────────────────────────────────────────────────────────────┐
│  shared/        Kotlin Multiplatform                             │
│  ─────────────  ─────────────────────────────────────────────    │
│                 API client (Ktor), repositories, modelli,        │
│                 DB locale (SQLDelight), realtime (SSE),          │
│                 auth & TokenStore, DI (Koin)                     │
│                                                                  │
│  Targets: jvm (Android), iosX64/Arm64, jsIr (Web)                │
└──────────────┬───────────────┬─────────────────┬─────────────────┘
               │               │                 │
        ┌──────▼──────┐ ┌──────▼──────┐  ┌───────▼────────┐
        │ androidApp/ │ │  iosApp/    │  │  webApp/       │
        │             │ │             │  │                │
        │ Jetpack     │ │ SwiftUI +   │  │ Astro SSR +    │
        │ Compose,    │ │ Liquid      │  │ React 19 +     │
        │ Material 3  │ │ Glass (iOS  │  │ Tailwind 4 +   │
        │ Expressive  │ │ 26)         │  │ Keystatic CMS  │
        └─────────────┘ └─────────────┘  └────────────────┘
```

| Modulo | Stack | Output |
|---|---|---|
| `shared/` | Kotlin Multiplatform, Ktor 3, SQLDelight, Koin 5 | `.aar` (Android), XCFramework (iOS), JS lib (Web) |
| `androidApp/` | Jetpack Compose, Material 3 Expressive, Coil 3 | `.aab` + `.apk` |
| `iosApp/` | SwiftUI iOS 26, Liquid Glass, Stripe SDK, Firebase | `.ipa` |
| `webApp/` | Astro 6 SSR, React 19, Tailwind 4, Keystatic, Unlayer | Docker image `ghcr.io/mensa-italia/mensa-web` |
| `tools/` | Bash + Tolgee | Sincronizzazione traduzioni i18n |

---

## Pipeline di release

Tutto orchestrato da [`.github/workflows/release.yml`](.github/workflows/release.yml). Un singolo workflow gestisce **web, Android e iOS in parallelo** con gating di qualità.

### Trigger

| Trigger | Effetto |
|---|---|
| Push a `master` con `[ALPHA]` nel subject | web `:alpha` + Play `internal` + TestFlight (interno) |
| Push a `master` con `[BETA]` nel subject | web `:beta` + Play `beta` + TestFlight gruppo `Test open` |
| Push a `master` con `[RELEASE]` nel subject | web `:latest` + Play `production` + App Store review |
| Push a `feat/mvp-testing-public` | solo web `:dev` (niente native) |
| `workflow_dispatch` | input `track` scelto dalla UI |

Il bump versione è automatico (`VERSION` file, +1 patch). `versionCode` Android = `30000000 + git commit count` (sopra al legacy Flutter). `CFBundleVersion` iOS allineato.

### DAG

```
prepare ─┬─> web        ─┐
         ├─> android   ─┐ │
         │              ↓ │
         │       play-publish (track-aware)
         │                ↓
         ├─> ios          │
         │    ↓           │
         │  testflight-publish (fastlane alpha/beta/release)
         │                ↓
         ├─> lint-web, typecheck-web, lint-kotlin, lint-swift,
         │   lint-dockerfile, scan-secrets, scan-vulns,
         │   codeql-js, codeql-kotlin, codeql-swift
         │   └─> gate-summary (aggregator)
         │            ↓
         └─────> finalize (tag + GitHub Release con AAB+APK+IPA)
                       ↓
                cleanup-old-runs (runs > 24h)
```

### Quality gate

I dieci job sopra alimentano `gate-summary`: se anche solo uno fallisce il publish è bloccato. **Eccezione: track `alpha` bypassa il gate** (per testing rapido sui propri device).

---

## Quality & Security

Due flussi distinti.

### `quality.yml` — radar su ogni push

Triggera su ogni `push` (incluso `feat/mvp-testing-public`) e su PR a `master`. Modalità **blanda**: ogni job ha `continue-on-error: true`, riporta warning senza far diventare rosso il workflow.

| Job | Cosa controlla | Linguaggio |
|---|---|---|
| `web-typecheck` | `astro check` con `@astrojs/check` | TS/Astro |
| `web-lint` | ESLint 9 flat config | JS/TS/TSX/Astro |
| `kotlin-lint` | detekt 1.23 (default config + custom) | Kotlin |
| `swift-lint` | SwiftLint (regole opt-in) | Swift |
| `dockerfile-lint` | Hadolint | `Dockerfile.web` |
| `secret-scan` | Gitleaks (org-licensed) | tutta la repo |

### `release.yml` — gate strict

Gli stessi check girano anche nel flusso di release ma in modalità **strict** (no `continue-on-error`). In aggiunta:

| Job extra | Cosa controlla |
|---|---|
| `scan-vulns` | Trivy filesystem scan, severity CRITICAL+HIGH |
| `codeql-js` | CodeQL SAST JavaScript/TypeScript |
| `codeql-kotlin` | CodeQL SAST Java/Kotlin |
| `codeql-swift` | CodeQL SAST Swift |

Tutti i SARIF sono caricati nella tab **Security** del repo.

---

## Build e deploy

### Web (Docker container)

L'immagine è pubblicata su **GHCR** ad ogni release. Pull e run con:

```bash
docker pull ghcr.io/mensa-italia/mensa-web:latest
docker run --rm -p 4321:4321 ghcr.io/mensa-italia/mensa-web:latest
```

Per il setup completo con Traefik + HTTPS automatico via Let's Encrypt vedi [`docker-compose.example.yml`](docker-compose.example.yml). Sulla macchina di destinazione:

```bash
DOMAIN=app.mensa.it ACME_EMAIL=tuo@dominio.it docker compose up -d
```

Esiste anche un webhook Portainer che redeploya lo stack `dev` automaticamente quando un build su `feat/mvp-testing-public` finisce con successo ([`notify.yml`](.github/workflows/notify.yml)).

### Android

Build locale:

```bash
./gradlew :androidApp:assembleDebug          # APK di sviluppo
./gradlew :androidApp:bundleRelease          # AAB per Play Store (firmato se hai i secret)
```

Signing in CI: keystore (`KEY_JKS`), password (`KEY_PASSWORD`), alias password (`ALIAS_PASSWORD`). Alias hardcoded = `key`.

### iOS

Build locale:

```bash
cd iosApp
xcodegen generate
open iosApp.xcodeproj
```

Build in CI: macos-14 + Xcode 26. Genera `.xcodeproj` con xcodegen, builda XCFramework Release dello shared, importa cert/profile da secrets, archive + exportArchive, upload TestFlight via fastlane (lane `alpha`/`beta`/`production`).

---

## Sviluppo locale

### Prerequisiti

- **Java 17+** (Android Gradle Plugin)
- **Xcode 26+** (solo per iOS, su Mac)
- **Bun** 1.2+ (per webApp)
- **Android Studio Hedgehog+** (consigliato per Android)
- **xcodegen** (`brew install xcodegen`)

### Web

```bash
cd webApp
bun install
bun run build:shared    # builda mensa-shared via gradle
bun dev                 # http://localhost:4321
```

### Android

```bash
./gradlew :androidApp:installDebug
```

### iOS

```bash
cd iosApp && xcodegen generate && open iosApp.xcodeproj
# Run da Xcode (⌘R)
```

---

## Convenzioni

- **i18n**: chiavi gestite via Tolgee. Vedi [`tools/tolgee-push.sh`](tools/tolgee-push.sh).
- **Versione**: single source of truth nel file [`VERSION`](VERSION) alla radice, bumpata dalla CI nel job `finalize`.
- **Commit-driven release**: solo i token `[ALPHA]`, `[BETA]`, `[RELEASE]` sulla prima riga del subject triggerano la pubblicazione. Body libero.

---

## License

Proprietary — Associazione Mensa Italia ETS.
