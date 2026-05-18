/**
 * Boutique product detail island.
 */
import { useEffect, useState } from "react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebBoutiqueProduct } from "../../lib/mensa";

const LS_USER_KEY = "mensa.auth.user";

function readLsUser() {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem(LS_USER_KEY);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

function fmtPrice(cents: number): string {
  return new Intl.NumberFormat("it-IT", {
    style: "currency",
    currency: "EUR",
  }).format(cents / 100);
}

function Inner({ productId }: { productId: string }) {
  const eager = readLsUser();
  const [product, setProduct] = useState<MensaWebBoutiqueProduct | null | undefined>(undefined);
  const [activeImg, setActiveImg] = useState<string | null>(null);

  useEffect(() => {
    if (eager === null) window.location.replace("/login");
  }, []);

  useEffect(() => {
    let cancel: () => void = () => {};
    let cancelled = false;
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      const fast = await Mensa.boutique.getById(productId);
      if (!cancelled) {
        setProduct(fast ?? null);
        if (fast?.imageUrl) setActiveImg(fast.imageUrl);
      }
      cancel = Mensa.boutique.subscribeAll((all) => {
        if (cancelled) return;
        const found = all.find((p) => p.id === productId) ?? null;
        setProduct(found);
        if (found?.imageUrl && activeImg === null) setActiveImg(found.imageUrl);
      });
      Mensa.boutique.refresh().catch(() => {});
    })();
    return () => { cancelled = true; cancel(); };
  }, [productId]);

  if (product === undefined) {
    return <p className="product-detail__pending" aria-live="polite">Caricamento…</p>;
  }
  if (product === null) {
    return (
      <div className="product-detail__notfound">
        <p className="product-detail__notfound-title">Prodotto non trovato</p>
        <a href="/boutique" className="product-detail__back">← Torna alla boutique</a>
      </div>
    );
  }

  const displayImg = activeImg || product.imageUrl;
  const allImgs = [product.imageUrl, ...product.imageUrls].filter(
    (u, i, arr) => u && arr.indexOf(u) === i
  ) as string[];

  const mailtoSubject = encodeURIComponent(`Richiesta prodotto: ${product.name}`);

  return (
    <div className="product-detail">
      <a href="/boutique" className="product-detail__back">← Torna alla boutique</a>

      <div className="product-detail__layout">
        {/* LEFT */}
        <div className="product-detail__left">
          {displayImg ? (
            <img
              src={displayImg}
              alt={product.name}
              className="product-detail__hero"
              loading="lazy"
            />
          ) : (
            <div className="product-detail__hero product-detail__hero--placeholder" aria-hidden="true" />
          )}

          {allImgs.length > 1 && (
            <div className="product-detail__thumbs">
              {allImgs.map((url) => (
                <button
                  key={url}
                  type="button"
                  className={`product-detail__thumb ${activeImg === url ? "product-detail__thumb--active" : ""}`}
                  onClick={() => setActiveImg(url)}
                  aria-label={`Immagine prodotto`}
                >
                  <img src={url} alt="" loading="lazy" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT */}
        <div className="product-detail__right">
          {product.alternativeOf && (
            <span className="product-detail__alt-chip">
              Alternativa a: {product.alternativeOf}
            </span>
          )}
          <h1 className="product-detail__name">{product.name}</h1>
          <p className="product-detail__price">{fmtPrice(product.priceCents)}</p>

          {product.description && (
            <p className="product-detail__desc">{product.description}</p>
          )}

          <div className="product-detail__actions">
            {product.orderUrl ? (
              <a
                href={product.orderUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="product-btn product-btn--primary"
              >
                Ordina ora
              </a>
            ) : (
              <a
                href={`mailto:info@mensa.it?subject=${mailtoSubject}`}
                className="product-btn product-btn--secondary"
              >
                Contattaci
              </a>
            )}
          </div>
        </div>
      </div>

      <style>{`
        .product-detail { display: grid; gap: var(--spacing-5); }

        .product-detail__back {
          font-size: var(--text-xs);
          color: var(--color-mensa-blue);
          text-decoration: none;
          font-weight: 500;
        }
        .product-detail__back:hover { text-decoration: underline; }

        .product-detail__pending { font-size: var(--text-sm); color: var(--color-text-tertiary); }
        .product-detail__notfound { display: grid; gap: var(--spacing-3); padding-block: var(--spacing-8); }
        .product-detail__notfound-title { margin: 0; font-size: var(--text-base); font-weight: 600; color: var(--color-text-primary); }

        .product-detail__layout {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: var(--spacing-8);
          align-items: start;
        }
        @media (max-width: 1024px) {
          .product-detail__layout { grid-template-columns: 1fr; }
        }

        .product-detail__left { display: grid; gap: var(--spacing-3); }

        .product-detail__hero {
          width: 100%;
          max-width: 480px;
          aspect-ratio: 1 / 1;
          object-fit: cover;
          border-radius: var(--radius-md);
          background: var(--color-surface-sunken);
        }
        .product-detail__hero--placeholder {
          background: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 10%, var(--color-surface))
          );
        }
        .product-detail__thumbs {
          display: flex;
          gap: var(--spacing-2);
          flex-wrap: wrap;
        }
        .product-detail__thumb {
          width: 64px;
          height: 64px;
          border: 2px solid var(--color-border-subtle);
          border-radius: var(--radius-sm);
          overflow: hidden;
          cursor: pointer;
          padding: 0;
          background: none;
          transition: border-color var(--motion-fast) var(--ease-out-quart);
        }
        .product-detail__thumb--active { border-color: var(--color-mensa-blue); }
        .product-detail__thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }

        .product-detail__right { display: grid; gap: var(--spacing-4); align-content: start; }

        .product-detail__alt-chip {
          display: inline-block;
          font-size: var(--text-2xs);
          font-weight: 500;
          color: var(--color-text-tertiary);
          background: var(--color-surface-elevated);
          padding: 2px 8px;
          border-radius: var(--radius-full);
        }
        .product-detail__name {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          line-height: 1.2;
        }
        .product-detail__price {
          margin: 0;
          font-size: var(--text-3xl);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-mensa-blue);
        }
        .product-detail__desc {
          margin: 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
          line-height: 1.55;
        }
        .product-detail__actions { display: flex; gap: var(--spacing-3); flex-wrap: wrap; }

        .product-btn {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          padding: 10px var(--spacing-6);
          font-size: var(--text-sm);
          font-weight: 600;
          border-radius: var(--radius-sm);
          text-decoration: none;
          transition: opacity var(--motion-fast) var(--ease-out-quart);
        }
        .product-btn--primary {
          background: var(--color-mensa-blue);
          color: var(--color-text-on-brand);
        }
        .product-btn--primary:hover { opacity: 0.88; }
        .product-btn--secondary {
          background: var(--color-surface-elevated);
          color: var(--color-text-primary);
          border: 1px solid var(--color-border-strong);
        }
        .product-btn--secondary:hover { background: var(--color-surface-sunken); }
      `}</style>
    </div>
  );
}

export function ProductDetailApp({ productId }: { productId: string }) {
  return (
    <MensaProvider>
      <Inner productId={productId} />
    </MensaProvider>
  );
}
