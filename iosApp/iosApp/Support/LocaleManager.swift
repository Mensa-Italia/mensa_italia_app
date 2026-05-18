import Foundation
import SwiftUI
import Shared

/// Single source of truth for the in-app language.
///
/// The user can override the device locale (System Settings → Language) with
/// a per-app choice persisted in `UserDefaults`. The override is the IETF
/// language tag the i18n loader feeds to Tolgee (`it`, `en`, `fr`, etc.) — or
/// `nil` meaning "follow the system".
///
/// `version` is bumped on every catalog refresh so SwiftUI consumers can
/// `.id(localeManager.version)` a high-level view and force a full re-render
/// when the language changes (cheaper than threading bindings through every
/// `tr()` call site).
@MainActor
final class LocaleManager: ObservableObject {
    static let shared = LocaleManager()

    private static let overrideKey = "app.locale.override"

    /// Bumped each time `koin.i18n.ready` re-emits (catalog reload after
    /// `bootstrap`). Attach `.id(localeManager.version)` somewhere near the
    /// root to force every `tr()` lookup to re-evaluate.
    @Published private(set) var version: Int = 0

    /// Locales advertised by the backend (`languages` config). Updated live.
    @Published private(set) var availableLocales: [String] = ["it"]

    /// Currently active language tag (override > device > "it").
    @Published private(set) var activeTag: String

    /// User override, persisted. `nil` = follow device locale.
    @Published private(set) var override: String?

    private var readySub: Closeable?
    private var localesSub: Closeable?

    private init() {
        let stored = UserDefaults.standard.string(forKey: Self.overrideKey)
        self.override = stored
        self.activeTag = Self.resolveTag(override: stored)
    }

    /// Tag the i18n loader should be fed at boot (or after an override change).
    var preferredTag: String { activeTag }

    /// Hook this once after the first `koin.i18n.bootstrap` call (RootView).
    func startObservingCatalog() {
        readySub?.close()
        let readyFlow = koin.i18n.ready as Kotlinx_coroutines_coreFlow
        // We only care about the *event* (catalog reloaded), not the payload —
        // type the callback as the most permissive `AnyObject?` so we don't
        // depend on the exact Kotlin → Swift bridged name of `TranslationLoader.Ready`.
        readySub = subscribeOptionalFlow(readyFlow) { [weak self] (_: AnyObject?) in
            Task { @MainActor [weak self] in
                self?.version &+= 1
            }
        } onError: { _ in }

        localesSub?.close()
        let localesFlow = koin.i18n.availableLocales as Kotlinx_coroutines_coreFlow
        localesSub = subscribeFlow(localesFlow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                let arr = (list as? [String]) ?? []
                self?.availableLocales = arr.isEmpty ? ["it"] : arr
            }
        } onError: { _ in }
    }

    func stopObservingCatalog() {
        readySub?.close(); readySub = nil
        localesSub?.close(); localesSub = nil
    }

    /// Switch to a specific language. Pass `nil` to clear the override and
    /// follow the device locale. Re-bootstraps the i18n loader; on success
    /// the `ready` flow emits and `version` increments → views re-render.
    func setOverride(_ tag: String?) async {
        if let tag, tag.isEmpty { return }
        override = tag
        if let tag {
            UserDefaults.standard.set(tag, forKey: Self.overrideKey)
        } else {
            UserDefaults.standard.removeObject(forKey: Self.overrideKey)
        }
        let resolved = Self.resolveTag(override: tag)
        activeTag = resolved
        try? await koin.i18n.bootstrap(preferred: resolved)
        // Defensive bump in case the StateFlow doesn't re-emit (same Ready ref).
        version &+= 1
    }

    /// Display name for a language tag, in the user's CURRENT active locale
    /// (not in the language tag itself). E.g. "Inglese" when active is `it`,
    /// "English" when active is `en`.
    func displayName(for tag: String) -> String {
        let target = Locale(identifier: tag)
        let display = Locale(identifier: activeTag)
        return display.localizedString(forIdentifier: target.identifier)
            ?? target.identifier.capitalized
    }

    /// Display name in the language's OWN tongue (used as a quiet subtitle:
    /// "English", "Français", "Italiano"). Helps the user recognise their
    /// language even when the active locale is one they don't read.
    func nativeName(for tag: String) -> String {
        let target = Locale(identifier: tag)
        return target.localizedString(forIdentifier: target.identifier)
            ?? target.identifier.capitalized
    }

    private static func resolveTag(override: String?) -> String {
        if let override, !override.isEmpty { return override }
        // `Locale.current.identifier` reflects the formatting locale (date/
        // number formats) — NOT the user's preferred app language. A user
        // with Italian as their only language but US region returns `it_US`
        // — but if iOS falls back to `en_…` for the formatting locale we'd
        // wrongly serve English. The correct source is
        // `Locale.preferredLanguages`, which is the ordered language list
        // from Settings → General → Language & Region (BCP-47, e.g. `it-IT`).
        let preferred = Locale.preferredLanguages.first ?? "it"
        // Strip region/script suffix: `it-IT` → `it`, `zh-Hans-CN` → `zh`.
        return String(preferred.split(separator: "-").first ?? "it")
    }
}
