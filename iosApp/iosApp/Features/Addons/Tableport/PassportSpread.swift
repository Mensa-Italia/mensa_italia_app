import SwiftUI
import Shared

/// Two facing passport pages, bound in the middle: an unfolded iPhone Duo
/// shows the passport open, with the spine on the hinge. On its side the
/// pages lie left and right; held upright, with the hinge running across,
/// one above the other.
///
/// Spread `n` shows pages `2n` (left, or top) and `2n + 1` (right, or
/// bottom). Pages turn with a real curl (`PassportCurlBook`); the index only
/// moves once a turn has finished.
struct PassportSpread: View {
    let pageWidth: CGFloat
    let pageHeight: CGFloat
    let stamps: [StampUserModel]
    let stampsRevealed: Bool
    let onTapStamp: (StampUserModel) -> Void
    @Binding var spreadIndex: Int
    /// `.horizontal`: pages side by side. `.vertical`: one above the other.
    var axis: Axis = .horizontal
    /// See `SinglePassportPage.outerRadius` / `.insets`, per page.
    var outerRadius: CGFloat = 18
    var firstInsets = EdgeInsets()
    var secondInsets = EdgeInsets()

    private var pages: [[StampUserModel]] { PassportSpread.pages(of: stamps) }

    static func pages(of stamps: [StampUserModel]) -> [[StampUserModel]] {
        let perPage = PassportLayout.stampsPerPage
        guard !stamps.isEmpty else { return [[]] }
        return stride(from: 0, to: stamps.count, by: perPage)
            .map { Array(stamps[$0..<min($0 + perPage, stamps.count)]) }
    }

    static func spreadCount(of stamps: [StampUserModel]) -> Int {
        (pages(of: stamps).count + 1) / 2
    }

    /// Where each page is bound: the first page's spine faces the second.
    private var firstSpine: Edge { axis == .horizontal ? .trailing : .bottom }
    private var secondSpine: Edge { axis == .horizontal ? .leading : .top }

    var body: some View {
        PassportCurlBook(
            pageCount: pages.count,
            pagesPerView: 2,
            axis: axis,
            firstPage: Binding(
                get: { spreadIndex * 2 },
                set: { spreadIndex = $0 / 2 }
            )
        ) { index in
            AnyView(page(index, spine: index % 2 == 0 ? firstSpine : secondSpine))
        }
        // The spine's place is fixed when the book is made.
        .id(axis)
        .frame(
            width: axis == .horizontal ? pageWidth * 2 : pageWidth,
            height: axis == .horizontal ? pageHeight : pageHeight * 2
        )
    }

    // MARK: - Pages

    @ViewBuilder
    private func page(_ index: Int, spine: Edge) -> some View {
        let exists = pages.indices.contains(index)
        SinglePassportPage(
            width: pageWidth,
            height: pageHeight,
            pageIndex: index,
            totalPages: pages.count,
            chunk: exists ? pages[index] : [],
            totalStamps: stamps.count,
            stampsRevealed: stampsRevealed,
            onTapStamp: onTapStamp,
            spineEdge: spine,
            squareSpine: true,
            blank: !exists,
            outerRadius: outerRadius,
            insets: spine == firstSpine ? firstInsets : secondInsets
        )
    }
}

/// Pages turned with a real page curl (UIPageViewController's, as in Books):
/// the sheet bends under the finger, lifts from the corner it's held by and
/// shows the following page on its back. A rigid sheet swung on the spine
/// read as a card flipping, not as paper.
///
/// One page at a time, bound on its leading edge (a phone), or two, bound in
/// the middle (iPhone Duo unfolded: side by side, or one above the other held
/// upright). Edge taps and drags are the page controller's own. `firstPage`
/// follows the turns; set from outside, the book turns to it.
///
/// Always double-sided, so a lifted sheet shows paper on its back: one-sided,
/// UIKit draws the back as the front seen through, mirrored and translucent.
/// A spread's backs are its pages; a single page's back is `verso(page)`.
struct PassportCurlBook: UIViewControllerRepresentable {
    let pageCount: Int
    let pagesPerView: Int
    var axis: Axis = .horizontal
    @Binding var firstPage: Int
    let page: (Int) -> AnyView
    var verso: (Int) -> AnyView = { _ in AnyView(EmptyView()) }
    /// Taps on the page's edges turn it, as well as drags. Off where the
    /// page has things to tap near its edges.
    var tapToTurn = true

