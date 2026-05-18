/**
 * AudioPlayerService — Singleton audio engine for Quid narrations and Podcasts.
 *
 * Usage:
 *   import { AudioPlayer } from "./AudioPlayerService";
 *   AudioPlayer.playSingle(track);
 *   AudioPlayer.subscribe(state => console.log(state));
 *
 * SSR caveat: the HTMLAudioElement is lazily constructed on the first play*()
 * call, so this file is safe to import in Astro server-rendered pages.
 *
 * View-transitions caveat: Astro's ClientRouter re-evaluates module-level code
 * on every soft navigation when the module is imported in a new page. Because
 * this module exports a singleton constructed at module evaluation time the
 * same instance persists across navigations in the same session (the JS module
 * cache is shared). If the user navigates to a page that doesn't import this
 * module the HTMLAudioElement event listeners are kept alive by the singleton
 * reference. Playback continues. This is intentional; a full hard-navigate
 * (Ctrl+R) would reset state, which is acceptable.
 */

export type AudioTrack = {
  id: string;
  title: string;
  subtitle: string;
  artworkUrl?: string;
  audioUrl: string;
  durationSec: number;
  originDeepLink?: string;
};

export interface AudioPlayerState {
  currentTrack: AudioTrack | null;
  isPlaying: boolean;
  progressSec: number;
  durationSec: number;
  queue: readonly AudioTrack[];
  currentIndex: number;
  isPresentingFullPlayer: boolean;
}

type Listener = (state: AudioPlayerState) => void;

class AudioPlayerService {
  private audio: HTMLAudioElement | null = null;
  private state: AudioPlayerState = {
    currentTrack: null,
    isPlaying: false,
    progressSec: 0,
    durationSec: 0,
    queue: [],
    currentIndex: -1,
    isPresentingFullPlayer: false,
  };
  private listeners = new Set<Listener>();

  // ── Subscription ──────────────────────────────────────────────────────────

  subscribe(cb: Listener): () => void {
    this.listeners.add(cb);
    // Immediately emit current state to new subscriber
    cb({ ...this.state });
    return () => this.listeners.delete(cb);
  }

  getState(): AudioPlayerState {
    return { ...this.state };
  }

  private emit() {
    const snap = { ...this.state };
    for (const cb of this.listeners) {
      cb(snap);
    }
  }

  // ── Element bootstrap ─────────────────────────────────────────────────────

  private ensureAudio(): HTMLAudioElement {
    if (this.audio) return this.audio;

    const audio = new Audio();
    audio.preload = "metadata";

    audio.addEventListener("timeupdate", () => {
      this.state = { ...this.state, progressSec: audio.currentTime };
      this.emit();
    });

    audio.addEventListener("loadedmetadata", () => {
      this.state = { ...this.state, durationSec: audio.duration };
      this.emit();
    });

    audio.addEventListener("ended", () => {
      if (this.hasNext()) {
        this.skipToNext();
      } else {
        this.state = { ...this.state, isPlaying: false, progressSec: 0 };
        this.emit();
      }
    });

    audio.addEventListener("error", () => {
      this.state = { ...this.state, isPlaying: false };
      this.emit();
    });

    audio.addEventListener("play", () => {
      this.state = { ...this.state, isPlaying: true };
      this.emit();
    });

    audio.addEventListener("pause", () => {
      this.state = { ...this.state, isPlaying: false };
      this.emit();
    });

    this.audio = audio;
    return audio;
  }

  // ── Media Session ─────────────────────────────────────────────────────────

