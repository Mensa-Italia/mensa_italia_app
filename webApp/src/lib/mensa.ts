/**
 * Bridge to the Kotlin Multiplatform shared module compiled to ESM JS.
 *
 * The Kotlin/JS target was configured with `useEsModules()` so the emitted
 * `shared.mjs` exports `MensaWebSdk`, `MensaWebAuth`, `MensaWebUser`, plus the
 * per-feature `MensaWeb<Feature>` classes flat at the module root. We re-shape
 * the Kotlin types into idiomatic TS POJOs at the boundary so React renders +
 * memoization compare cleanly and consumers never see `KtList`/`KtMap` or
 * Kotlin property accessors.
 *
 * To rebuild after changing Kotlin sources, run from web/:
 *   bun run build:shared
 *
 * Source: ../mensa-kmp/shared/src/jsMain/kotlin/it/mensa/web/MensaWeb.kt
 */
import {
  MensaWebSdk as KotlinSdk,
  EventCreatePayload as KotlinEventCreatePayload,
  EventUpdatePayload as KotlinEventUpdatePayload,
  EventScheduleCreatePayload as KotlinEventScheduleCreatePayload,
  EventScheduleUpdatePayload as KotlinEventScheduleUpdatePayload,
  DealCreatePayload as KotlinDealCreatePayload,
  DealUpdatePayload as KotlinDealUpdatePayload,
  DealContactPayload as KotlinDealContactPayload,
  SigCreatePayload as KotlinSigCreatePayload,
  SigUpdatePayload as KotlinSigUpdatePayload,
  PositionCreatePayload as KotlinPositionCreatePayload,
  LocalOfficeLinkCreatePayload as KotlinLocalOfficeLinkCreatePayload,
  LocalOfficeLinkUpdatePayload as KotlinLocalOfficeLinkUpdatePayload,
  LocalOfficeTestDateCreatePayload as KotlinLocalOfficeTestDateCreatePayload,
  LocalOfficeTestDateUpdatePayload as KotlinLocalOfficeTestDateUpdatePayload,
  type MensaWebAddon as KotlinAddon,
  type MensaWebAddons as KotlinAddons,
  type MensaWebAuth as KotlinAuth,
  type MensaWebBoutique as KotlinBoutique,
  type MensaWebBoutiqueProduct as KotlinBoutiqueProduct,
  type MensaWebDeal as KotlinDeal,
  type MensaWebDealContact as KotlinDealContact,
  type MensaWebDeals as KotlinDeals,
  type MensaWebDevice as KotlinDevice,
  type MensaWebDevices as KotlinDevices,
  type MensaWebPosition as KotlinPosition,
  type MensaWebPositions as KotlinPositions,
  type MensaWebDocument as KotlinDocument,
  type MensaWebDocumentSummary as KotlinDocumentSummary,
  type MensaWebDocuments as KotlinDocuments,
  type MensaWebEvent as KotlinEvent,
  type MensaWebEventSchedule as KotlinEventSchedule,
  type MensaWebEvents as KotlinEvents,
  type MensaWebI18n as KotlinI18n,
  type MensaWebLocalOffice as KotlinLocalOffice,
  type MensaWebLocalOfficeLink as KotlinLocalOfficeLink,
  type MensaWebLocalOfficeMember as KotlinLocalOfficeMember,
  type MensaWebLocalOffices as KotlinLocalOffices,
  type MensaWebMetadata as KotlinMetadata,
  type MensaWebMember as KotlinMember,
  type MensaWebNotification as KotlinNotification,
  type MensaWebNotifications as KotlinNotifications,
  type MensaWebPodcast as KotlinPodcast,
  type MensaWebPodcastEpisode as KotlinPodcastEpisode,
  type MensaWebPodcasts as KotlinPodcasts,
  type MensaWebQuid as KotlinQuid,
  type MensaWebQuidArticle as KotlinQuidArticle,
  type MensaWebQuidIssue as KotlinQuidIssue,
  type MensaWebReceipt as KotlinReceipt,
  type MensaWebReceipts as KotlinReceipts,
  type MensaWebRegSoci as KotlinRegSoci,
  type MensaWebSearch as KotlinSearch,
  type MensaWebSearchHit as KotlinSearchHit,
  type MensaWebSig as KotlinSig,
  type MensaWebSigs as KotlinSigs,
  type MensaWebTestDate as KotlinTestDate,
  type MensaWebTicket as KotlinTicket,
  type MensaWebTickets as KotlinTickets,
  type MensaWebUser as KotlinUser,
} from "mensa-shared";

// ── Type mirrors ─────────────────────────────────────────────────────────────

export type AuthStateKind = "Unknown" | "Anonymous" | "Authenticated";
export type SearchStateKind = "idle" | "loading" | "success" | "error";
export type TicketStatus = "active" | "expired";
export type ReceiptKind = "donation" | "renewal" | "purchase" | "other";

/** Plain mirror of the Kotlin `MensaWebUser` data class. */
export interface MensaWebUser {
  id: string;
  username: string;
  name: string;
  avatar: string;
  email: string;
  expireMembershipMs: number;
  powers: readonly string[];
  addons: readonly string[];
  isMembershipActive: boolean;
  createdMs: number;
}

export interface MensaWebEvent {
  id: string;
  title: string;
  description: string;
  /** Full public URL for use in <img> tags. Empty string if no image. */
  coverUrl: string;
  /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
  image: string;
  infoLink: string;
  bookingLink: string;
  startsMs: number;
  endsMs: number;
  isNational: boolean;
  isOnline: boolean;
  isPublic: boolean;
  isSpot: boolean;
  region: string;
  locationName: string;
  locationAddress: string;
  /** PocketBase id of the linked saved position; empty when online. */
  locationId: string;
  ownerName: string;
}

export interface MensaWebDeal {
  id: string;
  name: string;
  sector: string;
  description: string;
  eligibility: string;
  howToGet: string;
  validFromMs: number;
  validUntilMs: number;
  discount: string;
  link: string;
  /** Full public URL for use in <img> tags. Empty string if no image. */
  coverUrl: string;
  /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
  image: string;
  isActive: boolean;
  isLocal: boolean;
  region: string;
  locationName: string;
  locationAddress: string;
  vatNumber: string;
}

export interface MensaWebDealContact {
  id: string;
  name: string;
  email: string;
  phone: string;
  note: string;
}

export interface MensaWebNotification {
  id: string;
  titleKey: string;
  bodyKey: string;
  /** Named params as a plain dict — reconstructed from the Kotlin flat array. */
  params: Record<string, string>;
  targetType: string;
  targetId: string;
  createdMs: number;
  /** Seconds since epoch when seen; 0 means unread. */
  seenMs: number;
}

