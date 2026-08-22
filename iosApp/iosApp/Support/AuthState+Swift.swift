import Shared

/// Swift-friendly projection of the Kotlin sealed interface AuthState.
///
/// In Swift, Kotlin sealed interfaces are exposed as a protocol (AuthState)
/// with concrete classes: AuthStateUnknown, AuthStateAnonymous, AuthStateAuthenticated.
/// Pattern match with `is` checks or switch statements.
extension AuthState {
    /// Convenience computed property returning a typed Swift enum.
    var swiftPhase: AuthPhase {
        switch self {
        case is AuthStateAuthenticated: return .authenticated
        case is AuthStateAnonymous: return .anonymous
        default: return .loading
        }
    }
}

enum AuthPhase {
    case loading
    case anonymous
    case authenticated
}

enum RootPhase: Equatable {
    case loading
    case anonymous
    case onboarding
    case main

    /// Tessera scaduta: l'unica cosa che si vede e' la pagina di rinnovo.
    /// Prima questo stato non esisteva e con la tessera scaduta si entrava
    /// nell'app come se niente fosse. La regola sta in `Membership` (:shared),
    /// cosi' Android e iOS chiudono la porta nello stesso momento.
    case membershipExpired
}
