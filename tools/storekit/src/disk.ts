import { statfs } from "node:fs/promises";

/**
 * Free-space helpers.
 *
 * Both capture backends write large files (a clean Xcode build, an emulator
 * userdata image, one PNG per scene) and both fail opaquely when the volume
 * fills up: `simctl io screenshot` exits 128, the emulator aborts mid-boot.
 * Checking up front turns those into an actionable message.
 */

/** Megabytes available to an unprivileged process at `path`. */
export async function freeMb(path: string): Promise<number | null> {
  try {
    const fs = await statfs(path);
    return (Number(fs.bavail) * Number(fs.bsize)) / (1024 * 1024);
  } catch {
    return null;
  }
}

export async function assertFreeSpace(
  path: string,
  requiredMb: number,
  context: string,
): Promise<void> {
  const free = await freeMb(path);
  if (free === null || free >= requiredMb) return;
  throw new Error(
    `Not enough free disk for ${context}: ${Math.round(free)} MB available, ` +
      `${requiredMb} MB needed.\n` +
      "`bun run storekit prune` drops the build cache but keeps the captures; " +
      "`bun run storekit clean` wipes out/ entirely.",
  );
}

/**
 * Headroom for a clean XCFramework + xcodebuild run.
 *
 * Measured on this project: ~1.2 GB of Xcode intermediates plus ~370 MB of
 * products, and the intermediates are pruned as soon as the `.app` exists.
 * Deliberately close to the real footprint. A padded threshold would reject
 * builds that fit perfectly well.
 */
export const IOS_BUILD_FREE_MB = 2_500;
/** Slack kept for the PNGs written during a capture loop. */
export const CAPTURE_FREE_MB = 600;