export interface MensaWebMember {
  id: string;
  name: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  region: string;
  city: string;
  avatarUrl: string;
  sigs: readonly string[];
  localOffices: readonly string[];
  fullProfileLink: string;
  /** Epoch ms; 0 when the backend didn't supply a birthdate. */
  birthdateMs: number;
  /** Extra key/value fields from PocketBase `full_data`. Mobile classifies
   *  these by keyword into Profile / Mensa / Contatti / SIG sections. */
  fullData: readonly { key: string; value: string }[];
}

export interface MensaWebSig {
  id: string;
  name: string;
  link: string;
  description: string;
  groupType: string;
  /** Full public URL for use in <img> tags. Empty string if no image. */
  coverUrl: string;
  /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
  image: string;
}

export interface MensaWebTicket {
  id: string;
  name: string;
  description: string;
  status: TicketStatus;
  qrPayload: string;
  /** 0 if no deadline set. */
  deadlineMs: number;
  internalRef: string;
  createdMs: number;
  linkUrl: string;
  customerData: string;
}

export interface MensaWebReceipt {
  id: string;
  kind: ReceiptKind;
  description: string;
  amountCents: number;
  dateMs: number;
  stripeCode: string;
  status: string;
}

export interface MensaWebLocalOffice {
  id: string;
  slug: string;
  name: string;
  kicker: string;
  bio: string;
  region: string;
  coverUrl: string;
}

export interface MensaWebLocalOfficeMember {
  id: string;
  role: "officer" | "admin" | "assistant";
  name: string;
  email: string;
  avatarUrl: string;
  region: string;
  officeId: string;
  officeName: string;
}

export interface MensaWebLocalOfficeLink {
  id: string;
  officeId: string;
  kind: string;
  parentId: string;
  title: string;
  url: string;
  icon: string;
  sortOrder: number;
}

export interface MensaWebQuidIssue {
  id: number;
  slug: string;
  title: string;
  number: number;
  description: string;
  articleCount: number;
  coverUrl: string;
  isPdf: boolean;
  pdfUrl: string;
}

export interface MensaWebQuidArticle {
  id: number;
  issueId: number;
  title: string;
  byline: string;
  leadHtml: string;
  bodyHtml: string;
  heroImageUrl: string;
  audioUrl: string;
  durationSec: number;
  wpUrl: string;
}

export interface MensaWebPodcast {
  id: string;
  title: string;
  description: string;
  coverUrl: string;
  episodeCount: number;
  totalDurationSec: number;
}

export interface MensaWebPodcastEpisode {
  id: string;
  podcastId: string;
  title: string;
  description: string;
  audioUrl: string;
  coverUrl: string;
  durationSec: number;
  publishedMs: number;
}

export interface MensaWebDocument {
  id: string;
  title: string;
  description: string;
  category: string;
  dateMs: number;
  pdfUrl: string;
  elaboratedId: string;
  uploadedBy: string;
}

export interface MensaWebDocumentSummary {
  id: string;
  documentId: string;
  markdown: string;
}

export interface MensaWebBoutiqueProduct {
  id: string;
  name: string;
  description: string;
  priceCents: number;
  imageUrl: string;
  imageUrls: readonly string[];
  orderUrl: string;
  alternativeOf: string;
}

export interface MensaWebAddon {
  id: string;
  name: string;
  description: string;
  iconUrl: string;
  version: string;
  url: string;
  requiredPower: number;
}

export interface MensaWebSearchHit {
  type: string;
  id: string;
  label: string;
  sublabel: string;
  imageUrl: string;
  url: string;
  score: number;
}

export interface MensaWebEventSchedule {
  id: string;
  title: string;
  eventId: string;
  description: string;
  startsMs: number;
  endsMs: number;
  maxExternalGuests: number;
  price: number;
  infoLink: string;
  isSubscriptable: boolean;
}

export interface MensaWebTestDate {
  id: string;
  officeId: string;
  officeName: string;
  region: string;
  dateMs: number;
  location: string;
  notes: string;
  maxParticipants: number;
  assistants: readonly string[];
}

export interface MensaWebDevice {
  id: string;
  userId: string;
  deviceName: string;
  firebaseId: string;
  createdMs: number;
  updatedMs: number;
}

export interface MensaWebPosition {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  state: string;
}

// ── Write payload interfaces (used as input for create/update calls) ──────────

export interface EventCreateInput {
  name: string;
  description: string;
  /** Filename already on PocketBase storage — multipart upload is a future wave. */
  image?: string;
  infoLink?: string;
  bookingLink?: string;
  startsMs: number;
  endsMs: number;
  isNational?: boolean;
  isOnline?: boolean;
  isPublic?: boolean;
  isSpot?: boolean;
  contact?: string;
  region?: string;
  positionId?: string | null;
  ownerId: string;
}

export type EventUpdateInput = EventCreateInput;

export interface EventScheduleCreateInput {
  title: string;
  eventId: string;
  description?: string;
  startsMs: number;
  endsMs: number;
  maxExternalGuests?: number;
  price?: number;
  infoLink?: string;
  isSubscriptable?: boolean;
}

export interface EventScheduleUpdateInput {
  title: string;
  description?: string;
  startsMs: number;
  endsMs: number;
  maxExternalGuests?: number;
  price?: number;
  infoLink?: string;
  isSubscriptable?: boolean;
}

export interface DealCreateInput {
  name: string;
  commercialSector: string;
  details?: string;
  who?: string;
  howToGet?: string;
  link?: string;
  vatNumber?: string;
  positionId?: string | null;
  validFromMs?: number;
  validUntilMs?: number;
}

export type DealUpdateInput = DealCreateInput;

export interface DealContactInput {
  name: string;
  email: string;
  phone?: string;
  note?: string;
  dealId: string;
}

export interface SigCreateInput {
  name: string;
  link: string;
  groupType: string;
  description?: string;
  /** PocketBase filename (no URL). Non-empty only when image was already uploaded. */
  image?: string;
}

export type SigUpdateInput = SigCreateInput;

export interface PositionCreateInput {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  createdBy?: string;
}

export interface LocalOfficeLinkCreateInput {
  officeId: string;
  kind: string;
  parentId?: string;
  title: string;
  url?: string;
  icon?: string;
  sortOrder?: number;
}

export interface LocalOfficeLinkUpdateInput {
  kind: string;
  parentId?: string;
  title: string;
  url?: string;
  icon?: string;
  sortOrder?: number;
}

export interface LocalOfficeTestDateCreateInput {
  officeId: string;
  dateMs: number;
  location: string;
  notes?: string;
  maxParticipants?: number;
  assistants?: string[];
}

export interface LocalOfficeTestDateUpdateInput {
  dateMs: number;
  location: string;
  notes?: string;
  maxParticipants?: number;
  assistants?: string[];
}

// ── Snapshot helpers (Kotlin → plain TS object) ──────────────────────────────

