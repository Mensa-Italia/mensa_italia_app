import SwiftUI

/// Hides the navigation bar's material backdrop so the large-title text
/// is never obscured by a glass blur when the view is scrolled to the top.
/// When the title collapses to inline, the system's default chrome takes
/// over again — this only removes the expanded-mode backdrop.
struct CleanNavBar: ViewModifier {
    func body(content: Content) -> some View {
        content
            .toolbarBackground(.hidden, for: .navigationBar)
    }
}

extension View {
    func cleanNavBar() -> some View {
        modifier(CleanNavBar())
    }
}
