import type { StorekitConfig } from "./src/types.ts";

/**
 * Unica fonte di verità della pipeline store.
 *
 * Cambiare una scena, una frase o una risoluzione qui e rilanciare
 * `bun run all` rigenera l'intero set per entrambi gli store.
 */
const config: StorekitConfig = {
  app: {
    name: "Mensa Italia",
    bundleId: "it.mensa.app",
    applicationId: "it.mensa.app",
    appStoreId: "1524200080",
    teamId: "6WA5D3RJBU",
    iosScheme: "iosApp",
    androidAssembleTask: ":androidApp:assembleDebug",
    androidApkPath: "androidApp/build/outputs/apk/debug/androidApp-debug.apk",
  },

  theme: "aurora",

  locales: ["it", "en"],
  defaultLocale: "it",

  storeLocaleFolders: {
    // Cartelle attese da `deliver` (App Store Connect).
    ios: { it: "it", en: "en-US" },
    // Cartelle attese da `supply` (Google Play).
    android: { it: "it-IT", en: "en-US" },
  },

  appearance: "light",

  statusBar: {
    time: "09:41",
    batteryPercent: 100,
    cellularBars: 4,
    wifiBars: 3,
  },

  devices: {
    ios: [
      {
        id: "iphone-6-9",
        label: 'iPhone 6.9"',
        deviceType: "iPhone 17 Pro Max",
        runtime: "iOS 26.4",
        simulatorName: "storekit-iPhone-6.9",
        store: { width: 1320, height: 2868 },
        deliverTarget: "APP_IPHONE_69",
        frame: { style: "iphone", bezel: 0.021, radius: 0.115, camera: "island" },
        enabled: true,
      },
      {
        id: "ipad-13",
        label: 'iPad 13"',
        deviceType: "iPad Pro 13-inch (M4)",
        runtime: "iOS 26.4",
        simulatorName: "storekit-iPad-13",
        // 2048×2732 è la misura iPad Pro accettata da ASC e riconosciuta da
        // `deliver`; il simulatore M4 cattura a 2064×2752 e viene riscalato.
        store: { width: 2048, height: 2732 },
        deliverTarget: "APP_IPAD_PRO_3GEN_129",
        frame: { style: "ipad", bezel: 0.026, radius: 0.045, camera: "none" },
        enabled: true,
      },
    ],
    android: [
      {
        id: "phone",
        label: "Pixel 7",
        avd: "storekit_pixel7",
        avdDevice: "pixel_7",
        systemImage: "system-images;android-35;google_apis;arm64-v8a",
        store: { width: 1080, height: 1920 },
        supplyFolder: "phoneScreenshots",
        frame: { style: "pixel", bezel: 0.019, radius: 0.075, camera: "punch-hole" },
        enabled: true,
      },
      {
        id: "tablet-10",
        label: "Pixel Tablet",
        avd: "storekit_pixel_tablet",
        avdDevice: "pixel_tablet",
        systemImage: "system-images;android-35;google_apis;arm64-v8a",
        store: { width: 1600, height: 2560 },
        supplyFolder: "tenInchScreenshots",
        frame: { style: "pixel-tablet", bezel: 0.024, radius: 0.04, camera: "none" },
        // Richiede un secondo AVD: attivalo con `--devices tablet-10` o
        // mettendo `enabled: true` (storekit lo crea da solo).
        enabled: false,
      },
    ],
  },

  scenes: [
    {
      id: "today",
      ios: { tab: "today" },
      android: { screen: "today" },
      copy: {
        it: {
          kicker: "La tua giornata",
          headline: ["Tutto il Mensa,", "appena apri l'app"],
        },
        en: {
          kicker: "Your day",
          headline: ["All of Mensa,", "when you open the app"],
        },
      },
    },
    {
      id: "card",
      ios: { tab: "card" },
      android: { screen: "card" },
      copy: {
        it: {
          kicker: "Tessera digitale",
          headline: ["La tessera socio", "sempre con te"],
        },
        en: {
          kicker: "Digital card",
          headline: ["Your membership card,", "always on hand"],
        },
      },
    },
    {
      id: "events",
      ios: { launchScreen: "events" },
      android: { screen: "events" },
      copy: {
        it: {
          kicker: "Eventi e SIG",
          headline: ["Trova il prossimo", "incontro vicino a te"],
        },
        en: {
          kicker: "Events and SIGs",
          headline: ["Find the next", "gathering near you"],
        },
      },
    },
    {
      id: "discover",
      ios: { tab: "discover" },
      android: { screen: "discover" },
      copy: {
        it: {
          kicker: "Scopri",
          headline: ["Sedi, convenzioni,", "documenti, podcast"],
        },
        en: {
          kicker: "Discover",
          headline: ["Chapters, deals,", "documents, podcasts"],
        },
      },
    },
    {
      id: "search",
      ios: { tab: "search" },
      android: { screen: "search" },
      copy: {
        it: {
          kicker: "Ricerca globale",
          headline: ["Soci, eventi e atti", "in un solo campo"],
        },
        en: {
          kicker: "Global search",
          headline: ["Members, events, papers", "in a single field"],
        },
      },
    },
    {
      id: "profile",
      ios: { tab: "profile" },
      android: { screen: "profile" },
      copy: {
        it: {
          kicker: "Il tuo profilo",
          headline: ["Notifiche, lingua", "e privacy sotto controllo"],
        },
        en: {
          kicker: "Your profile",
          headline: ["Notifications, language", "and privacy in your hands"],
        },
      },
    },
  ],

  storeText: {
    it: {
      name: "Mensa Italia",
      subtitle: "Tessera, eventi e community",
      promotionalText:
        "Tessera digitale, eventi, SIG e sedi locali: l'app ufficiale di Mensa Italia.",
      shortDescription:
        "Tessera digitale, eventi, SIG e sedi locali di Mensa Italia.",
      featureGraphicTagline: "L'app ufficiale di Mensa Italia",
      keywords:
        "mensa,mensa italia,soci,tessera,eventi,sig,quiz,qi,associazione,community",
      description: `L'app ufficiale di Mensa Italia: la tua tessera associativa, gli eventi e tutta la vita dell'associazione in un unico posto.

PER I SOCI
• Tessera associativa digitale, pronta anche offline
• Eventi nazionali e locali, con iscrizione e promemoria in calendario
• SIG: i gruppi di interesse speciale, con le loro attività
• Sedi locali: chi c'è vicino a te e cosa organizza
• Rubrica e registro soci
• Documenti ufficiali dell'associazione
• Quid: articoli e podcast della rivista
• Convenzioni e vantaggi riservati ai soci
• Notifiche su ciò che ti interessa davvero

PER CHI NON È ANCORA SOCIO
• Calendario eventi aperti al pubblico
• Le sedi locali e i loro contatti
• Come funziona il test di ammissione

Mensa è l'associazione internazionale che riunisce persone con un quoziente intellettivo nel 2% più alto della popolazione. Mensa Italia ne è la sezione nazionale.`,
      releaseNotes:
        "Nuova app nativa: più veloce, più leggera e completamente ridisegnata su iOS e Android.",
    },
    en: {
      name: "Mensa Italia",
      subtitle: "Card, events and community",
      promotionalText:
        "Digital membership card, events, SIGs and local chapters: the official Mensa Italia app.",
      shortDescription:
        "Digital membership card, events, SIGs and local chapters of Mensa Italia.",
      featureGraphicTagline: "The official Mensa Italia app",
      keywords:
        "mensa,mensa italia,members,card,events,sig,iq,high iq,society,community",
      description: `The official Mensa Italia app: your membership card, the events and the whole life of the society in one place.

FOR MEMBERS
• Digital membership card, ready even offline
• National and local events, with sign-up and calendar reminders
• SIGs: the special interest groups and their activities
• Local chapters: who is near you and what they organise
• Member directory and register
• Official society documents
• Quid: articles and podcasts from the magazine
• Deals and benefits reserved to members
• Notifications about what actually matters to you

IF YOU ARE NOT A MEMBER YET
• Calendar of events open to the public
• Local chapters and how to reach them
• How the admission test works

Mensa is the international society that brings together people with an IQ in the top 2% of the population. Mensa Italia is its Italian national chapter.`,
      releaseNotes:
        "Brand new native app: faster, lighter and fully redesigned on iOS and Android.",
    },
  },
};

export default config;
