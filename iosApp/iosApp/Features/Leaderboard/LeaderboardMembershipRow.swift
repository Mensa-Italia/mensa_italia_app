import Shared
import SwiftUI

/// The ranking's row in Today's membership card, right under Tableport: the
/// member's place beside the chevron (in its metal on the podium), and how
/// far the next place is. Mirrors Android `LeaderboardMembershipRow`.
struct LeaderboardMembershipRow: View {
    /// Nil when the members' ranking is switched off and only regions or
    /// secretaries are on: the row then just opens the page.
    let board: LeaderboardBoard?

    private var detail: String? {
        guard let board else { return tr("leaderboard.home.regions", fallback: "Regioni e segretari") }
        if board.me != nil, let gap = board.pointsToNext?.int32Value, let next = board.nextPosition?.int32Value {
            return tr("leaderboard.home.to_next", fallback: "{points} punti al {position}° posto",
                      ["points": "\(gap)", "position": "\(next)"])
        }
        if board.me != nil {
            return tr("leaderboard.leading", fallback: "Sei in testa alla classifica")
        }
        return board.podium.first.map { tr("leaderboard.home.leader", fallback: "In testa {name}", ["name": $0.displayName]) }
    }

    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(Color.orange)
                    .frame(width: 30, height: 30)
                Image(systemName: "trophy.fill")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
            }
            VStack(alignment: .leading, spacing: 1) {
                Text(tr("leaderboard.home.section", fallback: "Classifica"))
                    .font(.body)
                    .foregroundStyle(.primary)
                if let detail {
                    Text(detail)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer()
            if let me = board?.me {
                HStack(spacing: 4) {
                    if let tier = me.podiumTier {
                        Image(systemName: "medal.fill").foregroundStyle(tier.metal.mid)
                    }
                    Text("\(me.position)°")
                        .font(.headline.monospacedDigit())
                        .foregroundStyle(.primary)
                }
            }
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .contentShape(Rectangle())
    }

}
