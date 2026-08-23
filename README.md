# Mensa Italia — App

### Pipeline

[![Pipeline](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/pipeline.yml/badge.svg?branch=main)](https://github.com/Mensa-Italia/mensa_italia_app/actions/workflows/pipeline.yml)
[![Latest release](https://img.shields.io/github/v/release/Mensa-Italia/mensa_italia_app?label=release&color=blue)](https://github.com/Mensa-Italia/mensa_italia_app/releases/latest)
[![Tag latest](https://img.shields.io/github/v/tag/Mensa-Italia/mensa_italia_app?label=tag&color=lightgrey)](https://github.com/Mensa-Italia/mensa_italia_app/tags)

### Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Swift](https://img.shields.io/badge/Swift-F05138?logo=swift&logoColor=white)
![SwiftUI](https://img.shields.io/badge/SwiftUI-007AFF?logo=swift&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white)
![Ktor](https://img.shields.io/badge/Ktor-087CFA?logo=kotlin&logoColor=white)
![SQLDelight](https://img.shields.io/badge/SQLDelight-003545?logo=sqlite&logoColor=white)
![Koin](https://img.shields.io/badge/Koin-FFD700?logoColor=black)
![Stripe](https://img.shields.io/badge/Stripe-635BFF?logo=stripe&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?logo=firebase&logoColor=black)
![PocketBase](https://img.shields.io/badge/PocketBase-B8DBE4?logo=pocketbase&logoColor=black)

### Platforms

![iOS 26+](https://img.shields.io/badge/iOS-26+-000?logo=apple&logoColor=white)
![Android 7+](https://img.shields.io/badge/Android-7+%20(SDK%2024)-3DDC84?logo=android&logoColor=white)

App ufficiale di **Mensa Italia**: tessera digitale, eventi, sedi locali, notifiche, area pubblica, autenticazione e onboarding nuovi soci.

Due client nativi che condividono lo stesso core di business logic scritto in **Kotlin Multiplatform**.

> Il client web viveva in `webApp/` (Astro + React + Keystatic) e non esiste
> piu': e' stato eliminato insieme ai target Kotlin/JS e Kotlin/Wasm della
> shared, al `Dockerfile.web` e a tutti i job della pipeline che pubblicavano
> `ghcr.io/mensa-italia/mensa-web`. Il sito resta a
> [mensa-hub](https://github.com/Mensa-Italia), che quell'immagine la pubblica
> per conto suo.

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
│  Targets: android (jvm), iosX64/Arm64/SimulatorArm64, watchOS    │
└──────────────────────┬───────────────┬───────────────────────────┘
                       │               │
                ┌──────▼──────┐ ┌──────▼──────┐
                │ androidApp/ │ │  iosApp/    │
                │             │ │             │
                │ Jetpack     │ │ SwiftUI +   │
                │ Compose,    │ │ Liquid      │
                │ Material 3  │ │ Glass (iOS  │
                │ Expressive  │ │ 26)         │
                └─────────────┘ └─────────────┘
```

| Modulo | Stack | Output |
|---|---|---|
| `shared/` | Kotlin Multiplatform, Ktor 3, SQLDelight, Koin 5 | `.aar` (Android), XCFramework (iOS + watchOS) |
| `androidApp/` | Jetpack Compose, Material 3 Expressive, Coil 3 | `.aab` + `.apk` |
| `iosApp/` | SwiftUI iOS 26, Liquid Glass, Stripe SDK, Firebase | `.ipa` |
| `tools/` | Bash + Tolgee | Sincronizzazione traduzioni i18n |
| `tools/storekit/` | Bun + TypeScript, simctl/adb, Chromium headless | Screenshot e metadata per App Store e Google Play |

---

## Pipeline

Un solo workflow, [`.github/workflows/pipeline.yml`](.github/workflows/pipeline.yml), secondo la convenzione dell'organizzazione ([Mensa-Italia/.github](https://github.com/Mensa-Italia/.github), `PIPELINE.md`). Cosa succede lo decide il ref, non il nome del file.

### Livelli

| Trigger | Livello | Controlli | Pubblica |
|---|---|---|---|
| PR verso `main` | `check` | detekt + segreti | niente |
| Tag `bX.Y.Z` | `staging` | base + segreti + dipendenze + SBOM | Play `beta` + TestFlight gruppo `Test open` |
| Tag `vX.Y.Z` | `release` | tutti | Play `production` + App Store review + Release firmata |

Un push su `main` **non fa partire niente**: i livelli `dev` e `audit` esistono in `plan.yml` ma qui i trigger corrispondenti non ci sono, perché erano legati all'immagine Docker. Per far compilare Android e iOS su CI serve un tag.

Da quando `webApp/` è stato eliminato la pipeline non costruisce più nessuna immagine: `plan` viene chiamato senza input `image` e i job `image`, `image-release` e `notify` non esistono più. Restano solo gli artefatti mobili.

`workflow_dispatch` ha un input `tier` per forzare un livello a mano; il default `auto` si comporta come un push normale. Forzare `staging` o `release` richiede comunque di essere su un tag: senza tag non esiste una versione, e `plan.yml` si ferma con un errore invece di pubblicare qualcosa senza nome.

La versione viene dal tag (`vX.Y.Z` → `1.2.3`), non più da un bump automatico del file `VERSION`. `versionCode` Android = `30000000 + git commit count` (sopra al legacy Flutter). `CFBundleVersion` iOS allineato.

### DAG

```
plan ─┬─> check (detekt) ────────────────────────────────┐
      ├─> segreti (gitleaks, ogni livello) ─────────────┤
      ├─> deep (OSV + SBOM; staging, release, audit) ───┤
      ├─> codeql-kotlin / codeql-swift                   │  (non bloccanti)
      │                                                  │
      └─> version-code ─┬─> android ─> play ────────────┤
                        └─> ios ─────> testflight ──────┤
                             └─> bundle ────────────────┴─> publish (Release firmata)
```

### Cancelli

I job che pubblicano leggono i `result` espliciti dei controlli:

| Job | Deve essere verde |
|---|---|
| `play`, `testflight` | `check`, `segreti`, `deep`, build corrispondente |
| `publish` | `check`, `segreti`, `deep`, `bundle` |

I due job CodeQL **non** compaiono in quei cancelli e sono `continue-on-error`: l'organizzazione è sul piano free, senza Advanced Security, e l'API code-scanning risponde `403`. Un CodeQL dentro i `needs` di `publish` renderebbe `publish` irraggiungibile per sempre. Vanno rimessi bloccanti quando l'organizzazione avrà Advanced Security.

---

## Build e deploy

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

Build in CI: `macos-latest` + Xcode `latest-stable`. Genera `.xcodeproj` con xcodegen, builda XCFramework Release dello shared, importa cert/profile da secrets, archive + exportArchive, upload TestFlight via fastlane (lane `beta` a staging, `production` a release). Finché il billing macOS è bloccato i job macOS sono `continue-on-error`: la Release esce comunque, con i soli artefatti Android.

---

## Sviluppo locale

### Prerequisiti

- **Java 17+** (Android Gradle Plugin)
- **Xcode 26+** (solo per iOS, su Mac)
- **Android Studio Hedgehog+** (consigliato per Android)
- **xcodegen** (`brew install xcodegen`)
- **Bun** 1.2+ (solo per `tools/storekit`)

### Android

```bash
./gradlew :androidApp:installDebug
```

### iOS

```bash
cd iosApp && xcodegen generate && open iosApp.xcodeproj
# Run da Xcode (⌘R)
```

### Asset store (screenshot + metadata)

[`tools/storekit`](tools/storekit/README.md) esegue le app vere su simulatore
iOS ed emulatore Android, cattura le schermate, le incornicia e produce
l'albero che `deliver` e `supply` si aspettano — screenshot iPhone 6.9" e
iPad 13", phone e tablet Android, feature graphic 1024×500, icona 512×512 e
testi di listing per lingua.

```bash
cd tools/storekit
bun install
bun run doctor            # verifica toolchain
bun run preview           # confronta i temi, nessun device richiesto
bun run all               # capture → render → assets → deliver
```

Le scene si raggiungono con i punti di ingresso diretti delle app
(`--initial-tab` / `MENSA_LAUNCH_SCREEN` su iOS, `LaunchHarness` con extra
d'intent su Android, attivo solo su build debuggable), non simulando tap.

---

## Convenzioni

- **i18n**: chiavi gestite via Tolgee. Vedi [`tools/tolgee-push.sh`](tools/tolgee-push.sh).
- **Versione**: la decide il tag. `vX.Y.Z` per una release, `bX.Y.Z` per una staging; un tag di formato diverso non produce niente. Il file [`VERSION`](VERSION) viene scritto dalla CI a partire dal tag, per gradle.
- **Tag-driven release**: si pubblica taggando, non scrivendo token nel subject del commit. Niente più `[ALPHA]`/`[BETA]`/`[RELEASE]`, niente più bypass del quality gate.

---

## License

Proprietary — Associazione Mensa Italia ETS.
