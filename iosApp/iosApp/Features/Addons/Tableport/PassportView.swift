import SwiftUI
import Shared

// MARK: - Palette (passport skeuomorphic — physical object)

enum PassportPalette {
    /// Mensa cobalt for the leather cover.
    static let coverDeep   = Color(red: 0.045, green: 0.110, blue: 0.275)  // #0B1C46
    static let coverMid    = Color(red: 0.094, green: 0.180, blue: 0.420)  // #182E6B
    static let coverHi     = Color(red: 0.145, green: 0.260, blue: 0.560)  // #25428E
    /// Embossed gold.
    static let gold        = Color(red: 0.788, green: 0.663, blue: 0.416)  // #C9A96A
    static let goldDeep    = Color(red: 0.592, green: 0.471, blue: 0.235)  // #97783C
    static let goldHi      = Color(red: 0.949, green: 0.847, blue: 0.612)  // #F2D89C
    /// Ivory parchment inside.
    static let parchment   = Color(red: 0.949, green: 0.918, blue: 0.851)  // #F2EBD9
    static let parchmentEdge = Color(red: 0.875, green: 0.831, blue: 0.741) // #DFD4BD
    /// Stamp "ENTRY" red.
    static let stampRed    = Color(red: 0.667, green: 0.149, blue: 0.149)  // #AA2626
}

/// Entry point — keep existing call sites working.
struct TableportStampView: View {
    var body: some View { PassportView() }
}

/// Skeuomorphic passport view with hero entrance, cover-open 3D rotation, and stamp stagger.
struct PassportView: View {
    @State private var stamps: [StampUserModel] = []
    @State private var stampsSub: Closeable?

    @State private var showScanner = false
    @State private var confirmPayload: ScanPayload?
    @State private var selectedStamp: StampSelection?

    // Animation state
    @State private var stampsRevealed = false  // stamps stagger trigger (unfolded spread)
    /// A phone's passport as a two-sheet book: 0 the cover, 1 the stamps.
    @State private var coverPage = 0
    @State private var currentPage: Int = 0

    // Unfolded iPhone Duo: the passport lies open across the hinge.
    @State private var isSpread = false
    @State private var spreadIndex = 0

    /// The device folds (iPhone Duo). The phone is then the passport: folded
    /// it shows the cover and nothing else, unfolding it is what opens it.
    @State private var hasHinge = false
    /// Bars laid out down the side (iPhone Duo): the top inset the system
    /// still reserves for the navigation bar holds nothing.
    @State private var barsAreVertical = false

    /// Room kept clear at the bottom of the pages for the ENTRY button.
    private let entryButtonClearance: CGFloat = 84

