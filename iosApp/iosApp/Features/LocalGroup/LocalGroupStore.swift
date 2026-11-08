import Shared
import SwiftUI

/// The "Gruppo locale" tab's state, read once and shared by the tab bar
/// (whether the tab exists) and the tab's pages: the shared
/// `LocalGroupRepository`, which keeps the last snapshot per user.
@MainActor
@Observable
final class LocalGroupStore {
    static let shared = LocalGroupStore()

    private(set) var state: LocalGroupState = LocalGroupStateLoading.shared
    /// The `local_group_enabled` flag is on and a member is signed in (or the fixtures are on).
    private(set) var available = false
    /// True once `available` holds the repository's answer, not just the initial false.
    private(set) var availabilityKnown = false
    private var subscriptions: [Closeable] = []

    private init() {
        subscriptions = [
            // `LocalGroupState` is a protocol in Swift (a Kotlin sealed
            // interface), so the flow is read as NSObject and cast.
            subscribeFlow(koin.localGroup.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (value: NSObject) in
                guard let state = value as? LocalGroupState else { return }
                Task { @MainActor [weak self] in self?.state = state }
            },
            subscribeFlow(koin.localGroup.observeAvailable() as Kotlinx_coroutines_coreFlow) { [weak self] (value: KotlinBoolean) in
                Task { @MainActor [weak self] in
                    self?.available = value.boolValue
                    self?.availabilityKnown = true
                }
            },
        ]
    }

    /// The snapshot to show: the ready one, or the cached one under an error.
    var snapshot: LocalGroupSnapshot? {
        if let ready = state as? LocalGroupStateReady { return ready.snapshot }
        if let failed = state as? LocalGroupStateFailed { return failed.cached }
        return nil
    }

    /// Nothing to show yet: still loading, or failed with no snapshot saved.
    var needsInitialLoad: Bool {
        if state is LocalGroupStateLoading { return true }
        if let failed = state as? LocalGroupStateFailed { return failed.cached == nil }
        return false
    }

    /// Rereads everything. Network errors end up in `state`, never here.
    func refresh() async {
        try? await koin.localGroup.refresh()
    }
}

/// Shared helpers of the "Gruppo locale" pages.
enum LocalGroupFormat {
    /// "2026-11-03" -> "03/11/2026"; the raw text when it isn't a date.
    static func day(_ iso: String) -> String {
        let parts = iso.prefix(10).split(separator: "-")
        guard parts.count == 3 else { return iso }
        return "\(parts[2])/\(parts[1])/\(parts[0])"
    }

    /// The label of a member's movement.
    static func changeLabel(_ change: MemberChangeModel) -> String {
        switch change.changeKind {
        case MemberChangeKind.joined: tr("local_group.change.joined", fallback: "Nuovo socio")
        case MemberChangeKind.movedIn: tr("local_group.change.moved_in", fallback: "Arrivato da altra regione")
        case MemberChangeKind.left: tr("local_group.change.left", fallback: "Non rinnovato")
        case MemberChangeKind.movedOut: tr("local_group.change.moved_out", fallback: "Trasferito in altra regione")
        default: change.kind
        }
    }
}
