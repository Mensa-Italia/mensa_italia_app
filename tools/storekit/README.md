# storekit

Pipeline unica per gli asset degli store: **esegue le app reali**, cattura le
schermate su simulatore iOS ed emulatore Android, le incornicia in un artwork
coerente e impagina tutto nel formato che `deliver` e `supply` si aspettano.

```
build app ──► boot device ──► cattura scene ──► incornicia ──► out/fastlane/**
   ▲              ▲                 ▲               ▲
gradlew /     simctl /          launch args /     Chromium
xcodebuild    emulator          intent extras     headless
```

Nessuna schermata viene disegnata a mano: quello che finisce sugli store è la
UI vera, catturata dalla build corrente.

---

## Requisiti

| Cosa | Perché | Note |
|---|---|---|
| Xcode + simulatori iOS | build e cattura iOS | `xcode-select -p` deve puntare a Xcode |
| `xcodegen` | genera `iosApp.xcodeproj` | `brew install xcodegen` |
| Android SDK + emulator | build e cattura Android | autodetect, oppure `ANDROID_HOME` |
| ~7,4 GB liberi su disco | l'emulatore alloca una userdata da 6 GiB + 20% | verificato prima del boot, vedi sotto |
| JDK 17 o 21 | Gradle 8.10 non parte su Java 24+ | autodetect di `openjdk@17`/`@21` Homebrew, Temurin, JBR |
| Google Chrome (o Chromium) | motore di impaginazione | autodetect, oppure `STOREKIT_CHROME` |
| Bun | runtime del tool | `bun install` in questa cartella |
| fastlane | upload agli store (opzionale) | solo per `fastlane ios upload_screenshots` |

Controlla tutto in una volta:

```bash
cd tools/storekit && bun install && bun run doctor
```

---

## Uso

```bash
cd tools/storekit
cp .env.example .env      # credenziali account demo
bun install

bun run preview           # guarda i temi, nessun device richiesto
bun run all               # capture → render → assets → deliver
```

### Comandi

| Comando | Cosa fa |
|---|---|
| `doctor` | verifica toolchain e configurazione |
| `preview` | rende ogni tema su schermate sintetiche, senza device |
| `capture` | build app, boot device, cattura le scene in `out/raw/**` |
| `render` | incornicia le catture in `out/store/**` alle misure store |
| `assets` | feature graphic Play 1024×500 e icona 512×512 |
| `deliver` | impagina in `out/fastlane/**` per `deliver` e `supply` |
| `all` | tutti i precedenti in sequenza |
| `clean` | svuota `out/` |

### Opzioni utili

```bash
bun run storekit capture --platform ios --devices iphone-6-9 --locales it
bun run storekit capture --scenes today,card --rebuild
bun run storekit capture --platform android --headed     # mostra l'emulatore
bun run storekit render  --theme panorama
```

`--settle-ms`, `--max-wait-ms` e `--warmup-ms` regolano le tempistiche di
cattura se la rete è lenta.

---

## Come vengono raggiunte le schermate

Le scene non si navigano a colpi di tap simulati (fragili): ogni app espone un
punto di ingresso diretto.

**iOS**, già presente nel codice (`iosApp/iosApp/App/iosAppApp.swift`):

| Variabile / argomento | Effetto |
|---|---|
| `--initial-tab today\|discover\|search\|card\|profile` | apre un tab passando dal `RootView` reale |
| `MENSA_LAUNCH_SCREEN=events\|deals\|sigs\|…` | monta direttamente una feature view |
| `MENSA_AUTOLOGIN=1` + `MENSA_EMAIL` / `MENSA_PASSWORD` | invia il form di login vero → sessione autenticata |
| `MENSA_AUTOLOGIN_EMAIL` / `_PWD` | autologin delle sole scene `MENSA_LAUNCH_SCREEN` |
| `MENSA_SKIP_ONBOARDING=1` | in DEBUG ogni login *fresco* atterrerebbe su onboarding |
| `MENSA_REFRESH_ALL=1` | refresh di tutti i repository (warm-up della cache) |
| `MENSA_SUPPRESS_PERMISSION_PROMPTS=1` | niente alert di sistema sopra la cattura |
| `MENSA_DEMO_IDENTITY=1` | dati del socio sostituiti da un segnaposto |

Passano come `SIMCTL_CHILD_*` a `simctl launch`.

> ⚠️ **La build iOS deve essere firmata**, anche solo ad-hoc (`CODE_SIGN_IDENTITY=-`).
> Con `CODE_SIGNING_ALLOWED=NO` l'app non ottiene l'entitlement
> keychain-access-group, ogni chiamata al Keychain fallisce con
> `errSecMissingEntitlement (-34018)` e `TokenStore`, che gira su
> `KeychainSettings`, non può persistere la sessione. `AuthRepository.init()`
> intercetta e ricade su anonimo (lo documenta nel suo stesso commento). Il
> login riesce in memoria, poi `RootView`, che è keyed su `.id(locale.version)`,
> si rimonta quando il catalogo i18n è pronto, rifà `doInit()` e torna alla
> schermata di login. Sintomo: `Login successful` nei log ma la UI resta su
> login.

> ⚠️ Le due famiglie di variabili non sono intercambiabili: `MENSA_AUTOLOGIN`
> passa da `LoginViewModel`, che invia il form vero, mentre
> `MENSA_AUTOLOGIN_EMAIL` chiama `koin.auth.login` direttamente. Quest'ultima
> ritorna un `Result` costruito con `runCatching`: **non lancia mai** attraverso
> il bridge Swift, quindi un fallimento è indistinguibile da un successo e la
> app resta su login senza un solo log di errore.