    // The passport *is* the screen: closed, the cover fills it edge to edge;
    // on an unfolded iPhone Duo the two pages fill the two halves, bound on
    // the hinge (side by side, or one above the other held upright). The paper runs under bars, camera and hinge; only the
    // contents keep to the safe area.
    var body: some View {
        // The outer reader only measures the safe area: inside one that
        // ignores it, the insets come back as zero.
        GeometryReader { outer in
            let safe = contentInsets(outer.safeAreaInsets)
            GeometryReader { geo in
                let spread = spreadLayout(in: geo)
                ZStack {
                    if let spread {
                        openPassport(spread, in: geo, safe: safe)
                    } else {
                        singlePassport(in: geo, safe: safe)
                    }
                    EntryStampButton {
                        UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                        showScanner = true
                    }
                    .position(
                        x: entryButtonX(spread, in: geo, safe: safe),
                        y: geo.size.height - safe.bottom - 42
                    )
                }
                .onChange(of: spread != nil, initial: true) { _, unfolded in
                    setSpread(unfolded)
                }
            }
            .ignoresSafeArea()
        }
        .background {
            // Velvet, only seen while a tapped cover turns away.
            LinearGradient(
                colors: [
                    Color(red: 0.04, green: 0.05, blue: 0.10),
                    Color(red: 0.10, green: 0.12, blue: 0.18)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()
        }
        .modifier(HingePresence(hasHinge: $hasHinge))
        .modifier(VerticalBarPresence(isVertical: $barsAreVertical))
        .toolbar(.hidden, for: .tabBar)
        .navigationTitle(tr("addons.stamp.passport.title", fallback: "Passaporto"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // The cover says it already; a title would sit on the leather.
            ToolbarItem(placement: .principal) { Color.clear.frame(width: 1, height: 1) }
            // Titled, so it reads in iPhone Duo's vertical bar too.
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink {
                    MedalCabinetView()
                } label: {
                    Label {
                        Text(tr("medals.title", fallback: "Medagliere"))
                    } icon: {
                        RainbowMedalIcon()
                    }
                }
            }
        }
        .task {
            start()
        }
        // Il prefetch segue la pagina. Scaldare solo all'arrivo dei dati
        // basterebbe se si scaldasse tutto, ma adesso si scalda una finestra:
        // senza questo, sfogliando oltre la finestra iniziale le immagini
        // tornerebbero a caricarsi solo quando sono gia' a schermo.
        .onChange(of: currentPage) { _, _ in warmAroundOpenPages() }
        .onChange(of: spreadIndex) { _, _ in warmAroundOpenPages() }
        .onChange(of: isSpread) { _, _ in warmAroundOpenPages() }
        .onDisappear { stop() }
        .sheet(isPresented: $showScanner) {
            QRScannerView { id, code in
                showScanner = false
                confirmPayload = ScanPayload(id: id, code: code)
            } onCancel: {
                showScanner = false
            }
        }
        .sheet(item: $confirmPayload) { payload in
            StampConfirmSheet(stampId: payload.id, code: payload.code) {
                confirmPayload = nil
                Task { await refresh() }
            }
        }
        .sheet(item: $selectedStamp) { sel in
            PassportStampDetailSheet(stamp: sel.stamp)
        }
    }

    /// Where the passport's contents may go. At least 16pt from every edge
    /// so the gold rule clears the display's rounded corners.
    private func contentInsets(_ safe: EdgeInsets) -> EdgeInsets {
        var insets = safe
        if barsAreVertical { insets.top = 0 }
        insets.top = max(insets.top, 16)
        insets.leading = max(insets.leading, 16)
        insets.bottom = max(insets.bottom, 16)
        insets.trailing = max(insets.trailing, 16)
        return insets
    }

    /// Centred on the page it sits on: the right one when open side by
    /// side, the bottom one (as on a single page) held upright.
    private func entryButtonX(_ spread: SpreadLayout?, in geo: GeometryProxy, safe: EdgeInsets) -> CGFloat {
        let right = geo.size.width - safe.trailing
        guard let spread, spread.axis == .horizontal else { return (safe.leading + right) / 2 }
        return (spread.spine + spread.spineMargin + right) / 2
    }

    private var coverHint: String {
        hasHinge
            ? tr("passport.unfold_to_open", fallback: "APRI L'IPHONE PER SFOGLIARLO")
            : tr("passport.swipe_to_open", fallback: "· SWIPE TO OPEN ·")
    }

    // MARK: - Open passport (unfolded iPhone Duo)

    private struct SpreadLayout: Equatable {
        /// `.horizontal`: pages side by side, the hinge running down.
        /// `.vertical`: held upright, the hinge running across.
        let axis: Axis
        /// x (side by side) or y (upright) of the hinge, where the spine goes.
        let spine: CGFloat
        /// Half the hinge, its margins included: contents keep clear of it.
        let spineMargin: CGFloat
    }

    /// Nil without a hinge inside this view, or without room for two pages
    /// of stamps on its sides (e.g. still beside Explore's index). Turned
    /// upright the unfolded phone is still open: only the hinge runs across.
    private func spreadLayout(in geo: GeometryProxy) -> SpreadLayout? {
        if let hinge = geo.verticalHinge,
           hinge.minX >= 240, geo.size.width - hinge.maxX >= 240 {
            return SpreadLayout(axis: .horizontal, spine: hinge.midX, spineMargin: hinge.width / 2)
        }
        if let hinge = geo.horizontalHinge,
           hinge.minY >= 240, geo.size.height - hinge.maxY >= 240 {
            return SpreadLayout(axis: .vertical, spine: hinge.midY, spineMargin: hinge.height / 2)
        }
        return nil
    }

    private func openPassport(_ layout: SpreadLayout, in geo: GeometryProxy, safe: EdgeInsets) -> some View {
        let bottom = safe.bottom + entryButtonClearance
        let sideBySide = layout.axis == .horizontal
        let w = sideBySide ? min(layout.spine, geo.size.width - layout.spine) : geo.size.width
        let h = sideBySide ? geo.size.height : min(layout.spine, geo.size.height - layout.spine)
        let firstInsets = sideBySide
            ? EdgeInsets(top: safe.top, leading: safe.leading, bottom: bottom, trailing: layout.spineMargin)
            : EdgeInsets(top: safe.top, leading: safe.leading, bottom: layout.spineMargin, trailing: safe.trailing)
        let secondInsets = sideBySide
            ? EdgeInsets(top: safe.top, leading: layout.spineMargin, bottom: bottom, trailing: safe.trailing)
            : EdgeInsets(top: layout.spineMargin, leading: safe.leading, bottom: bottom, trailing: safe.trailing)
        return PassportSpread(
            pageWidth: w,
            pageHeight: h,
            stamps: stamps,
            stampsRevealed: stampsRevealed,
            onTapStamp: { stamp in
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                selectedStamp = StampSelection(stamp: stamp)
            },
            spreadIndex: $spreadIndex,
            axis: layout.axis,
            outerRadius: 0,
            firstInsets: firstInsets,
            secondInsets: secondInsets
        )
        .frame(width: sideBySide ? w * 2 : w, height: sideBySide ? h : h * 2)
        .position(
            x: sideBySide ? layout.spine : w / 2,
            y: sideBySide ? h / 2 : layout.spine
        )
    }

    /// The stamps of the pages beside the open ones: ahead of the scroll on a phone, by
    /// spread on an unfolded iPhone Duo. The spread warmed by single pages
    /// (it used to, on opening and when the stamps arrived) left the second
    /// page of the next spread to load while it curled into view.
    private func warmAroundOpenPages() {
        if isSpread {
            StampImagePrefetcher.warmAround(
                stamps, page: spreadIndex, perPage: PassportLayout.stampsPerPage * 2
            )
        } else {
            StampImagePrefetcher.warmAround(
                stamps, page: currentPage, perPage: PassportLayout.stampsPerPage, ahead: 4
            )
        }
    }

    /// Unfolding shows the open passport straight away, folding the closed
    /// cover: the phone's own hinge is the opening animation.
    private func setSpread(_ unfolded: Bool) {
        guard unfolded != isSpread else { return }
        var instant = Transaction()
        instant.disablesAnimations = true
        withTransaction(instant) {
            isSpread = unfolded
            if unfolded {
                hasHinge = true
                stampsRevealed = true
            } else {
                stampsRevealed = false
                currentPage = min(spreadIndex * 2, max(PassportSpread.pages(of: stamps).count - 1, 0))
            }
        }
    }

    // MARK: - Single page (folded iPhone Duo, other iPhones)

    private func singlePassport(in geo: GeometryProxy, safe: EdgeInsets) -> some View {
        let w = geo.size.width
        let h = geo.size.height
        var insets = safe
        insets.bottom += entryButtonClearance
        // Read here, in the body: the book's pages are built later, in their
        // own hosting controllers, and a state read only there would neither
        // see new stamps nor make this view redraw when they arrive.
        let stamps = self.stamps

        return Group {
            if hasHinge {
                // Folded iPhone Duo: the cover and nothing else, since the
                // phone itself opens it.
                cover(width: w, height: h, insets: insets)
            } else {
                // A phone: the cover is the first sheet of a book, swiped
                // away with the same page curl as the inside pages; the
                // stamps lie under it. Swiping back closes it.
                PassportCurlBook(
                    pageCount: 2,
                    pagesPerView: 1,
                    firstPage: $coverPage,
                    page: { index in
                        index == 0
                            ? AnyView(cover(width: w, height: h, insets: insets))
                            : AnyView(stampScroll(stamps, width: w, height: h, insets: insets))
                    },
                    verso: { index in
                        index == 0
                            ? AnyView(PassportCoverLining(width: w, height: h))
                            : AnyView(Color.clear)
                    },
                    // Stamps sit near the page's edges; taps there open them.
                    tapToTurn: false
                )
            }
        }
        .frame(width: w, height: h)
    }

    private func cover(width: CGFloat, height: CGFloat, insets: EdgeInsets) -> some View {
        PassportCover(
            width: width,
            height: height,
            cornerRadius: 0,
            insets: insets,
            hint: coverHint
        )
    }

    /// Every stamp in one floating scroll, already in place under the cover.
    private func stampScroll(_ stamps: [StampUserModel], width: CGFloat, height: CGFloat, insets: EdgeInsets) -> some View {
        PassportStampScroll(
            width: width,
            height: height,
            stamps: stamps,
            stampsRevealed: true,
            onTapStamp: { stamp in
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                selectedStamp = StampSelection(stamp: stamp)
            },
            currentPage: $currentPage,
            insets: insets
        )
    }

    // MARK: - Data plumbing (unchanged from previous TableportStampView)

    private func start() {
        stampsSub?.close()
        stampsSub = FlowBridgeKt.subscribe(
            flow: koin.stamps.observeAll(),
            onEach: { value in
                Task { @MainActor in
                    let list = (value as? [StampUserModel]) ?? []
                    self.stamps = list
                    warmAroundOpenPages()
                    StampImagePrefetcher.warmCollection(list)
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    private func stop() {
        stampsSub?.close(); stampsSub = nil
    }

    private func refresh() async {
        do { try await koin.stamps.refresh(filter: nil, sort: "-created") } catch { }
    }
}

private struct ScanPayload: Identifiable {
    let id: String
    let code: String
}

// MARK: - Entry "stamp" button (replaces glassProminent)

private struct EntryStampButton: View {
    let action: () -> Void
    @State private var pressed = false

    var body: some View {
        Button {
            action()
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "qrcode.viewfinder")
                    .font(.system(size: 18, weight: .heavy))
                Text(tr("addons.stamp.passport.scan", fallback: "ENTRY · SCAN"))
                    .font(.system(size: 15, weight: .black, design: .serif))
                    .tracking(2.2)
            }
            .foregroundStyle(PassportPalette.stampRed)
            .padding(.horizontal, 26)
            .padding(.vertical, 14)
            .background(
                ZStack {
                    // Inked stamp rectangle with slight rotation, ragged edge feel.
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .stroke(PassportPalette.stampRed, lineWidth: 2.2)
                    RoundedRectangle(cornerRadius: 6, style: .continuous)
                        .stroke(PassportPalette.stampRed.opacity(0.5), lineWidth: 1)
                        .padding(3)
                }
                .background(
                    PassportPalette.parchment
                        .opacity(0.92)
                        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
                )
            )
            .rotationEffect(.degrees(-2.5))
            .scaleEffect(pressed ? 0.94 : 1)
            .shadow(color: .black.opacity(0.35), radius: 8, x: 0, y: 4)
        }
        .buttonStyle(.plain)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    if !pressed {
                        withAnimation(.spring(response: 0.2, dampingFraction: 0.7)) { pressed = true }
                    }
                }
                .onEnded { _ in
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.65)) { pressed = false }
                }
        )
    }
}