function snapshotUser(u: KotlinUser): MensaWebUser {
  return {
    id: u.id,
    username: u.username,
    name: u.name,
    avatar: u.avatar,
    email: u.email,
    expireMembershipMs: u.expireMembershipMs,
    powers: [...u.powers],
    addons: [...u.addons],
    isMembershipActive: u.isMembershipActive,
    createdMs: u.createdMs,
  };
}

function snapshotEvent(e: KotlinEvent): MensaWebEvent {
  return {
    id: e.id,
    title: e.title,
    description: e.description,
    coverUrl: e.coverUrl,
    image: e.image,
    infoLink: e.infoLink,
    bookingLink: e.bookingLink,
    startsMs: e.startsMs,
    endsMs: e.endsMs,
    isNational: e.isNational,
    isOnline: e.isOnline,
    isPublic: e.isPublic,
    isSpot: e.isSpot,
    region: e.region,
    locationName: e.locationName,
    locationAddress: e.locationAddress,
    locationId: e.locationId,
    ownerName: e.ownerName,
  };
}

function snapshotDeal(d: KotlinDeal): MensaWebDeal {
  return {
    id: d.id,
    name: d.name,
    sector: d.sector,
    description: d.description,
    eligibility: d.eligibility,
    howToGet: d.howToGet,
    validFromMs: d.validFromMs,
    validUntilMs: d.validUntilMs,
    discount: d.discount,
    link: d.link,
    coverUrl: d.coverUrl,
    image: d.image,
    isActive: d.isActive,
    isLocal: d.isLocal,
    region: d.region,
    locationName: d.locationName,
    locationAddress: d.locationAddress,
    vatNumber: d.vatNumber,
  };
}

function snapshotDealContact(c: KotlinDealContact): MensaWebDealContact {
  return {
    id: c.id,
    name: c.name,
    email: c.email,
    phone: c.phone,
    note: c.note,
  };
}

function snapshotNotification(n: KotlinNotification): MensaWebNotification {
  // The Kotlin side flattens the named-params dict to ["k1", "v1", "k2", "v2", ...]
  // because `@JsExport` can't carry a Kotlin Map. Reconstruct as plain object.
  const params: Record<string, string> = {};
  const flat = n.params;
  for (let i = 0; i + 1 < flat.length; i += 2) {
    params[flat[i]!] = flat[i + 1]!;
  }
  return {
    id: n.id,
    titleKey: n.titleKey,
    bodyKey: n.bodyKey,
    params,
    targetType: n.targetType,
    targetId: n.targetId,
    createdMs: n.createdMs,
    seenMs: n.seenMs,
  };
}

function snapshotMember(m: KotlinMember): MensaWebMember {
  return {
    id: m.id,
    name: m.name,
    firstName: m.firstName,
    lastName: m.lastName,
    email: m.email,
    phone: m.phone,
    region: m.region,
    city: m.city,
    avatarUrl: m.avatarUrl,
    sigs: [...m.sigs],
    localOffices: [...m.localOffices],
    fullProfileLink: m.fullProfileLink,
    // The bridge fields below were added later; guard against any stale
    // shared bundle that doesn't expose them yet so the React island
    // doesn't crash mid-render.
    birthdateMs: typeof m.birthdateMs === "number" ? m.birthdateMs : 0,
    fullData: Array.isArray(m.fullData)
      ? m.fullData.map((f) => ({ key: f.key, value: f.value }))
      : [],
  };
}

function snapshotSig(s: KotlinSig): MensaWebSig {
  return {
    id: s.id,
    name: s.name,
    link: s.link,
    description: s.description,
    groupType: s.groupType,
    coverUrl: s.coverUrl,
    image: s.image,
  };
}

function snapshotPosition(p: KotlinPosition): MensaWebPosition {
  return {
    id: p.id,
    name: p.name,
    address: p.address,
    latitude: p.latitude,
    longitude: p.longitude,
    state: p.state,
  };
}

function snapshotTicket(t: KotlinTicket): MensaWebTicket {
  return {
    id: t.id,
    name: t.name,
    description: t.description,
    status: t.status as TicketStatus,
    qrPayload: t.qrPayload,
    deadlineMs: t.deadlineMs,
    internalRef: t.internalRef,
    createdMs: t.createdMs,
    linkUrl: t.linkUrl,
    customerData: t.customerData,
  };
}

function snapshotReceipt(r: KotlinReceipt): MensaWebReceipt {
  return {
    id: r.id,
    kind: r.kind as ReceiptKind,
    description: r.description,
    amountCents: r.amountCents,
    dateMs: r.dateMs,
    stripeCode: r.stripeCode,
    status: r.status,
  };
}

function snapshotLocalOffice(o: KotlinLocalOffice): MensaWebLocalOffice {
  return {
    id: o.id,
    slug: o.slug,
    name: o.name,
    kicker: o.kicker,
    bio: o.bio,
    region: o.region,
    coverUrl: o.coverUrl,
  };
}

function snapshotLocalOfficeMember(m: KotlinLocalOfficeMember): MensaWebLocalOfficeMember {
  return {
    id: m.id,
    role: m.role as MensaWebLocalOfficeMember["role"],
    name: m.name,
    email: m.email,
    avatarUrl: m.avatarUrl,
    region: m.region,
    officeId: m.officeId,
    officeName: m.officeName,
  };
}

function snapshotLocalOfficeLink(l: KotlinLocalOfficeLink): MensaWebLocalOfficeLink {
  return {
    id: l.id,
    officeId: l.officeId,
    kind: l.kind,
    parentId: l.parentId,
    title: l.title,
    url: l.url,
    icon: l.icon,
    sortOrder: l.sortOrder,
  };
}

function snapshotQuidIssue(i: KotlinQuidIssue): MensaWebQuidIssue {
  return {
    id: i.id,
    slug: i.slug,
    title: i.title,
    number: i.number,
    description: i.description,
    articleCount: i.articleCount,
    coverUrl: i.coverUrl,
    isPdf: i.isPdf,
    pdfUrl: i.pdfUrl,
  };
}

function snapshotQuidArticle(a: KotlinQuidArticle): MensaWebQuidArticle {
  return {
    id: a.id,
    issueId: a.issueId,
    title: a.title,
    byline: a.byline,
    leadHtml: a.leadHtml,
    bodyHtml: a.bodyHtml,
    heroImageUrl: a.heroImageUrl,
    audioUrl: a.audioUrl,
    durationSec: a.durationSec,
    wpUrl: a.wpUrl,
  };
}

function snapshotPodcast(p: KotlinPodcast): MensaWebPodcast {
  return {
    id: p.id,
    title: p.title,
    description: p.description,
    coverUrl: p.coverUrl,
    episodeCount: p.episodeCount,
    totalDurationSec: p.totalDurationSec,
  };
}