    /// The book's sheets' faces in order. A spread shows two pages, and an
    /// odd last one faces a blank; a single page is followed by its back.
    fileprivate var slotCount: Int {
        pagesPerView == 2 ? max(2, pageCount + pageCount % 2) : 2 * max(1, pageCount)
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIViewController(context: Context) -> UIPageViewController {
        let spine: UIPageViewController.SpineLocation = pagesPerView == 2 ? .mid : .min
        let book = UIPageViewController(
            transitionStyle: .pageCurl,
            navigationOrientation: axis == .horizontal ? .horizontal : .vertical,
            options: [.spineLocation: NSNumber(value: spine.rawValue)]
        )
        book.isDoubleSided = true
        if !tapToTurn {
            book.gestureRecognizers
                .filter { $0 is UITapGestureRecognizer }
                .forEach { $0.isEnabled = false }
        }
        book.dataSource = context.coordinator
        book.delegate = context.coordinator
        book.view.backgroundColor = .clear
        book.setViewControllers(
            context.coordinator.controllers(at: firstPage),
            direction: .forward,
            animated: false
        )
        return book
    }

    func updateUIViewController(_ book: UIPageViewController, context: Context) {
        let coordinator = context.coordinator
        coordinator.parent = self
        coordinator.refreshContent()
        let shown = coordinator.shownFirstPage(of: book)
        let target = coordinator.normalized(firstPage)
        guard shown != target, !coordinator.isTurning else { return }
        book.setViewControllers(
            coordinator.controllers(at: target),
            direction: target > shown ? .forward : .reverse,
            animated: abs(target - shown) == pagesPerView
        )
    }

    final class Coordinator: NSObject, UIPageViewControllerDataSource, UIPageViewControllerDelegate {
        var parent: PassportCurlBook
        var isTurning = false
        private var cache: [Int: PageController] = [:]

        final class PageController: UIHostingController<AnyView> {
            var index = 0
        }

        init(_ parent: PassportCurlBook) {
            self.parent = parent
        }

        /// Pages are what `firstPage` counts; slots are the faces the page
        /// controller turns (a single page and its back are two slots).
        private var isSpread: Bool { parent.pagesPerView == 2 }

        func normalized(_ first: Int) -> Int {
            if isSpread {
                let clamped = max(0, min(first, parent.slotCount - 1))
                return clamped - clamped % 2
            }
            return max(0, min(first, max(parent.pageCount, 1) - 1))
        }

        /// What the page controller is handed: a spread's two pages; a
        /// single page alone (its back is asked for through the data source,
        /// and handing both throws).
        func controllers(at first: Int) -> [UIViewController] {
            isSpread
                ? [controller(normalized(first)), controller(normalized(first) + 1)]
                : [controller(normalized(first) * 2)]
        }

        func shownFirstPage(of book: UIPageViewController) -> Int {
            let slot = (book.viewControllers?.first as? PageController)?.index ?? 0
            return isSpread ? slot : slot / 2
        }

        private func content(_ slot: Int) -> AnyView {
            if isSpread { return parent.page(slot) }
            return slot % 2 == 0 ? parent.page(slot / 2) : parent.verso(slot / 2)
        }

        /// New stamps, revealed stamps: the pages already made redraw.
        func refreshContent() {
            for (slot, controller) in cache {
                controller.rootView = content(slot)
            }
        }

        private func controller(_ index: Int) -> PageController {
            if let controller = cache[index] { return controller }
            let controller = PageController(rootView: content(index))
            controller.index = index
            controller.view.backgroundColor = .clear
            controller.safeAreaRegions = []
            cache[index] = controller
            return controller
        }

        /// Keeps the pages around the open one; the rest are made again
        /// when reached.
        private func trimCache(around first: Int) {
            cache = cache.filter { abs($0.key - first) <= 4 }
        }

        func pageViewController(
            _ book: UIPageViewController,
            viewControllerBefore viewController: UIViewController
        ) -> UIViewController? {
            guard let index = (viewController as? PageController)?.index, index > 0 else { return nil }
            return controller(index - 1)
        }

        func pageViewController(
            _ book: UIPageViewController,
            viewControllerAfter viewController: UIViewController
        ) -> UIViewController? {
            guard let index = (viewController as? PageController)?.index,
                  index + 1 < parent.slotCount else { return nil }
            return controller(index + 1)
        }

        func pageViewController(
            _ book: UIPageViewController,
            willTransitionTo pendingViewControllers: [UIViewController]
        ) {
            isTurning = true
        }

        func pageViewController(
            _ book: UIPageViewController,
            didFinishAnimating finished: Bool,
            previousViewControllers: [UIViewController],
            transitionCompleted completed: Bool
        ) {
            isTurning = false
            guard completed else { return }
            let first = shownFirstPage(of: book)
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            trimCache(around: isSpread ? first : first * 2)
            if parent.firstPage != first { parent.firstPage = first }
        }
    }
}
