/**
 * Shared helpers for the imported staticPages collection.
 * Keeps cluster metadata + page lookup logic out of every .astro page.
 */
import { createReader } from "@keystatic/core/reader";
import keystaticConfig from "../../keystatic.config";

export type ClusterSlug =
  | "chi-siamo"
  | "iscriviti"
  | "concorsi"
  | "intelligenza"
  | "pubblicazioni"
  | "contatti"
  | "legal"
  | "news";

export interface ClusterMeta {
  slug: ClusterSlug;
  label: string;
  tagline: string;
  /** Optional hub page that lives outside /public/info (e.g. /public/about). */
  hubHref?: string;
}

export const CLUSTERS: Record<ClusterSlug, ClusterMeta> = {
  "chi-siamo":     { slug: "chi-siamo",     label: "Chi siamo",      tagline: "Mensa Italia in profondità",      hubHref: "/public/about" },
  iscriviti:       { slug: "iscriviti",     label: "Iscriviti",      tagline: "Tutti i percorsi di ammissione",  hubHref: "/public/info/iscriviti" },
  concorsi:        { slug: "concorsi",      label: "Concorsi",       tagline: "Le competizioni dei soci",        hubHref: "/public/info/concorsi" },
  intelligenza:    { slug: "intelligenza",  label: "Intelligenza",   tagline: "Cos'è il QI, perché si misura",   hubHref: "/public/info/intelligenza" },
  pubblicazioni:   { slug: "pubblicazioni", label: "Pubblicazioni",  tagline: "QUID, talks e archivio",          hubHref: "/public/info/pubblicazioni" },
  contatti:        { slug: "contatti",      label: "Contatti",       tagline: "Come raggiungerci",               hubHref: "/public/info/contatti" },
  legal:           { slug: "legal",         label: "Note legali",    tagline: "Privacy, cookie, dati",           hubHref: "/public/info/legal" },
  news:            { slug: "news",          label: "News",           tagline: "Rassegna stampa e novità",        hubHref: "/public/blog" },
};

export const PUBLIC_CLUSTER_ORDER: ClusterSlug[] = [
  "chi-siamo",
  "iscriviti",
  "concorsi",
  "intelligenza",
  "pubblicazioni",
  "contatti",
];

const reader = createReader(process.cwd(), keystaticConfig);

export interface StaticPageEntry {
  slug: string;
  pageSlug: string;
  cluster: ClusterSlug;
  title: string;
  kicker: string;
  intro: string;
  order: number;
  seoTitle: string;
  seoDescription: string;
  sourceUrl: string;
}

export async function loadAllStaticPages(): Promise<StaticPageEntry[]> {
  const all = await reader.collections.staticPages.all();
  return all.map(({ slug, entry }) => ({
    slug,
    pageSlug: extractPageSlug(slug, entry.cluster as ClusterSlug),
    cluster: entry.cluster as ClusterSlug,
    title: entry.title ?? "",
    kicker: entry.kicker ?? "",
    intro: entry.intro ?? "",
    order: entry.order ?? 100,
    seoTitle: entry.seoTitle ?? "",
    seoDescription: entry.seoDescription ?? "",
    sourceUrl: entry.sourceUrl ?? "",
  }));
}

function extractPageSlug(fullSlug: string, cluster: ClusterSlug): string {
  const prefix = `${cluster}-`;
  return fullSlug.startsWith(prefix) ? fullSlug.slice(prefix.length) : fullSlug;
}

export function groupByCluster(pages: StaticPageEntry[]): Map<ClusterSlug, StaticPageEntry[]> {
  const out = new Map<ClusterSlug, StaticPageEntry[]>();
  for (const p of pages) {
    const list = out.get(p.cluster) ?? [];
    list.push(p);
    out.set(p.cluster, list);
  }
  for (const list of out.values()) list.sort((a, b) => a.order - b.order);
  return out;
}

export async function loadPage(cluster: ClusterSlug, pageSlug: string) {
  const fullSlug = `${cluster}-${pageSlug}`;
  return reader.collections.staticPages.read(fullSlug);
}

export function pageHref(cluster: ClusterSlug, pageSlug: string): string {
  return `/public/info/${cluster}/${pageSlug}`;
}

export function clusterHubHref(cluster: ClusterSlug): string {
  return CLUSTERS[cluster].hubHref ?? `/public/info/${cluster}`;
}