  private setupMediaSession(track: AudioTrack) {
    if (typeof navigator === "undefined" || !("mediaSession" in navigator)) return;

    navigator.mediaSession.metadata = new MediaMetadata({
      title: track.title,
      artist: track.subtitle,
      artwork: track.artworkUrl
        ? [{ src: track.artworkUrl, sizes: "512x512", type: "image/jpeg" }]
        : [],
    });

    navigator.mediaSession.setActionHandler("play", () => this.play());
    navigator.mediaSession.setActionHandler("pause", () => this.pause());
    navigator.mediaSession.setActionHandler("nexttrack", () => this.skipToNext());
    navigator.mediaSession.setActionHandler("previoustrack", () => this.skipToPrevious());
    navigator.mediaSession.setActionHandler("seekto", (details) => {
      if (details.seekTime !== undefined) {
        this.seek(details.seekTime);
      }
    });
    navigator.mediaSession.setActionHandler("seekforward", (_details) => {
      this.skipForward15();
    });
    navigator.mediaSession.setActionHandler("seekbackward", (_details) => {
      this.skipBackward15();
    });
  }

  // ── Internal load ─────────────────────────────────────────────────────────

  private loadTrack(track: AudioTrack) {
    const audio = this.ensureAudio();
    audio.src = track.audioUrl;
    audio.load();
    this.state = {
      ...this.state,
      currentTrack: track,
      progressSec: 0,
      durationSec: track.durationSec,
    };
    this.setupMediaSession(track);
  }

  // ── Public playback API ───────────────────────────────────────────────────

  playSingle(track: AudioTrack): void {
    this.state = { ...this.state, queue: [track], currentIndex: 0 };
    this.loadTrack(track);
    this.play();
  }

  playQueue(tracks: AudioTrack[], startIndex = 0): void {
    if (tracks.length === 0) return;
    const idx = Math.min(startIndex, tracks.length - 1);
    this.state = { ...this.state, queue: tracks, currentIndex: idx };
    this.loadTrack(tracks[idx]!);
    this.play();
  }

  enqueue(track: AudioTrack): void {
    const queue = [...this.state.queue, track];
    this.state = { ...this.state, queue };
    // If nothing is playing, start immediately
    if (!this.state.currentTrack) {
      this.playQueue(queue, 0);
    } else {
      this.emit();
    }
  }

  toggle(): void {
    if (this.state.isPlaying) {
      this.pause();
    } else {
      this.play();
    }
  }

  play(): void {
    const audio = this.ensureAudio();
    if (!audio.src && this.state.currentTrack) {
      audio.src = this.state.currentTrack.audioUrl;
    }
    audio.play().catch(() => {
      this.state = { ...this.state, isPlaying: false };
      this.emit();
    });
  }

  pause(): void {
    this.audio?.pause();
  }

  seek(seconds: number): void {
    const audio = this.ensureAudio();
    audio.currentTime = Math.max(0, Math.min(seconds, audio.duration || seconds));
    this.state = { ...this.state, progressSec: audio.currentTime };
    this.emit();
  }

  skipForward15(): void {
    this.seek(this.state.progressSec + 15);
  }

  skipBackward15(): void {
    this.seek(this.state.progressSec - 15);
  }

  skipToNext(): void {
    const { queue, currentIndex } = this.state;
    if (!this.hasNext()) return;
    const nextIdx = currentIndex + 1;
    const track = queue[nextIdx]!;
    this.state = { ...this.state, currentIndex: nextIdx };
    this.loadTrack(track);
    this.play();
  }

  skipToPrevious(): void {
    const { queue, currentIndex } = this.state;
    if (!this.hasPrevious()) return;
    const prevIdx = currentIndex - 1;
    const track = queue[prevIdx]!;
    this.state = { ...this.state, currentIndex: prevIdx };
    this.loadTrack(track);
    this.play();
  }

  hasNext(): boolean {
    return this.state.currentIndex < this.state.queue.length - 1;
  }

  hasPrevious(): boolean {
    return this.state.currentIndex > 0;
  }

  openFullPlayer(): void {
    this.state = { ...this.state, isPresentingFullPlayer: true };
    this.emit();
  }

  closeFullPlayer(): void {
    this.state = { ...this.state, isPresentingFullPlayer: false };
    this.emit();
  }
}

export const AudioPlayer: AudioPlayerService = new AudioPlayerService();
