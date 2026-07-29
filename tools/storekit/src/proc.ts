import { spawn } from "node:child_process";
import { openSync } from "node:fs";
import { log } from "./log.ts";

export interface RunResult {
  code: number;
  stdout: string;
  stderr: string;
}

export interface RunOptions {
  cwd?: string;
  env?: Record<string, string | undefined>;
  /** Stream child output to this process instead of buffering it. */
  inherit?: boolean;
  /** Kill the child after this many ms and reject. */
  timeoutMs?: number;
  /** Bytes written to the child's stdin. */
  input?: string;
}

/** Runs a command to completion and returns its exit code and output. */
export function run(
  cmd: string,
  args: string[],
  opts: RunOptions = {},
): Promise<RunResult> {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, {
      cwd: opts.cwd,
      env: { ...process.env, ...opts.env } as NodeJS.ProcessEnv,
      stdio: opts.inherit
        ? ["ignore", "inherit", "inherit"]
        : [opts.input === undefined ? "ignore" : "pipe", "pipe", "pipe"],
    });

    let stdout = "";
    let stderr = "";
    child.stdout?.on("data", (d) => (stdout += d.toString()));
    child.stderr?.on("data", (d) => (stderr += d.toString()));

    if (opts.input !== undefined) {
      child.stdin?.write(opts.input);
      child.stdin?.end();
    }

    let timer: ReturnType<typeof setTimeout> | undefined;
    if (opts.timeoutMs) {
      timer = setTimeout(() => {
        child.kill("SIGKILL");
        reject(new Error(`${cmd} ${args.join(" ")} timed out after ${opts.timeoutMs}ms`));
      }, opts.timeoutMs);
    }

    child.on("error", (err) => {
      if (timer) clearTimeout(timer);
      reject(err);
    });
    child.on("close", (code) => {
      if (timer) clearTimeout(timer);
      resolve({ code: code ?? -1, stdout, stderr });
    });
  });
}

/** Like {@link run} but throws when the command exits non-zero. */
export async function runOrThrow(
  cmd: string,
  args: string[],
  opts: RunOptions = {},
): Promise<RunResult> {
  const res = await run(cmd, args, opts);
  if (res.code !== 0) {
    const detail = (res.stderr.trim() || res.stdout.trim() || "(no output)").slice(-4000);
    throw new Error(`\`${cmd} ${args.join(" ")}\` exited ${res.code}\n${detail}`);
  }
  return res;
}

/** Runs a command and returns raw stdout bytes (used for `screencap -p`). */
export function runBinary(
  cmd: string,
  args: string[],
  opts: RunOptions = {},
): Promise<{ code: number; stdout: Buffer; stderr: string }> {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, {
      cwd: opts.cwd,
      env: { ...process.env, ...opts.env } as NodeJS.ProcessEnv,
    });
    const chunks: Buffer[] = [];
    let stderr = "";
    child.stdout.on("data", (d: Buffer) => chunks.push(d));
    child.stderr.on("data", (d) => (stderr += d.toString()));
    child.on("error", reject);
    child.on("close", (code) =>
      resolve({ code: code ?? -1, stdout: Buffer.concat(chunks), stderr }),
    );
  });
}

/**
 * Starts a long-lived process (the emulator) and leaves it running.
 * `logFile` captures its output so a startup failure can be reported instead
 * of showing up as an unexplained timeout further down the line.
 */
export function spawnBackground(
  cmd: string,
  args: string[],
  opts: RunOptions & { logFile?: string } = {},
): { pid: number | undefined; kill: () => void; alive: () => boolean } {
  const out = opts.logFile ? openSync(opts.logFile, "w") : "ignore";
  const child = spawn(cmd, args, {
    cwd: opts.cwd,
    env: { ...process.env, ...opts.env } as NodeJS.ProcessEnv,
    detached: true,
    stdio: ["ignore", out, out],
  });
  child.unref();

  let exited = false;
  child.on("exit", () => (exited = true));

  return {
    pid: child.pid,
    kill: () => child.kill("SIGTERM"),
    alive: () => !exited,
  };
}

export const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * Polls `check` until it resolves truthy or `timeoutMs` elapses.
 * Throws with `label` in the message on timeout.
 */
export async function waitFor(
  label: string,
  check: () => Promise<boolean>,
  { timeoutMs = 120_000, intervalMs = 1_000 } = {},
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let announced = false;
  while (Date.now() < deadline) {
    if (await check()) return;
    if (!announced && Date.now() - (deadline - timeoutMs) > 5_000) {
      log.info(`waiting for ${label}…`);
      announced = true;
    }
    await sleep(intervalMs);
  }
  throw new Error(`Timed out after ${timeoutMs}ms waiting for ${label}`);
}