// MARK: - Stamp detail sheet (lightweight)

private struct PassportStampDetailSheet: View {
    let stamp: StampUserModel
    @Environment(\.dismiss) private var dismiss

    private var record: StampModel? { stamp.stampRecord }
    private var imageURL: URL? {
        guard let r = record, !r.image.isEmpty else { return nil }
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: PassportLayout.detailThumb)
    }
    private var title: String {
        if let d = record?.description_, !d.isEmpty { return d }
        return stamp.stampId
    }
    private var dateText: String {
        let date = Date(timeIntervalSince1970: Double(stamp.created.epochSeconds))
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .long
        return f.string(from: date)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    PassportStampDecal(
                        url: imageURL,
                        size: 240,
                        rotation: 0,
                        showsCancel: true
                    )
                    .padding(.top, 12)

                    Text(title)
                        .font(.title3.weight(.semibold))
                        .multilineTextAlignment(.center)
                    Label(dateText, systemImage: "calendar")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(24)
            }
            .navigationTitle(tr("addons.stamp.passport.detail", fallback: "Francobollo"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("app.done", fallback: "Fine")) { dismiss() }
                }
            }
        }
    }
}

private struct StampSelection: Identifiable {
    let stamp: StampUserModel
    var id: String { stamp.id }
}

/// Notes whether the device has a hinge at all (iPhone Duo).
private struct HingePresence: ViewModifier {
    @Binding var hasHinge: Bool

    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            content.onHingeChange { _, context in
                if context.hinge != nil { hasHinge = true }
            }
        } else {
            content
        }
    }
}

/// Notes whether the system lays the bars out vertically (iPhone Duo).
private struct VerticalBarPresence: ViewModifier {
    @Binding var isVertical: Bool

    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            content.background(VerticalBarProbe(isVertical: $isVertical))
        } else {
            content
        }
    }
}

@available(iOS 27.1, *)
private struct VerticalBarProbe: View {
    @Environment(\.toolbarVerticalEdge) private var edge
    @Binding var isVertical: Bool

    var body: some View {
        Color.clear.onChange(of: edge, initial: true) { _, edge in
            isVertical = edge != nil
        }
    }
}

/// Tableport over the whole screen, closed from its own bar. Opened from
/// Today, under the membership card.
struct PassportScreen: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            TableportStampView()
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button {
                            dismiss()
                        } label: {
                            Label(tr("app.close", fallback: "Chiudi"), systemImage: "xmark")
                        }
                    }
                }
        }
    }
}
