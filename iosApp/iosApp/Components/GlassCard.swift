import SwiftUI

/// Reusable Liquid Glass card container.
/// Uses `.glassEffect` on iOS 26 / Xcode 26.
struct GlassCard<Content: View>: View {
    var tint: Color?
    var padding: CGFloat
    var cornerRadius: CGFloat

    @ViewBuilder let content: () -> Content

    init(
        tint: Color? = nil,
        padding: CGFloat = 20,
        cornerRadius: CGFloat = 24,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.tint = tint
        self.padding = padding
        self.cornerRadius = cornerRadius
        self.content = content
    }

    var body: some View {
        content()
            .padding(padding)
            .glassEffect(
                .regular.tint(tint ?? .clear),
                in: .rect(cornerRadius: cornerRadius)
            )
    }
}