**Android**: `androidApp/…/support/LaunchHarness.kt`, aggiunto per questa
pipeline e **attivo solo su build debuggable**:

```bash
adb shell am start -n it.mensa.app/.MainActivity \
  --es mensa_screen today \
  --es mensa_autologin_email … --es mensa_autologin_pwd …
```

Gli alias di `mensa_screen` combaciano con i valori iOS, così una sola scena in
`storekit.config.ts` guida entrambe le piattaforme.

Su una build di release la harness è inerte: `LaunchHarness.configure` esce
subito se `FLAG_DEBUGGABLE` non è impostato, e le variabili d'ambiente non sono
iniettabili in un'app installata dallo store.

---

## Dati personali

Le immagini finiscono pubbliche e indicizzate sugli store, quindi storekit
attiva sempre [`DemoIdentity`](../../shared/src/commonMain/kotlin/it/mensa/shared/demo/DemoIdentity.kt):
il socio loggato viene mostrato come *Giulia Bianchi*, senza foto e con id
segnaposto. Il record vero resta intatto nel DB locale e sul backend, si filtra
solo cio' che la UI mostra.

Vengono sostituiti anche `id` e `username`, non solo il nome: la tessera ci
costruisce sopra il QR (`MENSA-IT|id:…|user:…`), che altrimenti finirebbe sugli
store in forma **scansionabile**.

> ⚠️ Il filtro copre il **socio loggato**, non gli altri. Se aggiungi scene che
> mostrano rubrica, registro soci o risultati di ricerca su persone, quelle
> schermate contengono dati di terzi e vanno guardate una per una prima
> dell'upload.

---

## Determinismo delle catture

- **Status bar congelata**: `simctl status_bar override` su iOS, SystemUI demo
  mode (`sysui_demo_allowed`) su Android: 9:41, batteria 100%, tacche piene.
- **Animazioni spente** su Android (`window_animation_scale` e affini).
- **Permessi pre-concessi**: `simctl privacy grant` e `pm grant`, così nessun
  dialogo di sistema copre la schermata.
- **Attesa per stabilità di frame**: si cattura di continuo finché due frame
  consecutivi non sono identici byte per byte, invece di sperare in una `sleep`.
- **Warm-up**: un primo avvio popola la cache SQLDelight locale, così le scene
  successive mostrano contenuti veri e non spinner.

---

## Spazio su disco (Android)

Al **primo** boot di un AVD l'emulatore alloca una partizione userdata da 6 GiB
più il 20% di margine: senza ~7,4 GB liberi si rifiuta di partire. Non è
negoziabile: l'emulator 36.x **ignora** sia `disk.dataPartition.size` in
`config.ini` (che riscrive dal profilo hardware all'avvio) sia il flag
`-partition-size`. Dai boot successivi riusa l'immagine già creata e lo spazio
richiesto crolla.

storekit verifica lo spazio solo quando l'immagine non esiste ancora, e in quel
caso si ferma subito con un messaggio chiaro invece di lasciare
`adb wait-for-device` appeso per cinque minuti. Se lo vedi:

```bash
bun run storekit clean   # svuota out/ (build cache iOS: qualche GB)
du -sh ~/.android/avd/*  # gli AVD vecchi sono il secondo candidato
```

---

## Temi

| id | Direzione |
|---|---|
| `aurora` *(default)* | notte brand, blu → ciano, testo chiaro, device con crop cinematografico |
| `parchment` | chiaro editoriale, carta calda, inchiostro, griglia appena percettibile |
| `panorama` | banda continua inclinata attraverso tutto il set, evoluzione del look attualmente online |

I temi vivono in [`src/render/theme.ts`](src/render/theme.ts). Le cornici device
sono disegnate in CSS ([`src/render/frame.ts`](src/render/frame.ts)): scalano a
qualsiasi risoluzione e non portano in repo render di dispositivi altrui.

Font e marchio vengono letti direttamente dalle sorgenti dell'app
(`gotham_bold.otf`, `foreground-mark.svg`): l'artwork non può divergere dal
brand.

---

## Misure prodotte

**App Store** (`out/fastlane/ios/screenshots/<locale>/`)

| Device | Misura | Display target |
|---|---|---|
| iPhone 6.9" | 1320 × 2868 | `APP_IPHONE_69` |
| iPad 13" | 2048 × 2732 | `APP_IPAD_PRO_3GEN_129` |

**Google Play** (`out/fastlane/android/metadata/android/<locale>/images/`)

| Asset | Misura |
|---|---|
| `phoneScreenshots/` | 1080 × 1920 |
| `tenInchScreenshots/` | 1600 × 2560 *(device `tablet-10`, disattivato di default)* |
| `featureGraphic.png` | 1024 × 500 |
| `icon.png` | 512 × 512 |

---

## Upload

Le lane in [`fastlane/`](fastlane/) caricano **solo** artwork e metadata: il
binario continua a passare da `.github/workflows/release.yml`, che genera un
proprio `Fastfile` temporaneo e non tocca questa cartella.

```bash
cd tools/storekit
fastlane ios upload_screenshots dry_run:true    # prova a vuoto
fastlane ios upload_screenshots
fastlane android upload_screenshots track:production
```

---

## Modificare il set

Tutto sta in [`storekit.config.ts`](storekit.config.ts): scene, testi per
lingua, device, misure, descrizioni store. Aggiungere una lingua significa
aggiungere il tag in `locales`, la cartella store in `storeLocaleFolders` e i
testi in `storeText`. La validazione all'avvio dice esattamente cosa manca.
