/**
 * Shared hook for list pages that combine `subscribeAll(cb)` + `refresh()`.
 *
 * Problem this solves: every list page (events, deals, members, sigs, etc.)
 * suffered the same UX bug — the SQLDelight-backed `subscribe*` would emit
 * `[]` from an empty cache before the first `refresh()` completed, causing
 * an "empty state" message to flash for a fraction of a second before the
 * real data arrived. Users perceived it as "no events" when in fact data
 * was still in flight.
 *
 * The fix is to gate the empty-state copy on a `hasFetched` flag that flips
 * `true` only after the first refresh resolves. Until then, treat `[]` as
 * "still loading" and render the skeleton.
 *
 * Usage:
 *   const { items, hasFetched, refreshing, refresh } = useListLoader({
 *     subscribe: (cb) => Mensa.events.subscribeAll(cb),
 *     refresh: () => Mensa.events.refresh(),
 *   });
 *   const loading = items === null || (!hasFetched && items.length === 0);
 *   const empty = hasFetched && items !== null && items.length === 0;
 */
import { useEffect, useState } from "react";
import { Mensa } from "./mensa";

export interface UseListLoaderArgs<T> {
  /** Called after Mensa.initialize(); must return an unsubscribe thunk. */
  subscribe: (cb: (items: readonly T[]) => void) => () => void;
  /** Optional one-shot refresh fired right after subscribe. */
  refresh?: () => Promise<unknown>;
}

export interface UseListLoaderResult<T> {
  items: readonly T[] | null;
  /** True after the first refresh resolves (success or failure). */
  hasFetched: boolean;
  /** True while a manual refresh is in flight. */
  refreshing: boolean;
  /** Manual refresh; sets `refreshing` for the duration. */
  refresh: () => Promise<void>;
}

export function useListLoader<T>({
  subscribe,
  refresh: refreshFn,
}: UseListLoaderArgs<T>): UseListLoaderResult<T> {
  const [items, setItems] = useState<readonly T[] | null>(null);
  const [hasFetched, setHasFetched] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let cancel: () => void = () => {};
    (async () => {
      await Mensa.initialize();
      if (cancelled) return;
      cancel = subscribe((next) => setItems(next));
      if (refreshFn) {
        try {
          await refreshFn();
        } catch {
          // swallow — caller can show a refresh-failed toast if desired
        }
        if (!cancelled) setHasFetched(true);
      } else {
        if (!cancelled) setHasFetched(true);
      }
    })();
    return () => {
      cancelled = true;
      cancel();
    };
     
  }, []);

  async function manualRefresh() {
    if (!refreshFn) return;
    setRefreshing(true);
    try {
      await refreshFn();
    } catch {
      // swallow
    } finally {
      setRefreshing(false);
      setHasFetched(true);
    }
  }

  return { items, hasFetched, refreshing, refresh: manualRefresh };
}
