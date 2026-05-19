/**
 * Motion helpers — shared GSAP setup for the public site.
 *
 * Loaded lazily in client-side <script> blocks. Respects
 * `prefers-reduced-motion: reduce` by short-circuiting every helper to a
 * "set final state immediately" mode.
 *
 * Design language: editorial-minimal, in the same family as the rest of the
 * site. Defaults bias toward subtle, fast, and uniform rather than playful.
 */
import { gsap } from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";

gsap.registerPlugin(ScrollTrigger);

/** True when the user has asked for reduced motion. */
export function prefersReducedMotion(): boolean {
  if (typeof window === "undefined") return false;
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/** Editorial ease — soft entry, no bounce. */
export const EASE_EDITORIAL = "power3.out";
export const EASE_EDITORIAL_SOFT = "power2.out";
export const EASE_EDITORIAL_IN = "power2.in";

/**
 * Reveal elements on scroll with a soft fade + translateY.
 *
 * Use a CSS class (default `.reveal`) to mark targets. Within a parent
 * marked `.reveal-group`, children stagger 80ms apart.
 */
export function setupScrollReveal(opts?: {
  selector?: string;
  groupSelector?: string;
  y?: number;
  duration?: number;
  stagger?: number;
}) {
  if (typeof window === "undefined") return;
  const selector = opts?.selector ?? ".reveal";
  const groupSelector = opts?.groupSelector ?? ".reveal-group";
  const y = opts?.y ?? 20;
  const duration = opts?.duration ?? 0.7;
  const stagger = opts?.stagger ?? 0.08;

  if (prefersReducedMotion()) {
    // Show everything immediately — no motion.
    gsap.set(selector, { opacity: 1, y: 0 });
    gsap.set(`${groupSelector} > *`, { opacity: 1, y: 0 });
    return;
  }

  // Pre-hide so elements don't flash visible before ScrollTrigger fires.
  // gsap.from() handles the FROM state at trigger time, but until then the
  // element is at its natural CSS opacity:1, which causes a brief flicker.
  gsap.utils.toArray<HTMLElement>(selector).forEach((el) => {
    if (!el.closest(groupSelector)) gsap.set(el, { opacity: 0, y });
  });
  gsap.utils.toArray<HTMLElement>(`${groupSelector} > *`).forEach((el) => {
    gsap.set(el, { opacity: 0, y });
  });

  // Standalone reveals — animate TO natural state from the pre-hidden state.
  gsap.utils.toArray<HTMLElement>(selector).forEach((el) => {
    if (el.closest(groupSelector)) return; // handled by group below
    gsap.to(el, {
      opacity: 1,
      y: 0,
      duration,
      ease: EASE_EDITORIAL,
      scrollTrigger: {
        trigger: el,
        start: "top 85%",
        toggleActions: "play none none none",
      },
    });
  });

  // Group staggered reveals
  gsap.utils.toArray<HTMLElement>(groupSelector).forEach((group) => {
    const children = Array.from(group.children) as HTMLElement[];
    if (!children.length) return;
    gsap.to(children, {
      opacity: 1,
      y: 0,
      duration,
      ease: EASE_EDITORIAL,
      stagger,
      scrollTrigger: {
        trigger: group,
        start: "top 85%",
        toggleActions: "play none none none",
      },
    });
  });
}

/**
 * Animate a numeric count-up when the element scrolls into view.
 *
 * The element's text is replaced with the formatted number on each tick.
 * Pass `format` to control thousands separators (default it-IT locale).
 */
export function countUp(
  el: HTMLElement,
  target: number,
  opts?: {
    duration?: number;
    locale?: string;
    suffix?: string;
    prefix?: string;
    start?: number;
  }
) {
  if (typeof window === "undefined" || !el) return;
  const duration = opts?.duration ?? 1.6;
  const locale = opts?.locale ?? "it-IT";
  const suffix = opts?.suffix ?? "";
  const prefix = opts?.prefix ?? "";
  const start = opts?.start ?? 0;
  const formatter = new Intl.NumberFormat(locale);

  if (prefersReducedMotion()) {
    el.textContent = prefix + formatter.format(target) + suffix;
    return;
  }

  const state = { v: start };
  el.textContent = prefix + formatter.format(start) + suffix;

  gsap.to(state, {
    v: target,
    duration,
    ease: "power2.out",
    onUpdate: () => {
      el.textContent = prefix + formatter.format(Math.round(state.v)) + suffix;
    },
    scrollTrigger: {
      trigger: el,
      start: "top 85%",
      toggleActions: "play none none none",
    },
  });
}

/** Re-export gsap + ScrollTrigger so pages can author bespoke timelines. */
export { gsap, ScrollTrigger };
