/**
 * Boutique product list island.
 */
import { useEffect, useMemo } from "react";
import { MensaProvider } from "../../lib/MensaProvider";
import { Mensa, type MensaWebBoutiqueProduct } from "../../lib/mensa";
import { useListLoader } from "../../lib/useListLoader";
import { ListSkeleton } from "../_shared/ListSkeleton";

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

function ProductCard({ product }: { product: MensaWebBoutiqueProduct }) {
  return (
    <a href={`/boutique/${product.id}`} className="product-card">
      <div
        className="product-card__photo"
        style={product.imageUrl ? { backgroundImage: `url(${product.imageUrl})` } : undefined}
        aria-hidden="true"
      >
        {!product.imageUrl && <span className="product-card__photo-placeholder" />}
      </div>
      <div className="product-card__body">
        <p className="product-card__name">{product.name}</p>
        <p className="product-card__price">{fmtPrice(product.priceCents)}</p>
      </div>

      <style>{`
        .product-card {
          display: flex;
          flex-direction: column;
          background: var(--color-surface);
          border: 1px solid var(--color-border-subtle);
          border-radius: var(--radius-md);
          text-decoration: none;
          color: inherit;
          overflow: hidden;
          transition: border-color var(--motion-fast) var(--ease-out-quart),
                      transform 160ms cubic-bezier(0.25, 1, 0.5, 1);
        }
        .product-card:hover { border-color: var(--color-mensa-blue); }
        @media (prefers-reduced-motion: no-preference) {
          .product-card:hover { transform: translateY(-1px); }
        }
        .product-card__photo {
          aspect-ratio: 4 / 5;
          width: 100%;
          background-size: cover;
          background-position: center;
          background-color: var(--color-surface-sunken);
        }
        .product-card__photo-placeholder {
          display: block;
          width: 100%;
          height: 100%;
          background: linear-gradient(
            135deg,
            color-mix(in oklch, var(--color-mensa-blue) 10%, var(--color-surface)),
            color-mix(in oklch, var(--color-mensa-cyan) 10%, var(--color-surface))
          );
        }
        .product-card__body {
          padding: var(--spacing-3) var(--spacing-4);
          display: grid;
          gap: var(--spacing-1);
        }
        .product-card__name {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 600;
          color: var(--color-text-primary);
          line-height: 1.35;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }
        .product-card__price {
          margin: 0;
          font-size: var(--text-sm);
          font-weight: 700;
          font-variant-numeric: tabular-nums;
          color: var(--color-mensa-blue);
        }
      `}</style>
    </a>
  );
}

function Inner() {
  const eager = useMemo(() => readLsUser(), []);
  const { items: products, hasFetched } = useListLoader<MensaWebBoutiqueProduct>({
    subscribe: (cb) => Mensa.boutique.subscribeAll(cb),
    refresh: () => Mensa.boutique.refresh(),
  });

  useEffect(() => {
    if (eager === null) window.location.replace("/login");
  }, [eager]);

  return (
    <div className="boutique">
      <header className="boutique__head">
        <div>
          <h1 className="boutique__title">Boutique</h1>
          <p className="boutique__subtitle">Merchandising e gadget ufficiali di Mensa Italia.</p>
        </div>
        <a
          href="https://www.mensa.it"
          target="_blank"
          rel="noopener noreferrer"
          className="boutique__site-btn"
        >
          Vai al sito →
        </a>
      </header>

      {products === null || (!hasFetched && products.length === 0) ? (
        <ListSkeleton count={6} variant="card" />
      ) : hasFetched && products.length === 0 ? (
        <div className="boutique__empty">
          <p className="boutique__empty-title">Catalogo non disponibile</p>
          <p className="boutique__empty-body">Torna più tardi o consulta il sito mensa.it.</p>
        </div>
      ) : (
        <div className="boutique__grid">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}

      <style>{`
        @keyframes boutique-enter {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        @media (prefers-reduced-motion: no-preference) {
          .boutique { animation: boutique-enter 280ms cubic-bezier(0.16, 1, 0.3, 1) both; }
        }

        .boutique { display: grid; gap: var(--spacing-6); }

        .boutique__head {
          display: grid;
          grid-template-columns: 1fr auto;
          align-items: end;
          gap: var(--spacing-5);
          padding-block-end: var(--spacing-5);
          border-block-end: 1px solid var(--color-border-subtle);
        }
        @media (max-width: 640px) { .boutique__head { grid-template-columns: 1fr; } }

        .boutique__title {
          margin: 0;
          font-family: var(--font-display);
          font-size: var(--text-2xl);
          font-weight: 700;
          letter-spacing: -0.02em;
          color: var(--color-text-primary);
          text-wrap: balance;
        }
        .boutique__subtitle {
          margin: var(--spacing-1) 0 0;
          font-size: var(--text-sm);
          color: var(--color-text-secondary);
        }
        .boutique__site-btn {
          padding: 8px var(--spacing-4);
          font-size: var(--text-xs);
          font-weight: 500;
          color: var(--color-mensa-blue);
          border: 1px solid var(--color-border-strong);
          border-radius: var(--radius-sm);
          text-decoration: none;
          background: transparent;
          white-space: nowrap;
        }
        .boutique__site-btn:hover { background: var(--color-surface-elevated); }

        .boutique__grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: var(--spacing-5);
        }
        @media (max-width: 1024px) { .boutique__grid { grid-template-columns: repeat(3, 1fr); } }
        @media (max-width: 768px) { .boutique__grid { grid-template-columns: repeat(2, 1fr); } }
        @media (max-width: 480px) { .boutique__grid { grid-template-columns: 1fr; } }

        .boutique__empty { padding-block: var(--spacing-8); text-align: center; }
        .boutique__empty-title { margin: 0 0 var(--spacing-2); font-size: var(--text-sm); font-weight: 600; color: var(--color-text-primary); }
        .boutique__empty-body { margin: 0; font-size: var(--text-sm); color: var(--color-text-secondary); }
      `}</style>
    </div>
  );
}

export function BoutiqueListApp() {
  return (
    <MensaProvider>
      <Inner />
    </MensaProvider>
  );
}
