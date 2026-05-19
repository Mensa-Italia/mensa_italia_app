import Foundation
import Shared

struct OnboardingPage {
    let icon: String
    let title: String
    let subtitle: String
}

@MainActor @Observable
final class OnboardingViewModel {
    var currentPage: Int = 0

    // Strings are resolved at VM construction via `tr(...)` literal calls so
    // the tolgee-push extractor picks up the keys + fallbacks.
    let pages: [OnboardingPage] = [
        OnboardingPage(
            icon: "sparkles",
            title: tr("onboarding.welcome.title", fallback: "Benvenuto in Mensa"),
            subtitle: tr("onboarding.welcome.subtitle", fallback: "Sei entrato a far parte del 2% più brillante. Ti aiutiamo a sfruttare al meglio la community.")
        ),
        OnboardingPage(
            icon: "calendar.badge.plus",
            title: tr("onboarding.events.title", fallback: "Eventi in tutta Italia"),
            subtitle: tr("onboarding.events.subtitle", fallback: "Trova cene, conferenze e incontri vicino a te, anche quelli nazionali. Iscriviti con un tap e aggiungili al calendario.")
        ),
        OnboardingPage(
            icon: "person.text.rectangle.fill",
            title: tr("onboarding.card.title", fallback: "La tua tessera, sempre con te"),
            subtitle: tr("onboarding.card.subtitle", fallback: "Mostrala al coordinatore quando partecipi a un evento. Niente più tessere di plastica da cercare nel portafoglio.")
        ),
        OnboardingPage(
            icon: "magnifyingglass",
            title: tr("onboarding.search.title", fallback: "Una ricerca per tutto"),
            subtitle: tr("onboarding.search.subtitle", fallback: "Persone, eventi, convenzioni, documenti. Un solo posto per trovare quello che ti serve.")
        )
    ]

    var isLastPage: Bool { currentPage == pages.count - 1 }

    private let onComplete: () -> Void

    init(initialPage: Int = 0, onComplete: @escaping () -> Void) {
        self.currentPage = initialPage
        self.onComplete = onComplete
    }

    func next() {
        guard currentPage < pages.count - 1 else { return }
        currentPage += 1
    }

    func complete() {
        // In DEBUG we deliberately DON'T persist the "shown" marker, so every
        // future login re-triggers the onboarding (paired with the
        // evaluatePhase() #if DEBUG branch in RootViewModel). The user still
        // enters the app normally on tap.
        #if !DEBUG
        if let uid = koin.auth.currentUser.value as? UserModel {
            koin.onboarding.markShown(userId: uid.id)
        }
        #endif
        onComplete()
    }
}
