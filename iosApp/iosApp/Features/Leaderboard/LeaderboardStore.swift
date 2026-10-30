import Shared
import SwiftUI

/// The members' ranking, read once and shared by Today's row and the full
/// page: `view_leaderboard` through the shared `LeaderboardRepository`,
/// which keeps it per user.
@MainActor
@Observable
final class LeaderboardStore {
    static let shared = LeaderboardStore()

    private(set) var board: LeaderboardBoard?
    /// The regions' and secretaries' rankings, best first; nil until read.
    private(set) var regions: [RegionRow]?
    private(set) var secretaries: [SecretaryRow]?
    /// Set when the last read failed and nothing was read before.
    private(set) var failed = false
    private(set) var regionsFailed = false
    /// Whether the member asked to be left out of the members' ranking.
    private(set) var hidden = false
    private var subscriptions: [Closeable] = []

    private init() {
        subscriptions = [
            subscribeOptionalFlow(koin.leaderboard.observe() as Kotlinx_coroutines_coreFlow) { [weak self] (board: LeaderboardBoard?) in
                // Animated, so the Home row eases in when the ranking arrives.
                Task { @MainActor [weak self] in withAnimation(.easeOut(duration: 0.35)) { self?.board = board } }
            },
            subscribeOptionalFlow(koin.leaderboard.observeRegions() as Kotlinx_coroutines_coreFlow) { [weak self] (rows: NSArray?) in
                Task { @MainActor [weak self] in self?.regions = rows as? [RegionRow] }
            },
            subscribeOptionalFlow(koin.leaderboard.observeSecretaries() as Kotlinx_coroutines_coreFlow) { [weak self] (rows: NSArray?) in
                Task { @MainActor [weak self] in self?.secretaries = rows as? [SecretaryRow] }
            },
            subscribeFlow(koin.leaderboard.observeHidden() as Kotlinx_coroutines_coreFlow) { [weak self] (value: KotlinBoolean) in
                Task { @MainActor [weak self] in self?.hidden = value.boolValue }
            },
        ]
    }

    /// Rereads the member's metadata, so `hidden` is current; errors leave it as it was.
    func refreshHidden() async {
        try? await koin.leaderboard.refreshHidden()
    }

    /// Leaves or rejoins the members' ranking; false when the save failed.
    func setHidden(_ hidden: Bool) async -> Bool {
        do {
            try await koin.leaderboard.setHidden(hidden: hidden)
            return true
        } catch {
            return false
        }
    }

    func refresh() async {
        do {
            try await koin.leaderboard.refresh()
            failed = false
        } catch {
            failed = board == nil
        }
        do {
            try await koin.leaderboard.refreshRegions()
            regionsFailed = false
        } catch {
            regionsFailed = regions == nil
        }
    }
}

/// The podium's metal for a position: gold, silver, bronze; nil past third.
func podiumMetal(position: Int) -> Medal.Tier? {
    switch position {
    case 1: .gold
    case 2: .silver
    case 3: .bronze
    default: nil
    }
}

extension LeaderboardRow {
    /// The podium's metal for this row: gold, silver, bronze; nil past third.
    var podiumTier: Medal.Tier? { podiumMetal(position: Int(position)) }
}

/// A member's face: the directory photo, or the app's initials bubble when
/// the directory isn't cached.
struct RankAvatar: View {
    let row: LeaderboardRow
    let size: CGFloat

    var body: some View {
        if let member = row.member {
            MemberAvatar(member: member, size: size)
        } else {
            Text(row.initials)
                .font(.system(size: size * 0.4, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .frame(width: size, height: size)
                .background(AppTheme.brandGradient, in: Circle())
        }
    }
}
