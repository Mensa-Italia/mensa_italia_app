const useColor = process.env.NO_COLOR === undefined && process.stdout.isTTY !== false;

const paint = (code: string, s: string) => (useColor ? `[${code}m${s}[0m` : s);

export const c = {
  dim: (s: string) => paint("2", s),
  bold: (s: string) => paint("1", s),
  red: (s: string) => paint("31", s),
  green: (s: string) => paint("32", s),
  yellow: (s: string) => paint("33", s),
  blue: (s: string) => paint("34", s),
  cyan: (s: string) => paint("36", s),
};

let indent = 0;
const pad = () => "  ".repeat(indent);

export const log = {
  step(msg: string) {
    console.log(`${pad()}${c.cyan("▸")} ${msg}`);
  },
  info(msg: string) {
    console.log(`${pad()}${c.dim("·")} ${c.dim(msg)}`);
  },
  ok(msg: string) {
    console.log(`${pad()}${c.green("✔")} ${msg}`);
  },
  warn(msg: string) {
    console.log(`${pad()}${c.yellow("!")} ${msg}`);
  },
  error(msg: string) {
    console.error(`${pad()}${c.red("✖")} ${msg}`);
  },
  title(msg: string) {
    console.log(`\n${pad()}${c.bold(msg)}`);
  },
  async group<T>(msg: string, fn: () => Promise<T>): Promise<T> {
    log.step(msg);
    indent += 1;
    try {
      return await fn();
    } finally {
      indent -= 1;
    }
  },
};

/** Human-readable duration, e.g. `1m 04s` / `820ms`. */
export function humanMs(ms: number): string {
  if (ms < 1000) return `${Math.round(ms)}ms`;
  const s = ms / 1000;
  if (s < 60) return `${s.toFixed(1)}s`;
  const m = Math.floor(s / 60);
  return `${m}m ${String(Math.round(s - m * 60)).padStart(2, "0")}s`;
}
