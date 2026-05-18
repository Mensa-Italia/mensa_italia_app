/**
 * Server-side helpers to call the public (unauthenticated) PocketBase endpoints
 * used by the marketing site. Mirrors `pb.fullListUnauthenticated` from the KMP
 * shared module — same base URL, no token, same filter/sort grammar.
 *
 * The same data flows through `Mensa.events.subscribeAll` on the authenticated
 * dashboard side; here we just bypass the KMP runtime since the marketing
 * pages render server-side and need no SQLite cache.
 */

const API_BASE = "https://svc.mensa.it";

export interface PbRecord {
  id: string;
  collectionId: string;
  collectionName: string;
  created: string;
  updated: string;
  [k: string]: unknown;
}

interface PbListResponse<T> {
  items: T[];
  totalItems: number;
  page: number;
  perPage: number;
  totalPages: number;
}

export interface PublicEventRecord extends PbRecord {
  name: string;
  description: string;
  image: string;
  info_link: string;
  booking_link: string;
  when_start: string;
  when_end: string;
  is_national: boolean;
  is_public: boolean;
  is_spot: boolean;
  contact: string;
  expand?: {
    position?: {
      id: string;
      collectionId: string;
      name: string;
      address: string;
      latitude: number;
      longitude: number;
    };
    owner?: {
      id: string;
      name: string;
      region: string;
    };
  };
}

export interface PublicLocalOfficeRecord extends PbRecord {
  name: string;
  slug: string;
  bio: string;
  region: string;
  /** PocketBase filename for the office cover/header. */
  image: string;
}

async function pbList<T>(
  collection: string,
  params: Record<string, string | undefined> = {},
): Promise<readonly T[]> {
  const url = new URL(`${API_BASE}/api/collections/${collection}/records`);
  url.searchParams.set("perPage", "200");
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== "") url.searchParams.set(k, v);
  }
  const res = await fetch(url.toString(), {
    headers: { Accept: "application/json" },
  });
  if (!res.ok) {
    throw new Error(`PB ${collection} ${res.status} ${res.statusText}`);
  }
  const json = (await res.json()) as PbListResponse<T>;
  return json.items;
}

/**
 * Build the PocketBase file URL for a given record's field. PB stores filenames
 * in the field and exposes them at `/api/files/{collectionId}/{recordId}/{filename}`.
 */
export function pbFileUrl(record: { collectionId: string; id: string }, filename: string): string {
  if (!filename) return "";
  return `${API_BASE}/api/files/${record.collectionId}/${record.id}/${filename}`;
}

/** Future-only events flagged `is_public`. Sorted by start ascending. */
export async function listPublicEvents(): Promise<readonly PublicEventRecord[]> {
  const nowIso = new Date().toISOString();
  return pbList<PublicEventRecord>("events", {
    filter: `is_public=true && when_end>="${nowIso}"`,
    sort: "when_start",
    expand: "position,owner",
  });
}

/** All local offices, public read via the `view_local_office` PB view. */
export async function listPublicLocalOffices(): Promise<readonly PublicLocalOfficeRecord[]> {
  return pbList<PublicLocalOfficeRecord>("view_local_office", {
    sort: "name",
  });
}

