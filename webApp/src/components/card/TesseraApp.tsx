import { useMemo, useRef, useState } from "react";
import { Share2, Copy, Check, Download, AlertTriangle } from "lucide-react";
import { MensaProvider, useMensa } from "../../lib/MensaProvider";
import { QrCode, qrSvg } from "./QrCode";

const LS_USER_KEY = "mensa.auth.user";
const API_BASE = "https://svc.mensa.it";
const RENEW_URL = "https://cloud32.mensa.it/rinnovo";
const SHARE_URL = "https://www.mensa.it";
const YEAR_MS = 365 * 86_400_000;
const BRAND_GRADIENT =
  "linear-gradient(135deg, oklch(38% 0.16 263), oklch(78% 0.13 222))";

function formatItalianDateLong(epochMs: number): string {
  return new Date(epochMs).toLocaleDateString("it-IT", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

function daysUntil(epochMs: number): number {
  return Math.ceil((epochMs - Date.now()) / 86_400_000);
}

type TesseraStatus = "active" | "expiring-soon" | "expiring" | "expired";

function tesseraStatus(expireMs: number): TesseraStatus {
  const days = daysUntil(expireMs);
  if (days < 0) return "expired";
  if (days < 14) return "expiring-soon";
  if (days < 60) return "expiring";
  return "active";
}

function avatarUrlFor(userId: string, avatarFile: string): string {
  if (!avatarFile) return "";
  return `${API_BASE}/api/files/_pb_users_auth_/${userId}/${avatarFile}`;
}

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "?";
  const last = parts.length > 1 ? parts[parts.length - 1]![0] : "";
  return (first + (last ?? "")).toUpperCase();
}

interface CardAvatarProps {
  userId: string;
  avatarFile: string;
  name: string;
  size?: number;
}

function CardAvatar({ userId, avatarFile, name, size = 48 }: CardAvatarProps) {
  const url = avatarUrlFor(userId, avatarFile);
  const [failed, setFailed] = useState(false);
  const showImg = !!url && !failed;

  if (showImg) {
    return (
      <img
        src={url}
        alt=""
        crossOrigin="anonymous"
        onError={() => setFailed(true)}
        style={{
          width: size,
          height: size,
          borderRadius: "50%",
          objectFit: "cover",
          flexShrink: 0,
          border: "2px solid rgba(255,255,255,0.7)",
          background: "rgba(255,255,255,0.1)",
        }}
      />
    );
  }
  return (
    <span
      aria-hidden="true"
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        width: size,
        height: size,
        borderRadius: "50%",
        background: BRAND_GRADIENT,
        color: "white",
        fontWeight: 700,
        fontSize: Math.round(size * 0.36),
        letterSpacing: "-0.01em",
        flexShrink: 0,
        userSelect: "none",
        fontFamily: "var(--font-display)",
        border: "2px solid rgba(255,255,255,0.7)",
      }}
    >
      {initialsOf(name)}
    </span>
  );
}

