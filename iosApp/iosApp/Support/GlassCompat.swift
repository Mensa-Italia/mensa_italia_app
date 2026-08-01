import SwiftUI

// Liquid Glass chrome with pre-iOS 26 fallbacks. The app deploys back to
// iOS 17, where the `.glassEffect`/`.glass*` APIs don't exist: fall back to
// frosted `.ultraThinMaterial` on the same shape and to the bordered button
// styles, which read the same visual hierarchy.
extension View {
    @ViewBuilder
    func compatGlass(cornerRadius: CGFloat, tint: Color? = nil) -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(
                .regular.tint(tint ?? .clear),
                in: .rect(cornerRadius: cornerRadius)
            )
        } else if let tint {
            self.background(tint.opacity(0.22), in: .rect(cornerRadius: cornerRadius))
                .background(.ultraThinMaterial, in: .rect(cornerRadius: cornerRadius))
        } else {
            self.background(.ultraThinMaterial, in: .rect(cornerRadius: cornerRadius))
        }
    }

    @ViewBuilder
    func compatGlassCapsule() -> some View {
        if #available(iOS 26.0, *) {
            self.glassEffect(.regular, in: .capsule)
        } else {
            self.background(.ultraThinMaterial, in: .capsule)
        }
    }

    @ViewBuilder
    func compatGlassProminentButtonStyle() -> some View {
        if #available(iOS 26.0, *) {
            self.buttonStyle(.glassProminent)
        } else {
            self.buttonStyle(.borderedProminent)
        }
    }

    @ViewBuilder
    func compatGlassButtonStyle() -> some View {
        if #available(iOS 26.0, *) {
            self.buttonStyle(.glass)
        } else {
            self.buttonStyle(.bordered)
        }
    }
}
