/**
 * Reusable QR code renderer via qrcode-generator.
 * Used by both TicketDetailApp and TesseraApp.
 *
 * qrcode-generator is a CommonJS module. It's imported via a dynamic `require`
 * shimmed by Vite's bundler at runtime, so we reference it through a global
 * declaration rather than a typed import to avoid needing @types/node.
 */

declare function require(id: string): (typeNumber: number, ecl: string) => {
  addData(data: string): void;
  make(): void;
  createSvgTag(opts: { scalable: boolean }): string;
};

function getQrcode() {
  // Vite bundles CJS modules transparently; access via globalThis.require shim
  // or fall back to the bundled version exposed on the window after Vite processes it.
  try {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return (globalThis as any).__qrcode_generator__ ?? require("qrcode-generator");
  } catch {
    return null;
  }
}

export function qrSvg(payload: string): string {
  const qrcode = getQrcode();
  if (!qrcode) return "";
  const qr = qrcode(0, "M");
  qr.addData(payload);
  qr.make();
  return qr.createSvgTag({ scalable: true });
}

interface QrCodeProps {
  payload: string;
  size?: number;
  label?: string;
}

export function QrCode({ payload, size = 240, label }: QrCodeProps) {
  if (!payload) {
    return (
      <div
        className="qr-unavailable"
        role="img"
        aria-label="QR non disponibile"
        style={{ width: size, height: size }}
      >
        <span className="qr-unavailable__icon" aria-hidden="true">—</span>
        <span className="qr-unavailable__label">QR non disponibile</span>
        <span className="qr-unavailable__hint">
          Il codice QR non è ancora stato generato per questo biglietto.
        </span>

        <style>{`
          .qr-unavailable {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            border: 1px solid var(--color-border-subtle);
            border-radius: var(--radius-md);
            background: var(--color-surface-sunken);
            padding: var(--spacing-4);
            text-align: center;
            box-sizing: border-box;
          }
          .qr-unavailable__icon {
            font-size: 2rem;
            color: var(--color-text-tertiary);
          }
          .qr-unavailable__label {
            font-size: var(--text-sm);
            font-weight: 600;
            color: var(--color-text-secondary);
          }
          .qr-unavailable__hint {
            font-size: var(--text-xs);
            color: var(--color-text-tertiary);
            max-inline-size: 24ch;
            line-height: 1.5;
          }
        `}</style>
      </div>
    );
  }

  const svg = qrSvg(payload);

  return (
    <div
      className="qr-wrap"
      style={{ width: size, height: size }}
      role="img"
      aria-label={label ?? "Codice QR"}
    >
      {svg ? (
        <div
          className="qr-inner"
          dangerouslySetInnerHTML={{ __html: svg }}
        />
      ) : (
        <div
          className="qr-inner"
          aria-hidden="true"
          style={{
            width: "100%",
            height: "100%",
            background:
              "repeating-conic-gradient(#000 0% 25%, #fff 0% 50%) 0 / 16px 16px",
            opacity: 0.15,
          }}
        />
      )}
      <style>{`
        .qr-wrap {
          background: #ffffff;
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 12px;
          box-sizing: border-box;
        }
        .qr-inner {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
        }
        .qr-inner svg {
          width: 100%;
          height: 100%;
          display: block;
        }
        .qr-loading {
          font-size: var(--text-xs);
          color: var(--color-text-tertiary);
        }
      `}</style>
    </div>
  );
}
