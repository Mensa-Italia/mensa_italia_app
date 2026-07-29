import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { REPO_ROOT, TOOL_ROOT } from "./paths.ts";

/**
 * Loads `tools/storekit/.env` (gitignored) into `process.env` without
 * clobbering variables the caller already exported. Keeps store credentials
 * out of the repo while letting CI pass them as real environment variables.
 */
export function loadDotEnv(): void {
  const file = join(TOOL_ROOT, ".env");
  if (!existsSync(file)) return;
  for (const rawLine of readFileSync(file, "utf8").split("\n")) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eq = line.indexOf("=");
    if (eq === -1) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
}

export interface Credentials {
  email: string;
  password: string;
}

/**
 * Demo-account credentials used to sign the app in before capturing.
 * Returns null when unset. Capture then falls back to the anonymous
 * (public-area) state, which is still valid but far less interesting.
 */
export function credentials(): Credentials | null {
  const email = process.env.MENSA_STOREKIT_EMAIL;
  const password = process.env.MENSA_STOREKIT_PASSWORD;
  if (!email || !password) return null;
  return { email, password };
}

const ANDROID_SDK_CANDIDATES = [
  process.env.ANDROID_HOME,
  process.env.ANDROID_SDK_ROOT,
  join(process.env.HOME ?? "", "Library/Android/sdk"),
  "/opt/homebrew/share/android-commandlinetools",
  "/usr/local/share/android-commandlinetools",
  "/usr/local/lib/android/sdk",
];

/** Resolves the Android SDK root, checking the usual macOS/CI locations. */
export function androidSdkRoot(): string {
  for (const candidate of ANDROID_SDK_CANDIDATES) {
    if (candidate && existsSync(join(candidate, "platform-tools", "adb"))) {
      return candidate;
    }
  }
  throw new Error(
    "Android SDK not found. Set ANDROID_HOME, or install the command line tools " +
      "(`brew install --cask android-commandlinetools`).",
  );
}

export interface AndroidTools {
  sdk: string;
  adb: string;
  emulator: string;
  avdmanager: string;
  sdkmanager: string;
  /** Environment every Android tool call must inherit. */
  env: Record<string, string>;
}

export function androidTools(): AndroidTools {
  const sdk = androidSdkRoot();
  const cmdline = join(sdk, "cmdline-tools", "latest", "bin");
  return {
    sdk,
    adb: join(sdk, "platform-tools", "adb"),
    emulator: join(sdk, "emulator", "emulator"),
    avdmanager: join(cmdline, "avdmanager"),
    sdkmanager: join(cmdline, "sdkmanager"),
    env: { ANDROID_HOME: sdk, ANDROID_SDK_ROOT: sdk },
  };
}

const CHROME_CANDIDATES = [
  process.env.STOREKIT_CHROME,
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  "/Applications/Chromium.app/Contents/MacOS/Chromium",
  "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
  "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
  "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
  "/usr/bin/google-chrome",
  "/usr/bin/google-chrome-stable",
  "/usr/bin/chromium",
  "/usr/bin/chromium-browser",
  "/snap/bin/chromium",
];

/** Locates a Chromium binary for the framing renderer. */
export function chromeExecutable(): string {
  for (const candidate of CHROME_CANDIDATES) {
    if (candidate && existsSync(candidate)) return candidate;
  }
  throw new Error(
    "No Chrome/Chromium binary found. Install Google Chrome or point " +
      "STOREKIT_CHROME at an existing Chromium executable.",
  );
}

/** Path to the repo-root gradlew wrapper. */
export const GRADLEW = join(REPO_ROOT, "gradlew");

/**
 * Gradle 8.10 refuses to start on a JVM newer than Java 23, and this repo
 * compiles against Java 17. Homebrew's `openjdk@17` / `temurin` kegs are
 * usually installed but not linked onto PATH, so resolve one explicitly
 * instead of failing with Gradle's opaque `* What went wrong: <version>`.
 */
export function javaHome(): string | undefined {
  if (process.env.JAVA_HOME) return process.env.JAVA_HOME;

  const roots = [
    "/opt/homebrew/opt/openjdk@17",
    "/opt/homebrew/opt/openjdk@21",
    "/usr/local/opt/openjdk@17",
    "/usr/local/opt/openjdk@21",
    "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home",
    "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home",
  ];
  for (const root of roots) {
    for (const candidate of [join(root, "libexec/openjdk.jdk/Contents/Home"), root]) {
      if (existsSync(join(candidate, "bin", "java"))) return candidate;
    }
  }
  return undefined;
}

/** Environment every Gradle invocation needs: Android SDK + a supported JDK. */
export function gradleEnv(): Record<string, string> {
  const env: Record<string, string> = {};
  try {
    Object.assign(env, androidTools().env);
  } catch {
    // Reported by the caller; the iOS-only XCFramework task may still work.
  }
  const home = javaHome();
  if (home) {
    env.JAVA_HOME = home;
    env.PATH = `${join(home, "bin")}:${process.env.PATH ?? ""}`;
  }
  return env;
}