/** Single local office by slug. */
export async function publicLocalOfficeBySlug(
  slug: string,
): Promise<PublicLocalOfficeRecord | null> {
  // Escape any quote in slug. PB filter syntax uses double quotes.
  const safe = slug.replace(/"/g, '\\"');
  const items = await pbList<PublicLocalOfficeRecord>("view_local_office", {
    filter: `slug="${safe}"`,
  });
  return items[0] ?? null;
}

// ── Local office detail ─────────────────────────────────────────────

export interface PublicLocalOfficeAdmin extends PbRecord {
  name: string;
  email: string;
  image: string;
  is_the_officer: boolean;
  local_office: string;
  region: string;
  user: string;
}

export interface PublicLocalOfficeLinktreeRow extends PbRecord {
  /** Tipo della riga: "section" | "link" (dipende dallo schema PB). */
  kind: string;
  /** Indica il parent section per le righe `link`. */
  parent: string;
  local_office: string;
  title: string;
  url: string;
  icon: string;
  sort_order: number;
}

export interface PublicLocalOfficeTestDate extends PbRecord {
  date: string;
  location: string;
  max_participants: number;
  notes: string;
  local_office: string;
  assistants: string[];
}

/** Linktree pubblico dell'office, ordinato per `sort_order`. */
export async function publicLinktreeByOffice(
  officeId: string,
): Promise<readonly PublicLocalOfficeLinktreeRow[]> {
  const safe = officeId.replace(/"/g, '\\"');
  return pbList<PublicLocalOfficeLinktreeRow>("view_local_office_linktree", {
    filter: `local_office="${safe}"`,
    sort: "sort_order",
  });
}

/** Admins (segretario + cosegretari) pubblici dell'office. */
export async function publicAdminsByOffice(
  officeId: string,
): Promise<readonly PublicLocalOfficeAdmin[]> {
  const safe = officeId.replace(/"/g, '\\"');
  return pbList<PublicLocalOfficeAdmin>("view_local_office_admins", {
    filter: `local_office="${safe}"`,
  });
}

export interface PublicLocalOfficeAssistant extends PbRecord {
  name: string;
  email: string;
  image: string;
  /** Provincia. */
  area: string;
  city: string;
  state: string;
  region: string;
  local_office: string;
  local_office_name: string;
  user: string;
}

/** Test assistants pubblici dell'office. */
export async function publicAssistantsByOffice(
  officeId: string,
): Promise<readonly PublicLocalOfficeAssistant[]> {
  const safe = officeId.replace(/"/g, '\\"');
  return pbList<PublicLocalOfficeAssistant>("view_local_office_assistants", {
    filter: `local_office="${safe}"`,
  });
}

// ── Podcasts & Quid (pubblici) ──────────────────────────────────────

export interface PublicPodcastRecord extends PbRecord {
  title: string;
  description: string;
  image: string;
  episodes_count: number;
  youtube_playlist_id: string;
}

export interface PublicQuidIssueRecord extends PbRecord {
  name: string;
  slug: string;
  number: number;
  articles_count: number;
  /** Direct URL (from WordPress), not a PB file. Use as-is. */
  image: string;
  pdf_url: string;
  published_at: string;
  category_id: string;
}

/** Tutti i podcast, sort by created desc. */
export async function listPublicPodcasts(): Promise<readonly PublicPodcastRecord[]> {
  return pbList<PublicPodcastRecord>("podcasts", { sort: "-created" });
}

/** Numeri pubblicati di Quid, sort by number desc. */
export async function listPublicQuidIssues(): Promise<readonly PublicQuidIssueRecord[]> {
  return pbList<PublicQuidIssueRecord>("quid_issues", { sort: "-number" });
}

// ── Event schedules ────────────────────────────────────────────────

export interface EventScheduleRecord extends PbRecord {
  title: string;
  event: string;
  description: string;
  image: string;
  when_start: string;
  when_end: string;
  max_external_guests: number;
  price: number;
  info_link: string;
  is_subscriptable: boolean;
}

/** Sotto-sessioni / programma di un evento (collection `events_schedule`). */
export async function listEventSchedules(
  eventId: string,
): Promise<readonly EventScheduleRecord[]> {
  const safe = eventId.replace(/"/g, '\\"');
  return pbList<EventScheduleRecord>("events_schedule", {
    filter: `event="${safe}"`,
    sort: "when_start",
  });
}

/** Date test futuri pubbliche dell'office. */
export async function publicUpcomingTestDatesByOffice(
  officeId: string,
): Promise<readonly PublicLocalOfficeTestDate[]> {
  const safe = officeId.replace(/"/g, '\\"');
  const nowIso = new Date().toISOString();
  return pbList<PublicLocalOfficeTestDate>("view_local_office_test_dates", {
    filter: `local_office="${safe}" && date>="${nowIso}"`,
    sort: "date",
  });
}
