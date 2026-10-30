import Shared
import SwiftUI

/// The first three on a podium under three spotlights: second, first, third,
/// each on a step of their metal that rises with a spring when the page
/// opens, third first and first last; then the members land on top. Tapping
/// one opens their profile. Mirrors Android `LeaderboardPodium`.
struct LeaderboardPodium: View {
    let rows: [LeaderboardRow]
    let onMember: (String) -> Void

    @State private var risen = [false, false, false]

    private static let stepHeights: [CGFloat] = [132, 100, 76]

    var body: some View {
        // Left to right: second, first, third.
        let places = [1, 0, 2].compactMap { index in rows.indices.contains(index) ? (index, rows[index]) : nil }
        HStack(alignment: .bottom, spacing: 10) {
            ForEach(places, id: \.0) { index, row in
                column(row: row, index: index)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(.top, 28)
        .background(alignment: .top) { spotlights }
        .onAppear {
            // Third, second, first.
            for (step, index) in [2, 1, 0].enumerated() {
                withAnimation(.spring(response: 0.75, dampingFraction: 0.58).delay(0.14 * Double(step))) {
                    risen[index] = true
                }
            }
        }
    }

    private func column(row: LeaderboardRow, index: Int) -> some View {
        let tier = row.podiumTier ?? .bronze
        let metal = tier.metal
        let avatar: CGFloat = index == 0 ? 84 : 64
        let stepHeight = Self.stepHeights[index]
        let name = row.isMe ? tr("leaderboard.you", fallback: "Tu") : row.displayName
        let points = tr("leaderboard.points", fallback: "{points} pt", ["points": "\(row.score)"])
        return Button {
            if row.member != nil, !row.isMe { onMember(row.id) }
        } label: {
            VStack(spacing: 0) {
                // The member lands on the step once it's up.
                VStack(spacing: 2) {
                    RankAvatar(row: row, size: avatar)
                        .padding(6)
                        .overlay(
                            Circle().strokeBorder(
                                AngularGradient(colors: [metal.hi, metal.mid, metal.lo, metal.mid, metal.hi], center: .center),
                                lineWidth: 3
                            )
                        )
                        .background(
                            Circle()
                                .fill(RadialGradient(colors: [metal.mid.opacity(0.55), .clear], center: .center,
                                                     startRadius: 0, endRadius: avatar * 0.85))
                                .frame(width: avatar * 1.7, height: avatar * 1.7)
                        )
                        .overlay(alignment: .bottomTrailing) {
                            MedalArtwork(tier: tier, symbol: "trophy.fill", size: index == 0 ? 30 : 24)
                                .offset(x: 2, y: 2)
                        }
                        .padding(.bottom, 6)
                    Text(name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(1)
                    Text(points)
                        .font(.caption.weight(.bold).monospacedDigit())
                        .foregroundStyle(metal.hi)
                }
                .opacity(risen[index] ? 1 : 0)
                .offset(y: risen[index] ? 0 : -28)
                .animation(.spring(response: 0.5, dampingFraction: 0.7).delay(0.14 * Double(2 - index) + 0.3),
                           value: risen[index])

                // The step, rising from the floor with a bounce.
                ZStack(alignment: .top) {
                    LinearGradient(colors: [metal.mid, metal.lo, Color(red: 0.043, green: 0.055, blue: 0.094)],
                                   startPoint: .top, endPoint: .bottom)
                    // Its rounded sides.
                    LinearGradient(colors: [.white.opacity(0.12), .clear, .black.opacity(0.25)],
                                   startPoint: .leading, endPoint: .trailing)
                    // The lit top face.
                    LinearGradient(colors: [metal.hi.opacity(0.9), .clear], startPoint: .top, endPoint: .bottom)
                        .frame(height: 10)
                    Text("\(row.position)")
                        .font(.system(size: stepHeight > 110 ? 64 : 48, weight: .black, design: .rounded))
                        .foregroundStyle(LinearGradient(colors: [.white.opacity(0.95), metal.hi.opacity(0.55)],
                                                        startPoint: .top, endPoint: .bottom))
                        .shadow(color: metal.lo, radius: 3, y: 2)
                        .padding(.top, 6)
                }
                .frame(height: stepHeight)
                .clipShape(UnevenRoundedRectangle(topLeadingRadius: 18, bottomLeadingRadius: 6,
                                                  bottomTrailingRadius: 6, topTrailingRadius: 18, style: .continuous))
                .scaleEffect(x: 1, y: risen[index] ? 1 : 0.001, anchor: .bottom)
                .padding(.top, 10)
            }
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(row.position)°, \(name), \(points)"))
    }

    /// Three cones of light from above, one on each step, the middle one
    /// brightest.
    private var spotlights: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height * 0.78
            ForEach([(0.5, 0), (0.165, 1), (0.835, 2)], id: \.1) { x, index in
                let tier: Medal.Tier = [.gold, .silver, .bronze][index]
                Path { path in
                    path.move(to: CGPoint(x: w * (x - 0.035), y: 0))
                    path.addLine(to: CGPoint(x: w * (x + 0.035), y: 0))
                    path.addLine(to: CGPoint(x: w * (x + 0.17), y: h))
                    path.addLine(to: CGPoint(x: w * (x - 0.17), y: h))
                    path.closeSubpath()
                }
                .fill(LinearGradient(colors: [tier.metal.hi.opacity(index == 0 ? 0.22 : 0.15), .clear],
                                     startPoint: .top, endPoint: .bottom))
                .opacity(risen[index] ? 1 : 0)
                .animation(.easeOut(duration: 0.6).delay(0.14 * Double(2 - index)), value: risen[index])
            }
        }
        .allowsHitTesting(false)
    }
}
