import SwiftUI

extension View {
    /// List style for a Settings-like index that is the sidebar of a
    /// NavigationSplitView: inset cards while collapsed into a pushing list
    /// (iPhone, iPhone Duo folded), the Settings sidebar look beside a detail
    /// (iPhone Duo unfolded, iPad).
    ///
    /// The sidebar gets the grouped (light grey) background, the same one
    /// `indexPageBackground()` gives the pages beside it: side by side, the
    /// two columns read as one surface.
    @ViewBuilder
    func indexListStyle(sidebar: Bool) -> some View {
        if sidebar {
            listStyle(.sidebar)
                .scrollContentBackground(.hidden)
                .background(Color(.systemGroupedBackground))
        } else {
            listStyle(.insetGrouped)
        }
    }

    /// Background of a page opened from an index (Explore, Profile): the
    /// sidebar's grouped grey, on iPhone too, so every section page sits on
    /// the same surface. Pages draw cards on it, not their own background;
    /// a card that wants to stand out uses `secondarySystemGroupedBackground`.
    func indexPageBackground() -> some View {
        background(Color(.systemGroupedBackground).ignoresSafeArea())
    }

    /// Keeps an index-and-page NavigationSplitView in its pushing, iPhone
    /// form where a regular-width window is still too narrow for a sidebar
    /// beside a page: iPhone Duo unfolded and held upright (~670pt) squeezed
    /// the sidebar until every row wrapped. NavigationSplitView collapses on
    /// the environment's size class, so that is what this narrows.
    func collapsesSidebarWhenNarrow() -> some View {
        modifier(NarrowSidebarCollapse())
    }
}

private struct NarrowSidebarCollapse: ViewModifier {
    /// Unfolded and on its side the iPhone Duo is ~950pt wide; upright, ~670.
    static let minimumWidth: CGFloat = 760

    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var width: CGFloat?

    func body(content: Content) -> some View {
        content
            .environment(\.horizontalSizeClass, effectiveSizeClass)
            .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
    }

    /// Compact until the width is known: starting regular and collapsing a
    /// frame later left the index's default section pushed over it.
    private var effectiveSizeClass: UserInterfaceSizeClass? {
        guard sizeClass == .regular else { return sizeClass }
        guard let width, width >= Self.minimumWidth else { return .compact }
        return .regular
    }
}