function snapshotPodcastEpisode(e: KotlinPodcastEpisode): MensaWebPodcastEpisode {
  return {
    id: e.id,
    podcastId: e.podcastId,
    title: e.title,
    description: e.description,
    audioUrl: e.audioUrl,
    coverUrl: e.coverUrl,
    durationSec: e.durationSec,
    publishedMs: e.publishedMs,
  };
}

function snapshotDocument(d: KotlinDocument): MensaWebDocument {
  return {
    id: d.id,
    title: d.title,
    description: d.description,
    category: d.category,
    dateMs: d.dateMs,
    pdfUrl: d.pdfUrl,
    elaboratedId: d.elaboratedId,
    uploadedBy: d.uploadedBy,
  };
}

function snapshotDocumentSummary(s: KotlinDocumentSummary): MensaWebDocumentSummary {
  return {
    id: s.id,
    documentId: s.documentId,
    markdown: s.markdown,
  };
}

function snapshotBoutiqueProduct(p: KotlinBoutiqueProduct): MensaWebBoutiqueProduct {
  return {
    id: p.id,
    name: p.name,
    description: p.description,
    priceCents: p.priceCents,
    imageUrl: p.imageUrl,
    imageUrls: [...p.imageUrls],
    orderUrl: p.orderUrl,
    alternativeOf: p.alternativeOf,
  };
}

function snapshotAddon(a: KotlinAddon): MensaWebAddon {
  return {
    id: a.id,
    name: a.name,
    description: a.description,
    iconUrl: a.iconUrl,
    version: a.version,
    url: a.url,
    requiredPower: a.requiredPower,
  };
}

function snapshotSearchHit(h: KotlinSearchHit): MensaWebSearchHit {
  return {
    type: h.type,
    id: h.id,
    label: h.label,
    sublabel: h.sublabel,
    imageUrl: h.imageUrl,
    url: h.url,
    score: h.score,
  };
}

function snapshotEventSchedule(s: KotlinEventSchedule): MensaWebEventSchedule {
  return {
    id: s.id,
    title: s.title,
    eventId: s.eventId,
    description: s.description,
    startsMs: s.startsMs,
    endsMs: s.endsMs,
    maxExternalGuests: s.maxExternalGuests,
    price: s.price,
    infoLink: s.infoLink,
    isSubscriptable: s.isSubscriptable,
  };
}

function snapshotTestDate(t: KotlinTestDate): MensaWebTestDate {
  return {
    id: t.id,
    officeId: t.officeId,
    officeName: t.officeName,
    region: t.region,
    dateMs: t.dateMs,
    location: t.location,
    notes: t.notes,
    maxParticipants: t.maxParticipants,
    assistants: [...t.assistants],
  };
}

function snapshotDevice(d: KotlinDevice): MensaWebDevice {
  return {
    id: d.id,
    userId: d.userId,
    deviceName: d.deviceName,
    firebaseId: d.firebaseId,
    createdMs: d.createdMs,
    updatedMs: d.updatedMs,
  };
}

// ── Subscription plumbing ───────────────────────────────────────────────────

/**
 * Wraps a Kotlin `subscribe*` thunk so callers always get a synchronous
 * unsubscribe even though we have to `await initialize()` first. The
 * Kotlin-side callback fires post-init; the returned closure stays callable
 * even if invoked before init completes (it sets a flag that suppresses the
 * deferred attach).
 */
function deferredSubscribe<T>(
  initialize: () => Promise<void>,
  attach: () => (cb: (value: T) => void) => () => void,
  cb: (value: T) => void,
): () => void {
  let cancel: () => void = () => {};
  let cancelled = false;
  initialize().then(() => {
    if (cancelled) return;
    cancel = attach()(cb);
  });
  return () => {
    cancelled = true;
    cancel();
  };
}

// ── Main SDK wrapper ─────────────────────────────────────────────────────────

class MensaWebSdkBridge {
  private readonly sdk: KotlinSdk;
  private readonly i18n_: KotlinI18n;
  private readonly auth_: KotlinAuth;
  private readonly events_: KotlinEvents;
  private readonly deals_: KotlinDeals;
  private readonly notifications_: KotlinNotifications;
  private readonly regSoci_: KotlinRegSoci;
  private readonly sigs_: KotlinSigs;
  private readonly tickets_: KotlinTickets;
  private readonly receipts_: KotlinReceipts;
  private readonly localOffices_: KotlinLocalOffices;
  private readonly devices_: KotlinDevices;
  private readonly positions_: KotlinPositions;
  private readonly quid_: KotlinQuid;
  private readonly podcasts_: KotlinPodcasts;
  private readonly documents_: KotlinDocuments;
  private readonly boutique_: KotlinBoutique;
  private readonly addons_: KotlinAddons;
  private readonly search_: KotlinSearch;
  private readonly metadata_: KotlinMetadata;
  private initOnce: Promise<void> | null = null;

  constructor() {
    this.sdk = new KotlinSdk();
    this.i18n_ = this.sdk.i18n;
    this.auth_ = this.sdk.auth;
    this.events_ = this.sdk.events;
    this.deals_ = this.sdk.deals;
    this.notifications_ = this.sdk.notifications;
    this.regSoci_ = this.sdk.regSoci;
    this.sigs_ = this.sdk.sigs;
    this.tickets_ = this.sdk.tickets;
    this.receipts_ = this.sdk.receipts;
    this.localOffices_ = this.sdk.localOffices;
    this.devices_ = this.sdk.devices;
    this.positions_ = this.sdk.positions;
    this.quid_ = this.sdk.quid;
    this.podcasts_ = this.sdk.podcasts;
    this.documents_ = this.sdk.documents;
    this.boutique_ = this.sdk.boutique;
    this.addons_ = this.sdk.addons;
    this.search_ = this.sdk.search;
    this.metadata_ = this.sdk.metadata;
  }

  initialize(): Promise<void> {
    if (this.initOnce === null) {
      this.initOnce = this.sdk.initialize();
    }
    return this.initOnce;
  }

  /**
   * Translate a Tolgee key synchronously after init completes.
   * `params` replaces `{name}` ICU placeholders in the string.
   * Returns `fallback` while the SDK is still initialising.
   *
   * Signature: `t(key, fallback, params?) => string`
   */
  readonly i18n = {
    t: (key: string, fallback: string, params?: Record<string, string>): string => {
      // Build the flat [k1,v1,k2,v2,...] array the Kotlin bridge expects.
      const flat: string[] = [];
      if (params) {
        for (const [k, v] of Object.entries(params)) {
          flat.push(k, v);
        }
      }
      try {
        return this.i18n_.t(key, fallback, flat);
      } catch {
        // Koin not yet ready (called before initialize() resolves) — safe fallback.
        return fallback;
      }
    },
  };

