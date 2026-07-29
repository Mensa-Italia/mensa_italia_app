/**
 * Shape of `storekit.config.ts`. Everything the pipeline needs (which
 * devices to drive, which screens to visit, what to write on the finished
 * artwork) lives in that one file.
 */

export type Platform = "ios" | "android";
export type Appearance = "light" | "dark";

/** Pixel dimensions of a finished store asset. */
export interface Size {
  width: number;
  height: number;
}

/** Procedural bezel drawn around a raw capture. */
export interface FrameSpec {
  /** Bezel family. Drives corner radius, camera cutout and button layout. */
  style: "iphone" | "ipad" | "pixel" | "pixel-tablet";
  /** Bezel thickness as a fraction of the framed device width. */
  bezel: number;
  /** Screen corner radius as a fraction of the framed device width. */
  radius: number;
  /** Draw a Dynamic Island pill (iPhone) or a punch-hole camera (Pixel). */
  camera: "island" | "punch-hole" | "none";
}

export interface IosDevice {
  /** Stable id used in output paths and on the CLI (`--devices`). */
  id: string;
  /** Human label used in logs. */
  label: string;
  /** `simctl` device type name, e.g. "iPhone 17 Pro Max". */
  deviceType: string;
  /** `simctl` runtime name, e.g. "iOS 26.4". Falls back to the newest iOS runtime. */
  runtime?: string;
  /** Name given to the simulator storekit creates. */
  simulatorName: string;
  /** Final store asset size. Raw captures are re-scaled into this canvas. */
  store: Size;
  /**
   * App Store Connect display-target folder name used by `deliver`, e.g.
   * `APP_IPHONE_6_9` / `APP_IPAD_PRO_3GEN_129`. See
   * https://docs.fastlane.tools/actions/deliver/#screenshot-sizes
   */
  deliverTarget: string;
  frame: FrameSpec;
  enabled: boolean;
}

export interface AndroidDevice {
  id: string;
  label: string;
  /** AVD name. Created on demand when missing. */
  avd: string;
  /**
   * `avdmanager` device profile (`avdmanager list device`) used when the AVD
   * has to be created, e.g. "pixel_7" / "pixel_tablet".
   */
  avdDevice: string;
  /** System image package id, e.g. "system-images;android-35;google_apis;arm64-v8a". */
  systemImage: string;
  store: Size;
  /** `supply` image-type folder: phoneScreenshots / sevenInchScreenshots / tenInchScreenshots. */
  supplyFolder: string;
  frame: FrameSpec;
  enabled: boolean;
}

/** Per-locale marketing copy stamped onto a screenshot. */
export interface SceneCopy {
  /** Small uppercase eyebrow above the headline. Optional. */
  kicker?: string;
  /** Headline, one array entry per rendered line. */
  headline: string[];
}

export interface Scene {
  /** Stable id: becomes the raw/output filename and the ordering key. */
  id: string;
  /**
   * iOS destination. `tab` uses the `--initial-tab` launch argument (goes
   * through the real RootView, so the tab bar is visible); `launchScreen`
   * uses `MENSA_LAUNCH_SCREEN` to mount a feature view directly.
   */
  ios?: { tab?: string; launchScreen?: string };
  /** Android destination: a `mensa_screen` alias resolved in MainAppShell. */
  android?: { screen: string };
  /** Extra settle time in ms on top of frame-stability detection. */
  settleMs?: number;
  /** Copy keyed by locale. Missing locales fall back to `defaultLocale`. */
  copy: Record<string, SceneCopy>;
}

export interface StoreTextEntry {
  name: string;
  subtitle?: string;
  promotionalText?: string;
  description: string;
  keywords?: string;
  releaseNotes?: string;
  /** Google Play caps the short description at 80 characters. */
  shortDescription?: string;
  /** Tagline printed on the Play feature graphic. */
  featureGraphicTagline?: string;
}

export interface StorekitConfig {
  app: {
    name: string;
    /** iOS bundle identifier. */
    bundleId: string;
    /** Android applicationId. */
    applicationId: string;
    /** Numeric App Store id, used by the fastlane lanes. */
    appStoreId?: string;
    /** Apple team / ASC account email, used by the fastlane Appfile. */
    appleId?: string;
    teamId?: string;
    /** Xcode scheme built for the simulator. */
    iosScheme: string;
    /** Gradle task producing the debuggable APK. */
    androidAssembleTask: string;
    /** Path of the APK relative to the repo root, after the assemble task. */
    androidApkPath: string;
  };
  /** Default visual theme id. Overridable with `--theme`. */
  theme: string;
  /** Locales to produce, as BCP-47 language tags. */
  locales: string[];
  /** Fallback locale for copy and for `deliver`'s default listing. */
  defaultLocale: string;
  /** Map from a storekit locale to the folder name `deliver`/`supply` expects. */
  storeLocaleFolders: {
    ios: Record<string, string>;
    android: Record<string, string>;
  };
  /** Light or dark UI when capturing. */
  appearance: Appearance;
  /** Frozen status-bar values so every capture reads identically. */
  statusBar: {
    time: string;
    batteryPercent: number;
    cellularBars: number;
    wifiBars: number;
  };
  devices: {
    ios: IosDevice[];
    android: AndroidDevice[];
  };
  scenes: Scene[];
  /** Store listing text emitted into the fastlane metadata tree. */
  storeText: Record<string, StoreTextEntry>;
}
