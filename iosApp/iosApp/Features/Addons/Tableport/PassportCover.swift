import SwiftUI

/// Hardback passport cover — Mensa cobalt leather + embossed gold lettering.
/// Designed to be rotated around its leading edge via rotation3DEffect.
struct PassportCover: View {
    let width: CGFloat
    let height: CGFloat

    var body: some View {
        ZStack {
            // Leather base — radial gradient gives subtle "bulge" lighting.
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    RadialGradient(
                        colors: [
                            PassportPalette.coverHi,
                            PassportPalette.coverMid,
                            PassportPalette.coverDeep
                        ],
                        center: .init(x: 0.35, y: 0.25),
                        startRadius: 6,
                        endRadius: max(width, height) * 0.95
                    )
                )

            // Grain — diagonal hatch via Canvas for tactile leather feel.
            Canvas { ctx, size in
                ctx.opacity = 0.06
                let step: CGFloat = 3
                var y: CGFloat = -size.height
                while y < size.height * 2 {
                    var path = Path()
                    path.move(to: CGPoint(x: -10, y: y))
                    path.addLine(to: CGPoint(x: size.width + 10, y: y - size.height * 0.6))
                    ctx.stroke(path, with: .color(.black), lineWidth: 0.5)
                    y += step
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .blendMode(.overlay)

            // Inner gold rule
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(
                    LinearGradient(
                        colors: [PassportPalette.goldHi, PassportPalette.gold, PassportPalette.goldDeep],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.2
                )
                .padding(14)
                .shadow(color: PassportPalette.goldDeep.opacity(0.4), radius: 0.5, x: 0.5, y: 0.5)

            // Gold lettering — embossed feel (offset shadows light/dark)
            VStack(spacing: 6) {
                Spacer()
                // Mensa lozenge mark
                ZStack {
                    Circle()
                        .stroke(
                            LinearGradient(
                                colors: [PassportPalette.goldHi, PassportPalette.goldDeep],
                                startPoint: .topLeading, endPoint: .bottomTrailing
                            ),
                            lineWidth: 1.5
                        )
                        .frame(width: 92, height: 92)
                    Text("M")
                        .font(.system(size: 56, weight: .black, design: .serif))
                        .foregroundStyle(
                            LinearGradient(
                                colors: [PassportPalette.goldHi, PassportPalette.gold],
                                startPoint: .top, endPoint: .bottom
                            )
                        )
                        .shadow(color: PassportPalette.goldDeep.opacity(0.7), radius: 0.5, x: 0.5, y: 1)
                        .shadow(color: .black.opacity(0.5), radius: 1, x: 0, y: 2)
                }
                Spacer()
                Text("MENSA ITALIA")
                    .font(.system(size: 18, weight: .black, design: .serif))
                    .tracking(6)
                    .embossedGold()
                Text("PASSAPORTO")
                    .font(.system(size: 13, weight: .bold, design: .serif))
                    .tracking(8)
                    .embossedGold()
                Spacer().frame(height: 24)
                // Subtle scan instruction (only readable on close inspection)
                Text(tr("passport.tap_to_open", fallback: "· TAP TO OPEN ·"))
                    .font(.system(size: 9, weight: .semibold, design: .serif))
                    .tracking(3)
                    .foregroundStyle(PassportPalette.gold.opacity(0.55))
                    .padding(.bottom, 22)
            }
            .padding(.horizontal, 28)

            // Highlight on top-left for 3D feel
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [.white.opacity(0.10), .clear],
                        startPoint: .topLeading,
                        endPoint: .center
                    )
                )
                .blendMode(.plusLighter)
                .allowsHitTesting(false)
        }
        .frame(width: width, height: height)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .overlay(
            // Spine accent on the leading edge
            HStack(spacing: 0) {
                LinearGradient(
                    colors: [PassportPalette.coverDeep, .black.opacity(0.0)],
                    startPoint: .leading, endPoint: .trailing
                )
                .frame(width: 12)
                Spacer()
            }
            .allowsHitTesting(false)
        )
    }
}

private extension View {
    func embossedGold() -> some View {
        self
            .foregroundStyle(
                LinearGradient(
                    colors: [PassportPalette.goldHi, PassportPalette.gold, PassportPalette.goldDeep],
                    startPoint: .top, endPoint: .bottom
                )
            )
            .shadow(color: .black.opacity(0.55), radius: 0.5, x: 0, y: 1)
            .shadow(color: PassportPalette.goldHi.opacity(0.6), radius: 0.3, x: 0, y: -0.5)
    }
}