  /**
   * The user object is mirrored to localStorage at the bridge layer: on
   * iOS/Android the shared module caches the user in SQLite which survives app
   * restarts, but on web sql.js runs in-memory and is wiped on every page
   * reload. Persisting the user JSON in localStorage gives the same
   * "session restored" UX without needing IndexedDB plumbing.
   */
  private static readonly USER_LS_KEY = "mensa.auth.user";

  private readUserSnapshotFromLs(): MensaWebUser | null {
    const cached = localStorage.getItem(MensaWebSdkBridge.USER_LS_KEY);
    if (!cached) return null;
    try {
      return JSON.parse(cached) as MensaWebUser;
    } catch {
      localStorage.removeItem(MensaWebSdkBridge.USER_LS_KEY);
      return null;
    }
  }

  readonly auth = {
    login: async (email: string, password: string): Promise<MensaWebUser> => {
      await this.initialize();
      const u = await this.auth_.login(email, password);
      const snap = snapshotUser(u);
      localStorage.setItem(MensaWebSdkBridge.USER_LS_KEY, JSON.stringify(snap));
      return snap;
    },
    logout: async (): Promise<void> => {
      await this.initialize();
      localStorage.removeItem(MensaWebSdkBridge.USER_LS_KEY);
      return this.auth_.logout();
    },
    subscribeAuthState: (cb: (state: AuthStateKind) => void): (() => void) =>
      deferredSubscribe<string>(
        () => this.initialize(),
        () => (inner) => this.auth_.subscribeAuthState(inner),
        (s) => cb(s as AuthStateKind),
      ),
    subscribeCurrentUser: (cb: (user: MensaWebUser | null) => void): (() => void) => {
      let cancel: () => void = () => {};
      let cancelled = false;
      // Immediate emission from localStorage (if any) so the UI doesn't
      // flash "loading" on a session-restored navigation.
      const lsSnap = this.readUserSnapshotFromLs();
      if (lsSnap) cb(lsSnap);
      this.initialize().then(() => {
        if (cancelled) return;
        cancel = this.auth_.subscribeCurrentUser((u) => {
          if (u) {
            const snap = snapshotUser(u);
            localStorage.setItem(MensaWebSdkBridge.USER_LS_KEY, JSON.stringify(snap));
            cb(snap);
          } else {
            // KMP says null — only honor it if there's no localStorage snap.
            // Otherwise this is the "DB just initialised, nothing cached
            // yet" case after a page reload, and the LS snapshot is the
            // real source of truth until the user explicitly logs out.
            const fallback = this.readUserSnapshotFromLs();
            if (!fallback) cb(null);
          }
        });
      });
      return () => {
        cancelled = true;
        cancel();
      };
    },
  };

