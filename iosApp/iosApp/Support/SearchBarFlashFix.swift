import SwiftUI
import UIKit

/// Fixes the iOS 26 search-bar flash at launch.
///
/// On views that use `.searchable` together with a large title and a `List`,
/// iOS 26 renders the search-bar drawer in the expanded position for a couple
/// of frames before realising it should be hidden. The result is a brief flash
/// of the grey search field at launch.
///
/// This modifier walks the responder chain after the view appears, sets
/// `hidesSearchBarWhenScrolling = true` on the hosting `UIViewController`'s
/// navigation item, and offsets the underlying `UIScrollView` past the search
/// drawer so the bar starts hidden. The bar then re-appears the moment the
/// user pulls down, which is what we want.
struct SearchBarFlashFixModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.background(SearchBarFlashFixHost().allowsHitTesting(false))
    }
}

private struct SearchBarFlashFixHost: UIViewRepresentable {
    func makeUIView(context: Context) -> ProbeView {
        let v = ProbeView(frame: .zero)
        v.isHidden = true
        return v
    }

    func updateUIView(_ uiView: ProbeView, context: Context) {
        uiView.scheduleFix()
    }
}

private final class ProbeView: UIView {
    private var didFix = false
    private var attempts = 0

    override func didMoveToWindow() {
        super.didMoveToWindow()
        guard window != nil else { return }
        scheduleFix()
    }

    func scheduleFix() {
        guard !didFix else { return }
        DispatchQueue.main.async { [weak self] in self?.tryFix() }
    }

    private func tryFix() {
        guard !didFix else { return }
        attempts += 1

        // Walk responder chain to the hosting UIViewController.
        var responder: UIResponder? = self
        while let r = responder, !(r is UIViewController) { responder = r.next }
        guard let vc = responder as? UIViewController else {
            retryIfPossible(); return
        }

        // Make sure the search-bar drawer hides when the user scrolls.
        vc.navigationItem.hidesSearchBarWhenScrolling = true
        if let sc = vc.navigationItem.searchController {
            sc.hidesNavigationBarDuringPresentation = true
        }

        // Find the primary UIScrollView and push content past the drawer so
        // the bar starts hidden. Typical search-bar drawer height is ~52pt;
        // we use 56pt to leave a hair of slack.
        guard let scroll = findScrollView(in: vc.view) else {
            retryIfPossible(); return
        }

        let drawerHeight: CGFloat = 56
        let target = CGPoint(x: scroll.contentOffset.x, y: drawerHeight)
        if scroll.contentOffset.y < drawerHeight {
            UIView.performWithoutAnimation {
                scroll.setContentOffset(target, animated: false)
            }
        }

        didFix = true
    }

    private func retryIfPossible() {
        guard attempts < 6 else { return }
        // Backoff: 16ms, 32ms, 64ms, ... up to ~1s.
        let delay = 0.016 * pow(2.0, Double(attempts - 1))
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { [weak self] in
            self?.tryFix()
        }
    }

    private func findScrollView(in view: UIView) -> UIScrollView? {
        if let s = view as? UIScrollView { return s }
        for sub in view.subviews {
            if let found = findScrollView(in: sub) { return found }
        }
        return nil
    }
}

extension View {
    /// Hide the iOS 26 `.searchable` drawer on first appearance to avoid the
    /// launch-time flash. The bar still appears when the user pulls down.
    func fixSearchBarFlash() -> some View { modifier(SearchBarFlashFixModifier()) }
}
