import Shared
import SwiftUI

/// The hall of fame (albo d'oro): the closed seasons, latest first, each with
/// the first three of the rankings switched on and, apart, the member's own
/// place. Same sheet style as the ⓘ (grouped list over the platform's glass).
/// Mirrors Android `LeaderboardHallSheet`.
struct LeaderboardHallSheet: View {
    /// The rankings switched on: only their podiums show.
    let kinds: [LeaderboardKind]

    @State private var store = LeaderboardStore.shared
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if let hall = store.hall, !hall.isEmpty {
                    List {
                        ForEach(hall.seasons, id: \.season) { season in
                            seasonSection(season)
                        }
                    }
                } else {
                    ContentUnavailableView(
                        tr("leaderboard.hall.title", fallback: "Albo d'oro"),
                        systemImage: "trophy",
                        description: Text(tr("leaderboard.hall.empty", fallback: "La prima stagione si chiude il 30 settembre."))
                    )
                }
            }
            .navigationTitle(tr("leaderboard.hall.title", fallback: "Albo d'oro"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Label(tr("app.close", fallback: "Chiudi"), systemImage: "xmark")
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .preferredColorScheme(.dark)
    }

    private func title(_ kind: LeaderboardKind) -> String {
        if kind == .members { return tr("leaderboard.tab.members", fallback: "Soci") }
        if kind == .regions { return tr("leaderboard.tab.regions", fallback: "Regioni") }
        return tr("leaderboard.tab.secretaries", fallback: "Segretari")
    }

    /// One closed season: its label as the header, then each enabled board's podium.
    @ViewBuilder
    private func seasonSection(_ season: HallSeason) -> some View {
        let boards = kinds.map { season.board(kind: $0) }.filter { !$0.isEmpty }
        Section {
            ForEach(boards, id: \.kind) { board in
                Text(title(board.kind).uppercased())
                    .font(.caption2.weight(.semibold))
                    .tracking(2)
                    .foregroundStyle(.secondary)
                    .listRowSeparator(.hidden, edges: .bottom)
                ForEach(board.podium, id: \.id) { HallRow(entry: $0, kind: board.kind) }
                // Below the podium only the member's own place, after a gap that says places are skipped.
                if let me = board.me {
                    Image(systemName: "ellipsis")
                        .foregroundStyle(.tertiary)
                        .frame(maxWidth: .infinity)
                    Text(tr("leaderboard.hall.you", fallback: "Il tuo posto"))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .listRowSeparator(.hidden, edges: .bottom)
                    HallRow(entry: me, kind: board.kind)
                }
            }
        } header: {
            Text(season.label)
        }
    }
}

/// A place of a closed season: the podium's metal or the number, a face for a
/// person, the name and the points. The member's own row says whose it is.
private struct HallRow: View {
    let entry: HallEntry
    let kind: LeaderboardKind

    var body: some View {
        HStack(spacing: 12) {
            place
            if kind != .regions { avatar }
            VStack(alignment: .leading, spacing: 1) {
                HStack(spacing: 6) {
                    Text(entry.displayName).lineLimit(1)
                    if entry.isMe {
                        Text(tr("leaderboard.you", fallback: "Tu"))
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.tint)
                    }
                }
            }
            Spacer()
            Text(tr("leaderboard.points", fallback: "{points} pt", ["points": "\(entry.score)"]))
                .font(.subheadline.weight(.semibold).monospacedDigit())
        }
        .accessibilityElement(children: .combine)
    }

    private var place: some View {
        Group {
            if let tier = podiumMetal(position: Int(entry.position)) {
                Image(systemName: "medal.fill")
                    .font(.title3)
                    .foregroundStyle(tier.metal.mid)
            } else {
                Text("\(entry.position)°")
                    .font(.body.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 34)
    }

    @ViewBuilder private var avatar: some View {
        if let member = entry.member {
            MemberAvatar(member: member, size: 36)
        } else {
            Text(entry.initials)
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundStyle(.white)
                .frame(width: 36, height: 36)
                .background(AppTheme.brandGradient, in: Circle())
        }
    }
}