function Inner() {
  const { ready, authState, user } = useMensa();
  const [copied, setCopied] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  if (typeof window !== "undefined" && ready && authState === "Anonymous" && !window.localStorage.getItem(LS_USER_KEY)) {
    window.location.replace("/login");
    return null;
  }

  if (!user) {
    return <p className="ta__pending" aria-live="polite">Caricamento…</p>;
  }

  const status = tesseraStatus(user.expireMembershipMs);
  const days = daysUntil(user.expireMembershipMs);
  const expiryStr = formatItalianDateLong(user.expireMembershipMs);
  const qrPayload = `MENSA-IT|id:${user.id}|user:${user.username}|exp:${user.expireMembershipMs}`;

  const showRenewBanner = status === "expired" || (days >= 0 && days <= 30);

  // Progress bar — days left out of a 365-day membership year.
  const pctLeft = useMemo(() => {
    if (days < 0) return 0;
    const clamped = Math.min(YEAR_MS, Math.max(0, days * 86_400_000));
    return Math.round((clamped / YEAR_MS) * 100);
  }, [days]);

  const barColor =
    status === "expired" ? "var(--color-status-error)" :
    status === "expiring-soon" ? "var(--color-status-error)" :
    status === "expiring" ? "var(--color-status-warning)" :
    "var(--color-status-success)";

  async function handleShare() {
    if (navigator.share) {
      try {
        await navigator.share({ title: "La mia tessera Mensa Italia", url: SHARE_URL });
      } catch {
        /* user cancelled */
      }
    } else {
      await handleCopy();
    }
  }

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(SHARE_URL);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch { /* clipboard unavailable */ }
  }

  // Card → PNG via inline SVG render (no external deps).
  async function handleDownload() {
    if (!user) return;
    setDownloading(true);
    try {
      const W = 1080;
      const H = 680; // 1.586 aspect
      const qr = qrSvg(qrPayload);
      // Strip outer <svg> wrapper if present to safely embed inside our SVG.
      const qrInner = qr.replace(/<\?xml[^?]*\?>/g, "");

      // Try to inline the avatar as data-url so the canvas isn't tainted.
      let avatarHref = "";
      const rawUrl = avatarUrlFor(user.id, user.avatar);
      if (rawUrl) {
        try {
          const res = await fetch(rawUrl, { mode: "cors" });
          if (res.ok) {
            const blob = await res.blob();
            avatarHref = await new Promise<string>((resolve, reject) => {
              const fr = new FileReader();
              fr.onload = () => resolve(String(fr.result));
              fr.onerror = reject;
              fr.readAsDataURL(blob);
            });
          }
        } catch { /* fall through to initials */ }
      }

      const initials = initialsOf(user.name);
      const avatarCircle = avatarHref
        ? `<defs><clipPath id="avClip"><circle cx="${W - 90}" cy="90" r="42"/></clipPath></defs>
           <image href="${avatarHref}" x="${W - 132}" y="48" width="84" height="84" clip-path="url(#avClip)" preserveAspectRatio="xMidYMid slice"/>
           <circle cx="${W - 90}" cy="90" r="42" fill="none" stroke="rgba(255,255,255,0.7)" stroke-width="3"/>`
        : `<circle cx="${W - 90}" cy="90" r="42" fill="url(#avGrad)" stroke="rgba(255,255,255,0.7)" stroke-width="3"/>
           <text x="${W - 90}" y="${90 + 12}" font-family="-apple-system, system-ui, sans-serif" font-size="32" font-weight="700" fill="white" text-anchor="middle">${initials}</text>`;

      const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#1a2548"/>
      <stop offset="100%" stop-color="#2d3f7a"/>
    </linearGradient>
    <linearGradient id="avGrad" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#2d3f7a"/>
      <stop offset="100%" stop-color="#5fb3d4"/>
    </linearGradient>
    <pattern id="stripes" patternUnits="userSpaceOnUse" width="40" height="40" patternTransform="rotate(45)">
      <line x1="0" y1="0" x2="0" y2="40" stroke="white" stroke-width="1" stroke-opacity="0.04"/>
    </pattern>
  </defs>
  <rect width="${W}" height="${H}" rx="36" fill="url(#bg)"/>
  <rect width="${W}" height="${H}" rx="36" fill="url(#stripes)"/>
  <text x="${W - 60}" y="${H - 60}" font-family="-apple-system, system-ui, sans-serif" font-size="520" font-weight="900" fill="white" fill-opacity="0.07" text-anchor="end" letter-spacing="-30">M</text>
  <text x="60" y="92" font-family="-apple-system, system-ui, sans-serif" font-size="22" font-weight="600" fill="white" letter-spacing="6">TESSERA SOCIO</text>
  ${avatarCircle}
  <text x="60" y="${H / 2 + 18}" font-family="-apple-system, system-ui, sans-serif" font-size="56" font-weight="700" fill="white">${escapeXml(user.name)}</text>
  <text x="60" y="${H - 110}" font-family="-apple-system, system-ui, sans-serif" font-size="22" font-weight="600" fill="white" fill-opacity="0.8" letter-spacing="2">SOCIO #${escapeXml(user.id)}</text>
  <text x="60" y="${H - 70}" font-family="-apple-system, system-ui, sans-serif" font-size="20" fill="white" fill-opacity="0.75">Valida fino al ${escapeXml(expiryStr)}</text>
  <text x="60" y="${H - 32}" font-family="-apple-system, system-ui, sans-serif" font-size="18" font-weight="700" fill="white" fill-opacity="0.85" letter-spacing="4">MENSA ITALIA</text>
  <g transform="translate(${W - 220}, ${H - 220})">
    <rect width="180" height="180" rx="16" fill="white"/>
    <g transform="translate(12, 12) scale(0.84)">${qrInner}</g>
  </g>
</svg>`;

      const blob = new Blob([svg], { type: "image/svg+xml;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      try {
        const img = new Image();
        img.crossOrigin = "anonymous";
        await new Promise<void>((resolve, reject) => {
          img.onload = () => resolve();
          img.onerror = () => reject(new Error("svg load failed"));
          img.src = url;
        });
        const canvas = document.createElement("canvas");
        canvas.width = W;
        canvas.height = H;
        const ctx = canvas.getContext("2d");
        if (!ctx) throw new Error("no 2d context");
        ctx.drawImage(img, 0, 0, W, H);
        const png = canvas.toDataURL("image/png");
        const a = document.createElement("a");
        a.href = png;
        a.download = `tessera-mensa-${user.id}.png`;
        document.body.appendChild(a);
        a.click();
        a.remove();
      } finally {
        URL.revokeObjectURL(url);
      }
    } catch {
      /* swallow */
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div className="ta">
      <header className="ta__head">
        <h1 className="ta__title">Tessera digitale</h1>
        <p className="ta__subtitle">La tua tessera socio Mensa Italia.</p>
      </header>

      {showRenewBanner && (
        <div className={`ta__banner ta__banner--${status === "expired" ? "expired" : "warn"}`} role="alert">
          <span className="ta__banner-icon" aria-hidden="true">
            <AlertTriangle size={20} strokeWidth={2} />
          </span>
          <div className="ta__banner-text">
            <strong>
              {status === "expired"
                ? "Tessera scaduta"
                : days === 0
                  ? "La tua tessera scade oggi"
                  : `La tua tessera scade tra ${days} giorn${days === 1 ? "o" : "i"}`}
            </strong>
            <span>Rinnova ora per non perdere l'accesso ai servizi.</span>
          </div>
          <a href={RENEW_URL} target="_blank" rel="noopener noreferrer" className="ta__banner-cta">
            Rinnova ora
          </a>
        </div>
      )}

      <div className="ta__layout">
        <section className="ta__card-col" aria-label="Tessera socio">
          <div
            ref={cardRef}
            className="ta__card"
            role="img"
            aria-label={`Tessera Mensa Italia — ${user.name} — Socio #${user.id} — valida fino al ${expiryStr}`}
          >
            <div className="ta__card-watermark" aria-hidden="true">
              <img src="/mensa-logo.svg" alt="" />
            </div>
            <div className="ta__card-texture" aria-hidden="true" />
            <div className="ta__card-radial" aria-hidden="true" />

            <div className="ta__card-top">
              <span className="ta__card-label-top">TESSERA SOCIO</span>
              <CardAvatar userId={user.id} avatarFile={user.avatar} name={user.name} size={48} />
            </div>
            <div className="ta__card-mid">
              <p className="ta__card-name">{user.name}</p>
            </div>
            <div className="ta__card-bottom">
              <div className="ta__card-bottom-l">
                <span className="ta__card-id">SOCIO #{user.id}</span>
                <span className="ta__card-expiry">valida fino al {expiryStr}</span>
              </div>
              <span className="ta__card-wordmark" aria-hidden="true">MENSA ITALIA</span>
            </div>
          </div>

          {/* Expiry progress bar */}
          <div className="ta__bar-wrap" aria-label={`${Math.max(0, days)} giorni rimanenti`}>
            <div className="ta__bar-track">
              <div
                className="ta__bar-fill"
                style={{ width: `${pctLeft}%`, background: barColor }}
              />
            </div>
            <div className="ta__bar-meta">
              <span>
                {days < 0
                  ? `Scaduta da ${Math.abs(days)} giorni`
                  : `${days} giorni rimanenti`}
              </span>
              <span>Scadenza: {expiryStr}</span>
            </div>
          </div>
        </section>

        <aside className="ta__panel">
          <section className="ta__section">
            <h2 className="ta__section-head">QR per gli eventi</h2>
            <p className="ta__qr-hint">
              Mostra questo codice al personale degli eventi per verificare la tua iscrizione.
            </p>
            <div className="ta__qr-center">
              <QrCode payload={qrPayload} size={180} label="QR di accesso eventi Mensa Italia" />
            </div>
          </section>

          <section className="ta__section">
            <h2 className="ta__section-head">Azioni</h2>
            <div className="ta__share-row">
              <button type="button" className="ta__btn-secondary" onClick={handleShare} aria-label="Condividi tessera">
                <Share2 size={16} strokeWidth={1.75} aria-hidden="true" />
                Condividi
              </button>
              <button type="button" className="ta__btn-secondary" onClick={handleCopy} aria-label={copied ? "Link copiato" : "Copia link"}>
                {copied ? <Check size={16} strokeWidth={1.75} aria-hidden="true" /> : <Copy size={16} strokeWidth={1.75} aria-hidden="true" />}
                {copied ? "Copiato!" : "Copia link"}
              </button>
              <button type="button" className="ta__btn-secondary" onClick={handleDownload} disabled={downloading} aria-label="Scarica tessera come immagine">
                <Download size={16} strokeWidth={1.75} aria-hidden="true" />
                {downloading ? "Generazione…" : "Scarica tessera"}
              </button>
            </div>
          </section>
        </aside>
      </div>

      <style>{`
        @keyframes ta-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .ta { animation: ta-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .ta { display: grid; gap: var(--spacing-6); max-inline-size: 1080px; }
        .ta__head { padding-block-end: var(--spacing-5); border-block-end: 1px solid var(--color-border-subtle); }
        .ta__title {
          margin: 0 0 var(--spacing-2);
          font-family: var(--font-display);
          font-size: var(--text-2xl); font-weight: 700; letter-spacing: -0.02em;
          color: var(--color-text-primary); text-wrap: balance;
        }
        .ta__subtitle { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }

        /* ── Renewal banner ───────────────────────────────────── */
        .ta__banner {
          display: grid;
          grid-template-columns: auto 1fr auto;
          gap: var(--spacing-4);
          align-items: center;
          padding: var(--spacing-4) var(--spacing-5);
          border-radius: var(--radius-md);
          border: 1px solid;
        }
        .ta__banner--warn {
          background: color-mix(in oklch, var(--color-status-warning) 10%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-warning) 40%, transparent);
          color: color-mix(in oklch, var(--color-status-warning) 75%, black);
        }
        .ta__banner--expired {
          background: color-mix(in oklch, var(--color-status-error) 10%, var(--color-surface));
          border-color: color-mix(in oklch, var(--color-status-error) 40%, transparent);
          color: color-mix(in oklch, var(--color-status-error) 70%, black);
        }
        .ta__banner-icon { display: inline-flex; }
        .ta__banner-text { display: grid; gap: 2px; font-size: var(--text-sm); color: var(--color-text-primary); }
        .ta__banner-text strong { font-weight: 700; }
        .ta__banner-text span { font-size: var(--text-xs); color: var(--color-text-secondary); }
        .ta__banner-cta {
          display: inline-flex; align-items: center; justify-content: center;
          padding: 10px var(--spacing-5);
          font-size: var(--text-sm); font-weight: 600;
          color: var(--color-text-on-brand);
          background: var(--color-mensa-blue);
          border-radius: var(--radius-sm); text-decoration: none;
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .ta__banner-cta:hover { opacity: 0.88; }
        .ta__banner-cta:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .ta__layout { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-6); align-items: start; }
        @media (max-width: 1023px) { .ta__layout { grid-template-columns: 1fr; } }

        /* ── Visual Card (ISO 1.586 aspect) ──────────────────── */
        .ta__card-col { display: flex; flex-direction: column; gap: var(--spacing-4); align-items: stretch; }
        .ta__card {
          position: relative; width: 100%; max-width: 540px;
          aspect-ratio: 1.586 / 1;
          border-radius: var(--radius-xl);
          background: linear-gradient(135deg, oklch(26% 0.13 263), oklch(38% 0.16 263));
          overflow: hidden;
          padding: 24px 28px; box-sizing: border-box;
          display: flex; flex-direction: column; justify-content: space-between;
          color: white; user-select: none;
          box-shadow: 0 10px 30px -12px oklch(20% 0.1 263 / 35%);
        }
        .ta__card-watermark {
          position: absolute; inset: 0; pointer-events: none; overflow: hidden;
        }
        .ta__card-watermark img {
          position: absolute; bottom: -20%; right: -15%;
          width: 75%; height: auto;
          opacity: 0.12; filter: brightness(0) invert(1);
        }
        .ta__card-texture {
          position: absolute; inset: 0; pointer-events: none;
          background: repeating-linear-gradient(
            45deg,
            transparent 0 14px,
            rgba(255,255,255,0.04) 14px 15px
          );
        }
        .ta__card-radial {
          position: absolute; inset: 0; pointer-events: none;
          background: radial-gradient(ellipse at 80% 20%, oklch(78% 0.13 222 / 18%) 0%, transparent 60%);
        }
        .ta__card-top { display: flex; align-items: center; justify-content: space-between; position: relative; z-index: 1; }
        .ta__card-label-top { font-size: 11px; font-weight: 700; letter-spacing: 0.18em; opacity: 0.85; text-transform: uppercase; }
        .ta__card-mid { position: relative; z-index: 1; }
        .ta__card-name {
          margin: 0; font-family: var(--font-display);
          font-size: clamp(20px, 3.6vw, 30px); font-weight: 700;
          color: white; letter-spacing: -0.01em; line-height: 1.2;
        }
        .ta__card-bottom {
          display: flex; align-items: flex-end; justify-content: space-between; gap: 12px;
          position: relative; z-index: 1;
        }
        .ta__card-bottom-l { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
        .ta__card-id { font-size: 11px; font-weight: 600; font-variant-numeric: tabular-nums; letter-spacing: 0.06em; opacity: 0.9; }
        .ta__card-expiry { font-size: 11px; opacity: 0.75; }
        .ta__card-wordmark {
          font-size: 10px; font-weight: 700; letter-spacing: 0.22em;
          opacity: 0.85; text-transform: uppercase; white-space: nowrap;
        }

        /* ── Progress bar ─────────────────────────────────────── */
        .ta__bar-wrap { display: grid; gap: 6px; max-width: 540px; width: 100%; }
        .ta__bar-track {
          width: 100%; height: 6px;
          background: var(--color-surface-sunken);
          border-radius: var(--radius-full);
          overflow: hidden;
        }
        .ta__bar-fill {
          height: 100%; border-radius: var(--radius-full);
          transition: width var(--motion-medium) var(--ease-out-quart);
        }
        .ta__bar-meta {
          display: flex; justify-content: space-between; gap: var(--spacing-3);
          font-size: var(--text-xs); color: var(--color-text-tertiary);
        }

        /* ── Right panel ──────────────────────────────────────── */
        .ta__panel { display: grid; gap: var(--spacing-4); }
        .ta__section {
          display: grid; gap: var(--spacing-3);
          padding: var(--spacing-4) var(--spacing-5);
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
        }
        .ta__section-head {
          margin: 0; font-size: var(--text-sm); font-weight: 600;
          color: var(--color-text-primary);
          padding-block-end: var(--spacing-2);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        .ta__qr-hint { margin: 0; font-size: var(--text-xs); color: var(--color-text-tertiary); line-height: 1.55; }
        .ta__qr-center { display: flex; align-items: center; justify-content: center; }
        .ta__share-row { display: flex; gap: var(--spacing-3); flex-wrap: wrap; }
        .ta__btn-secondary {
          display: inline-flex; align-items: center; gap: var(--spacing-2);
          padding: 8px var(--spacing-4);
          font-size: var(--text-sm); font-weight: 500;
          color: var(--color-text-primary);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm); cursor: pointer; background: transparent;
          transition: background var(--motion-fast) var(--ease-out-quart);
        }
        .ta__btn-secondary:hover:not(:disabled) { background: var(--color-surface-elevated); }
        .ta__btn-secondary:disabled { opacity: 0.55; cursor: progress; }
        .ta__btn-secondary:focus-visible { outline: 3px solid var(--color-ring); outline-offset: 2px; }

        .ta__pending { font-size: var(--text-sm); color: var(--color-text-tertiary); }
      `}</style>
    </div>
  );
}

function escapeXml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

export function TesseraApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