  readonly events = {
    subscribeAll: (cb: (events: readonly MensaWebEvent[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinEvent>>(
        () => this.initialize(),
        () => (inner) => this.events_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotEvent)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.events_.refresh();
    },
    getById: async (id: string): Promise<MensaWebEvent | null> => {
      await this.initialize();
      const e = await this.events_.getById(id);
      return e ? snapshotEvent(e) : null;
    },
    create: async (input: EventCreateInput): Promise<MensaWebEvent> => {
      await this.initialize();
      const payload = new KotlinEventCreatePayload(
        input.name,
        input.description ?? "",
        input.image ?? "",
        input.infoLink ?? "",
        input.bookingLink ?? "",
        input.startsMs,
        input.endsMs,
        input.isNational ?? false,
        input.isOnline ?? false,
        input.isPublic ?? false,
        input.isSpot ?? false,
        input.contact ?? "",
        input.region ?? "",
        input.positionId ?? null,
        input.ownerId,
      );
      const e = await this.events_.create(payload);
      return snapshotEvent(e);
    },
    update: async (id: string, input: EventUpdateInput): Promise<MensaWebEvent> => {
      await this.initialize();
      const payload = new KotlinEventUpdatePayload(
        input.name,
        input.description ?? "",
        input.image ?? "",
        input.infoLink ?? "",
        input.bookingLink ?? "",
        input.startsMs,
        input.endsMs,
        input.isNational ?? false,
        input.isOnline ?? false,
        input.isPublic ?? false,
        input.isSpot ?? false,
        input.contact ?? "",
        input.region ?? "",
        input.positionId ?? null,
        input.ownerId,
      );
      const e = await this.events_.update(id, payload);
      return snapshotEvent(e);
    },
    /**
     * Creates an event with a browser File as cover image via multipart/form-data.
     * The payload shape is the same as `create`; the File is read to bytes on the
     * Kotlin side via suspendCoroutine + file.arrayBuffer().
     */
    createMultipart: async (input: EventCreateInput, coverFile: File): Promise<MensaWebEvent> => {
      await this.initialize();
      const payload = new KotlinEventCreatePayload(
        input.name,
        input.description ?? "",
        input.image ?? "",
        input.infoLink ?? "",
        input.bookingLink ?? "",
        input.startsMs,
        input.endsMs,
        input.isNational ?? false,
        input.isOnline ?? false,
        input.isPublic ?? false,
        input.isSpot ?? false,
        input.contact ?? "",
        input.region ?? "",
        input.positionId ?? null,
        input.ownerId,
      );
      const e = await this.events_.createMultipart(payload, coverFile);
      return snapshotEvent(e);
    },
    updateMultipart: async (id: string, input: EventUpdateInput, coverFile: File): Promise<MensaWebEvent> => {
      await this.initialize();
      const payload = new KotlinEventUpdatePayload(
        input.name,
        input.description ?? "",
        input.image ?? "",
        input.infoLink ?? "",
        input.bookingLink ?? "",
        input.startsMs,
        input.endsMs,
        input.isNational ?? false,
        input.isOnline ?? false,
        input.isPublic ?? false,
        input.isSpot ?? false,
        input.contact ?? "",
        input.region ?? "",
        input.positionId ?? null,
        input.ownerId,
      );
      const e = await this.events_.updateMultipart(id, payload, coverFile);
      return snapshotEvent(e);
    },
    delete: async (id: string): Promise<void> => {
      await this.initialize();
      return this.events_.delete(id);
    },
    schedules: {
      list: async (eventId: string): Promise<readonly MensaWebEventSchedule[]> => {
        await this.initialize();
        const arr = await this.events_.listSchedules(eventId);
        return arr.map(snapshotEventSchedule);
      },
      create: async (input: EventScheduleCreateInput): Promise<MensaWebEventSchedule> => {
        await this.initialize();
        const payload = new KotlinEventScheduleCreatePayload(
          input.title,
          input.eventId,
          input.description ?? "",
          input.startsMs,
          input.endsMs,
          input.maxExternalGuests ?? 0,
          input.price ?? 0,
          input.infoLink ?? "",
          input.isSubscriptable ?? false,
        );
        const s = await this.events_.createSchedule(payload);
        return snapshotEventSchedule(s);
      },
      update: async (id: string, input: EventScheduleUpdateInput): Promise<MensaWebEventSchedule> => {
        await this.initialize();
        const payload = new KotlinEventScheduleUpdatePayload(
          input.title,
          input.description ?? "",
          input.startsMs,
          input.endsMs,
          input.maxExternalGuests ?? 0,
          input.price ?? 0,
          input.infoLink ?? "",
          input.isSubscriptable ?? false,
        );
        const s = await this.events_.updateSchedule(id, payload);
        return snapshotEventSchedule(s);
      },
      delete: async (id: string): Promise<void> => {
        await this.initialize();
        return this.events_.deleteSchedule(id);
      },
    },
  };

  readonly deals = {
    subscribeAll: (cb: (deals: readonly MensaWebDeal[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinDeal>>(
        () => this.initialize(),
        () => (inner) => this.deals_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotDeal)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.deals_.refresh();
    },
    getById: async (id: string): Promise<MensaWebDeal | null> => {
      await this.initialize();
      const d = await this.deals_.getById(id);
      return d ? snapshotDeal(d) : null;
    },
    contacts: async (dealId: string): Promise<readonly MensaWebDealContact[]> => {
      await this.initialize();
      const arr = await this.deals_.contacts(dealId);
      return arr.map(snapshotDealContact);
    },
    create: async (input: DealCreateInput): Promise<MensaWebDeal> => {
      await this.initialize();
      const payload = new KotlinDealCreatePayload(
        input.name,
        input.commercialSector,
        input.details ?? "",
        input.who ?? "",
        input.howToGet ?? "",
        input.link ?? "",
        input.vatNumber ?? "",
        input.positionId ?? null,
        input.validFromMs ?? 0,
        input.validUntilMs ?? 0,
      );
      const d = await this.deals_.create(payload);
      return snapshotDeal(d);
    },
    update: async (id: string, input: DealUpdateInput): Promise<MensaWebDeal> => {
      await this.initialize();
      const payload = new KotlinDealUpdatePayload(
        input.name,
        input.commercialSector,
        input.details ?? "",
        input.who ?? "",
        input.howToGet ?? "",
        input.link ?? "",
        input.vatNumber ?? "",
        input.positionId ?? null,
        input.validFromMs ?? 0,
        input.validUntilMs ?? 0,
      );
      const d = await this.deals_.update(id, payload);
      return snapshotDeal(d);
    },
    /**
     * Creates a deal with a browser File as cover image via multipart/form-data.
     * Uses PocketBaseClient.createMultipart directly; the attachment PB field
     * is populated with the file bytes.
     */
    createMultipart: async (input: DealCreateInput, coverFile: File): Promise<MensaWebDeal> => {
      await this.initialize();
      const payload = new KotlinDealCreatePayload(
        input.name,
        input.commercialSector,
        input.details ?? "",
        input.who ?? "",
        input.howToGet ?? "",
        input.link ?? "",
        input.vatNumber ?? "",
        input.positionId ?? null,
        input.validFromMs ?? 0,
        input.validUntilMs ?? 0,
      );
      const d = await this.deals_.createMultipart(payload, coverFile);
      return snapshotDeal(d);
    },
    updateMultipart: async (id: string, input: DealUpdateInput, coverFile: File): Promise<MensaWebDeal> => {
      await this.initialize();
      const payload = new KotlinDealUpdatePayload(
        input.name,
        input.commercialSector,
        input.details ?? "",
        input.who ?? "",
        input.howToGet ?? "",
        input.link ?? "",
        input.vatNumber ?? "",
        input.positionId ?? null,
        input.validFromMs ?? 0,
        input.validUntilMs ?? 0,
      );
      const d = await this.deals_.updateMultipart(id, payload, coverFile);
      return snapshotDeal(d);
    },
    delete: async (id: string): Promise<void> => {
      await this.initialize();
      return this.deals_.delete(id);
    },
    createContact: async (input: DealContactInput): Promise<MensaWebDealContact> => {
      await this.initialize();
      const payload = new KotlinDealContactPayload(
        input.name,
        input.email,
        input.phone ?? "",
        input.note ?? "",
        input.dealId,
      );
      const c = await this.deals_.createContact(payload);
      return snapshotDealContact(c);
    },
    updateContact: async (id: string, input: DealContactInput): Promise<MensaWebDealContact> => {
      await this.initialize();
      const payload = new KotlinDealContactPayload(
        input.name,
        input.email,
        input.phone ?? "",
        input.note ?? "",
        input.dealId,
      );
      const c = await this.deals_.updateContact(id, payload);
      return snapshotDealContact(c);
    },
    deleteContact: async (id: string): Promise<void> => {
      await this.initialize();
      return this.deals_.deleteContact(id);
    },
  };

  readonly notifications = {
    subscribeAll: (cb: (n: readonly MensaWebNotification[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinNotification>>(
        () => this.initialize(),
        () => (inner) => this.notifications_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotNotification)),
      ),
    subscribeUnreadCount: (cb: (count: number) => void): (() => void) =>
      deferredSubscribe<number>(
        () => this.initialize(),
        () => (inner) => this.notifications_.subscribeUnreadCount(inner),
        cb,
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.notifications_.refresh();
    },
    markSeen: async (id: string): Promise<void> => {
      await this.initialize();
      return this.notifications_.markSeen(id);
    },
    markAllSeen: async (): Promise<void> => {
      await this.initialize();
      return this.notifications_.markAllSeen();
    },
    delete: async (id: string): Promise<void> => {
      await this.initialize();
      return this.notifications_.delete(id);
    },
  };

  readonly regSoci = {
    subscribeAll: (cb: (members: readonly MensaWebMember[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinMember>>(
        () => this.initialize(),
        () => (inner) => this.regSoci_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotMember)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.regSoci_.refresh();
    },
    getById: async (id: string): Promise<MensaWebMember | null> => {
      await this.initialize();
      const m = await this.regSoci_.getById(id);
      return m ? snapshotMember(m) : null;
    },
    searchByName: async (query: string): Promise<readonly MensaWebMember[]> => {
      await this.initialize();
      const arr = await this.regSoci_.searchByName(query);
      return arr.map(snapshotMember);
    },
  };

  readonly sigs = {
    subscribeAll: (cb: (sigs: readonly MensaWebSig[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinSig>>(
        () => this.initialize(),
        () => (inner) => this.sigs_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotSig)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.sigs_.refresh();
    },
    getById: async (id: string): Promise<MensaWebSig | null> => {
      await this.initialize();
      const s = await this.sigs_.getById(id);
      return s ? snapshotSig(s) : null;
    },
    create: async (input: SigCreateInput): Promise<MensaWebSig> => {
      await this.initialize();
      const payload = new KotlinSigCreatePayload(
        input.name,
        input.link,
        input.groupType,
        input.description ?? "",
        input.image ?? "",
      );
      const s = await this.sigs_.create(payload);
      return snapshotSig(s);
    },
    update: async (id: string, input: SigUpdateInput): Promise<MensaWebSig> => {
      await this.initialize();
      const payload = new KotlinSigUpdatePayload(
        input.name,
        input.link,
        input.groupType,
        input.description ?? "",
        input.image ?? "",
      );
      const s = await this.sigs_.update(id, payload);
      return snapshotSig(s);
    },
    delete: async (id: string): Promise<void> => {
      await this.initialize();
      return this.sigs_.delete(id);
    },
  };

  readonly tickets = {
    subscribeAll: (cb: (tickets: readonly MensaWebTicket[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinTicket>>(
        () => this.initialize(),
        () => (inner) => this.tickets_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotTicket)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.tickets_.refresh();
    },
    getById: async (id: string): Promise<MensaWebTicket | null> => {
      await this.initialize();
      const t = await this.tickets_.getById(id);
      return t ? snapshotTicket(t) : null;
    },
  };

  readonly receipts = {
    subscribeAll: (cb: (receipts: readonly MensaWebReceipt[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinReceipt>>(
        () => this.initialize(),
        () => (inner) => this.receipts_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotReceipt)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.receipts_.refresh();
    },
    getById: async (id: string): Promise<MensaWebReceipt | null> => {
      await this.initialize();
      const r = await this.receipts_.getById(id);
      return r ? snapshotReceipt(r) : null;
    },
    pdfUrl: async (id: string): Promise<string | null> => {
      await this.initialize();
      // Kotlin's `String?` lands as `string | null | undefined` in TS;
      // normalise undefined → null at the boundary so callers get a clean union.
      const url = await this.receipts_.pdfUrl(id);
      return url ?? null;
    },
  };

  readonly localOffices = {
    subscribeAll: (cb: (offices: readonly MensaWebLocalOffice[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinLocalOffice>>(
        () => this.initialize(),
        () => (inner) => this.localOffices_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotLocalOffice)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.localOffices_.refresh();
    },
    bySlug: async (slug: string): Promise<MensaWebLocalOffice | null> => {
      await this.initialize();
      const o = await this.localOffices_.bySlug(slug);
      return o ? snapshotLocalOffice(o) : null;
    },
    subscribeTeam: (
      officeId: string,
      cb: (team: readonly MensaWebLocalOfficeMember[]) => void,
    ): (() => void) =>
      deferredSubscribe<Array<KotlinLocalOfficeMember>>(
        () => this.initialize(),
        () => (inner) => this.localOffices_.subscribeTeam(officeId, inner),
        (arr) => cb(arr.map(snapshotLocalOfficeMember)),
      ),
    subscribeLinktree: (
      officeId: string,
      cb: (rows: readonly MensaWebLocalOfficeLink[]) => void,
    ): (() => void) =>
      deferredSubscribe<Array<KotlinLocalOfficeLink>>(
        () => this.initialize(),
        () => (inner) => this.localOffices_.subscribeLinktree(officeId, inner),
        (arr) => cb(arr.map(snapshotLocalOfficeLink)),
      ),
    createLink: async (input: LocalOfficeLinkCreateInput): Promise<MensaWebLocalOfficeLink> => {
      await this.initialize();
      const payload = new KotlinLocalOfficeLinkCreatePayload(
        input.officeId,
        input.kind,
        input.parentId ?? "",
        input.title,
        input.url ?? "",
        input.icon ?? "",
        input.sortOrder ?? 0,
      );
      const l = await this.localOffices_.createLink(payload);
      return snapshotLocalOfficeLink(l);
    },
    updateLink: async (id: string, input: LocalOfficeLinkUpdateInput): Promise<MensaWebLocalOfficeLink> => {
      await this.initialize();
      const payload = new KotlinLocalOfficeLinkUpdatePayload(
        input.kind,
        input.parentId ?? "",
        input.title,
        input.url ?? "",
        input.icon ?? "",
        input.sortOrder ?? 0,
      );
      const l = await this.localOffices_.updateLink(id, payload);
      return snapshotLocalOfficeLink(l);
    },
    deleteLink: async (id: string): Promise<void> => {
      await this.initialize();
      return this.localOffices_.deleteLink(id);
    },
    upcomingTestDates: async (officeId: string): Promise<readonly MensaWebTestDate[]> => {
      await this.initialize();
      const arr = await this.localOffices_.upcomingTestDates(officeId);
      return arr.map(snapshotTestDate);
    },
    assistants: async (officeId: string): Promise<readonly MensaWebLocalOfficeMember[]> => {
      await this.initialize();
      const arr = await this.localOffices_.assistants(officeId);
      return arr.map(snapshotLocalOfficeMember);
    },
    createTestDate: async (input: LocalOfficeTestDateCreateInput): Promise<MensaWebTestDate> => {
      await this.initialize();
      const payload = new KotlinLocalOfficeTestDateCreatePayload(
        input.officeId,
        input.dateMs,
        input.location,
        input.notes ?? "",
        input.maxParticipants ?? 0,
        input.assistants ? [...input.assistants] : [],
      );
      const t = await this.localOffices_.createTestDate(payload);
      return snapshotTestDate(t);
    },
    updateTestDate: async (id: string, input: LocalOfficeTestDateUpdateInput): Promise<MensaWebTestDate> => {
      await this.initialize();
      const payload = new KotlinLocalOfficeTestDateUpdatePayload(
        input.dateMs,
        input.location,
        input.notes ?? "",
        input.maxParticipants ?? 0,
        input.assistants ? [...input.assistants] : [],
      );
      const t = await this.localOffices_.updateTestDate(id, payload);
      return snapshotTestDate(t);
    },
    deleteTestDate: async (id: string): Promise<void> => {
      await this.initialize();
      return this.localOffices_.deleteTestDate(id);
    },
  };

  readonly quid = {
    subscribeIssues: (cb: (issues: readonly MensaWebQuidIssue[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinQuidIssue>>(
        () => this.initialize(),
        () => (inner) => this.quid_.subscribeIssues(inner),
        (arr) => cb(arr.map(snapshotQuidIssue)),
      ),
    refreshIssues: async (): Promise<void> => {
      await this.initialize();
      return this.quid_.refreshIssues();
    },
    articlesForIssue: async (issueId: number): Promise<readonly MensaWebQuidArticle[]> => {
      await this.initialize();
      const arr = await this.quid_.articlesForIssue(issueId);
      return arr.map(snapshotQuidArticle);
    },
    articleById: async (id: number): Promise<MensaWebQuidArticle | null> => {
      await this.initialize();
      const a = await this.quid_.articleById(id);
      return a ? snapshotQuidArticle(a) : null;
    },
  };

  readonly podcasts = {
    subscribePodcasts: (cb: (podcasts: readonly MensaWebPodcast[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinPodcast>>(
        () => this.initialize(),
        () => (inner) => this.podcasts_.subscribePodcasts(inner),
        (arr) => cb(arr.map(snapshotPodcast)),
      ),
    refreshPodcasts: async (): Promise<void> => {
      await this.initialize();
      return this.podcasts_.refreshPodcasts();
    },
    subscribeEpisodes: (
      podcastId: string,
      cb: (episodes: readonly MensaWebPodcastEpisode[]) => void,
    ): (() => void) =>
      deferredSubscribe<Array<KotlinPodcastEpisode>>(
        () => this.initialize(),
        () => (inner) => this.podcasts_.subscribeEpisodes(podcastId, inner),
        (arr) => cb(arr.map(snapshotPodcastEpisode)),
      ),
  };

  readonly documents = {
    subscribeAll: (cb: (docs: readonly MensaWebDocument[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinDocument>>(
        () => this.initialize(),
        () => (inner) => this.documents_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotDocument)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.documents_.refresh();
    },
    getById: async (id: string): Promise<MensaWebDocument | null> => {
      await this.initialize();
      const d = await this.documents_.getById(id);
      return d ? snapshotDocument(d) : null;
    },
    getElaborated: async (elaboratedId: string): Promise<MensaWebDocumentSummary | null> => {
      await this.initialize();
      const s = await this.documents_.getElaborated(elaboratedId);
      return s ? snapshotDocumentSummary(s) : null;
    },
  };

  readonly boutique = {
    subscribeAll: (cb: (products: readonly MensaWebBoutiqueProduct[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinBoutiqueProduct>>(
        () => this.initialize(),
        () => (inner) => this.boutique_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotBoutiqueProduct)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.boutique_.refresh();
    },
    getById: async (id: string): Promise<MensaWebBoutiqueProduct | null> => {
      await this.initialize();
      const p = await this.boutique_.getById(id);
      return p ? snapshotBoutiqueProduct(p) : null;
    },
  };

  readonly addons = {
    subscribeAll: (cb: (addons: readonly MensaWebAddon[]) => void): (() => void) =>
      deferredSubscribe<Array<KotlinAddon>>(
        () => this.initialize(),
        () => (inner) => this.addons_.subscribeAll(inner),
        (arr) => cb(arr.map(snapshotAddon)),
      ),
    refresh: async (): Promise<void> => {
      await this.initialize();
      return this.addons_.refresh();
    },
  };

  readonly devices = {
    list: async (): Promise<readonly MensaWebDevice[]> => {
      await this.initialize();
      const arr = await this.devices_.list();
      return arr.map(snapshotDevice);
    },
    delete: async (id: string): Promise<void> => {
      await this.initialize();
      return this.devices_.delete(id);
    },
  };

  readonly positions = {
    list: async (): Promise<readonly MensaWebPosition[]> => {
      await this.initialize();
      const arr = await this.positions_.list();
      return arr.map(snapshotPosition);
    },
    create: async (input: PositionCreateInput): Promise<MensaWebPosition> => {
      await this.initialize();
      const payload = new KotlinPositionCreatePayload(
        input.name,
        input.address,
        input.latitude,
        input.longitude,
        input.createdBy ?? "",
      );
      const p = await this.positions_.create(payload);
      return snapshotPosition(p);
    },
    delete: async (id: string): Promise<MensaWebPosition> => {
      await this.initialize();
      const p = await this.positions_.delete(id);
      return snapshotPosition(p);
    },
  };

  readonly search = {
    subscribeState: (
      cb: (state: SearchStateKind, hits: readonly MensaWebSearchHit[]) => void,
    ): (() => void) => {
      let cancel: () => void = () => {};
      let cancelled = false;
      this.initialize().then(() => {
        if (cancelled) return;
        cancel = this.search_.subscribeState((state, hits) => {
          cb(state as SearchStateKind, hits.map(snapshotSearchHit));
        });
      });
      return () => {
        cancelled = true;
        cancel();
      };
    },
    update: (query: string): void => {
      // Fire-and-forget: state-store updates can happen pre-init; the
      // collector inside Kotlin awaits initialize() before bridging to the API.
      this.initialize().then(() => this.search_.update(query));
    },
    clear: (): void => {
      this.initialize().then(() => this.search_.clear());
    },
  };

  /**
   * Per-user metadata key/value store (notification preferences, opt-ins, …).
   * Mirrors `koin.metadata` on iOS/Android: `refresh(userId)` hydrates the
   * cache from the backend, `get(key)` reads from the cache,
   * `set(userId, key, value)` upserts.
   */
  readonly metadata = {
    refresh: async (userId: string): Promise<Record<string, string>> => {
      await this.initialize();
      const out = (await this.metadata_.refresh(userId)) as Record<string, unknown>;
      const result: Record<string, string> = {};
      for (const k of Object.keys(out)) {
        const v = out[k];
        if (typeof v === "string") result[k] = v;
      }
      return result;
    },
    get: (key: string): string | null => {
      // Synchronous read against the in-memory cache populated by refresh().
      // Returns null before init / before first refresh.
      try {
        return this.metadata_.get(key) ?? null;
      } catch {
        return null;
      }
    },
    set: async (userId: string, key: string, value: string): Promise<void> => {
      await this.initialize();
      await this.metadata_.set(userId, key, value);
    },
  };
}

// ── Exported singleton + types ───────────────────────────────────────────────

export interface MensaApi {
  initialize(): Promise<void>;
  i18n: MensaWebSdkBridge["i18n"];
  auth: MensaWebSdkBridge["auth"];
  events: MensaWebSdkBridge["events"];
  deals: MensaWebSdkBridge["deals"];
  notifications: MensaWebSdkBridge["notifications"];
  regSoci: MensaWebSdkBridge["regSoci"];
  sigs: MensaWebSdkBridge["sigs"];
  tickets: MensaWebSdkBridge["tickets"];
  receipts: MensaWebSdkBridge["receipts"];
  localOffices: MensaWebSdkBridge["localOffices"];
  devices: MensaWebSdkBridge["devices"];
  positions: MensaWebSdkBridge["positions"];
  quid: MensaWebSdkBridge["quid"];
  podcasts: MensaWebSdkBridge["podcasts"];
  documents: MensaWebSdkBridge["documents"];
  boutique: MensaWebSdkBridge["boutique"];
  addons: MensaWebSdkBridge["addons"];
  search: MensaWebSdkBridge["search"];
  metadata: MensaWebSdkBridge["metadata"];
}

export const Mensa: MensaApi = new MensaWebSdkBridge();
