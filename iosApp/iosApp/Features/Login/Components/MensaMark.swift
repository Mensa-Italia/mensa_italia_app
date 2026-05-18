import SwiftUI

/// Official Mensa Italia logomark loaded from the Asset Catalog (vector SVG).
/// Always white-on-blue per brandbook (page 14, "Il marchio · colore" — pittogramma
/// bianco su fondo blu).
struct MensaMark: View {
    var size: CGFloat = 96
    /// If true, the mark sits on the brand-blue squircle background as in
    /// brandbook page 12 ("Il logo del Mensa Italia"). If false, only the
    /// vector glyph is shown (e.g. when already on blue).
    var inBlueBadge: Bool = false

    var body: some View {
        Image("MensaLogo")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .foregroundStyle(.white)
            .frame(width: size, height: size)
            .padding(inBlueBadge ? size * 0.18 : 0)
            .background {
                if inBlueBadge {
                    RoundedRectangle(cornerRadius: size * 0.28, style: .continuous)
                        .fill(AppTheme.Colors.mensaBlue)
                        .shadow(color: AppTheme.Colors.mensaBlue.opacity(0.5),
                                radius: size * 0.18, y: size * 0.06)
                }
            }
    }
}

#Preview {
    ZStack {
        AppTheme.brandGradientDeep.ignoresSafeArea()
        VStack(spacing: 32) {
            MensaMark(size: 110)
            MensaMark(size: 110, inBlueBadge: true)
        }
    }
}
