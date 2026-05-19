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
    @State private var heroIn = false          // initial hero entrance
    @State private var isOpen = false          // cover open (rotation3D)
    @State private var pagesVisible = false    // parchment pages fade in
    @State private var stampsRevealed = false  // stamps stagger trigger
    @State private var currentPage: Int = 0
    @State private var totalPages: Int = 1
    @State private var coverOpacity: Double = 1
    @State private var coverMounted: Bool = true

    var body: some View {
        ZStack {
            // Velvet backdrop — heavier ink so the passport pops.
            LinearGradient(
                colors: [
                    Color(red: 0.04, green: 0.05, blue: 0.10),
                    Color(red: 0.10, green: 0.12, blue: 0.18)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            GeometryReader { geo in
                let w = min(geo.size.width - 32, 360)
                let h = w * 125.0 / 88.0  // passport aspect

                ZStack {
                    // Inside pages (revealed when cover swings open)
                    PassportInside(
                        width: w,
                        height: h,
                        stamps: stamps,
                        visible: pagesVisible,
                        stampsRevealed: stampsRevealed,
                        onTapStamp: { stamp in
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            selectedStamp = StampSelection(stamp: stamp)
                        },
                        currentPage: $currentPage,
                        totalPages: $totalPages
                    )
                    .opacity(pagesVisible ? 1 : 0)
                    .animation(.easeOut(duration: 0.45), value: pagesVisible)

                    // Cover (with 3D Y rotation around leading edge).
                    // When closed it sits ON TOP. When the open animation
                    // completes we REMOVE it from the hierarchy entirely so
                    // no leftover cobalt strip or hard shadow leaks onto the
                    // pages from the leading-edge accents on the cover.
                    if coverMounted {
                        PassportCover(width: w, height: h)
                            .rotation3DEffect(
                                .degrees(isOpen ? -162 : 0),
                                axis: (x: 0, y: 1, z: 0),
                                anchor: .leading,
                                perspective: 0.6
                            )
                            .opacity(coverOpacity)
                            .allowsHitTesting(coverOpacity > 0.05)
                            .onTapGesture {
                                UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
                                withAnimation(.spring(response: 0.85, dampingFraction: 0.72)) {
                                    isOpen.toggle()
                                }
                                if isOpen {
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                                        pagesVisible = true
                                    }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) {
                                        stampsRevealed = true
                                    }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.80) {
                                        withAnimation(.easeOut(duration: 0.28)) {
                                            coverOpacity = 0
                                        }
                                    }
                                    // Unmount the cover entirely after fade.
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.15) {
                                        if isOpen { coverMounted = false }
                                    }
                                }
                            }
                    } else {
                        // Invisible re-mount affordance: tap top-left corner
                        // of the spread to close again.
                        Color.clear
                            .frame(width: w, height: h)
                            .contentShape(Rectangle())
                            .onLongPressGesture(minimumDuration: 0.45) {
                                UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
                                coverMounted = true
                                coverOpacity = 1
                                withAnimation(.spring(response: 0.85, dampingFraction: 0.72)) {
                                    isOpen = false
                                }
                                stampsRevealed = false
                                pagesVisible = false
                            }
                            .allowsHitTesting(false) // don't intercept page taps; long-press handled by overlay below
                    }
                }
                .frame(width: w, height: h)
                // No outer clip: pages can fly past the passport silhouette
                // during the flip. Shadows still applied to give physical
                // weight on the velvet.
                .shadow(color: .black.opacity(0.22), radius: 28, x: 0, y: 16)
                .shadow(color: .black.opacity(0.14), radius: 6, x: 0, y: 3)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .scaleEffect(heroIn ? 1 : 0.92)
                .offset(y: heroIn ? 0 : 60)
                .opacity(heroIn ? 1 : 0)
                .animation(.spring(response: 0.7, dampingFraction: 0.78), value: heroIn)
            }
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 12) {
                if isOpen && totalPages > 1 {
                    pageIndicator
                        .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
                EntryStampButton {
                    UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
                    showScanner = true
                }
                .padding(.bottom, 12)
                .opacity(heroIn ? 1 : 0)
                .animation(.easeOut(duration: 0.45).delay(0.35), value: heroIn)
            }
            .animation(.easeInOut(duration: 0.3), value: isOpen)
        }
        .navigationTitle(tr("addons.stamp.passport.title", fallback: "Passaporto"))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            start()
            heroIn = true
        }
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

    // MARK: - Data plumbing (unchanged from previous TableportStampView)

    private func start() {
        stampsSub?.close()
        stampsSub = FlowBridgeKt.subscribe(
            flow: koin.stamps.observeAll(),
            onEach: { value in
                Task { @MainActor in
                    let list = (value as? [StampUserModel]) ?? []
                    self.stamps = list
                    StampImagePrefetcher.warmAll(list)
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

    // MARK: - Indicator (gold dots on velvet)

    @ViewBuilder
    private var pageIndicator: some View {
        HStack(spacing: 10) {
            Image(systemName: "arrow.left")
                .font(.system(size: 10, weight: .heavy))
                .foregroundStyle(PassportPalette.gold.opacity(currentPage > 0 ? 0.85 : 0.25))
            HStack(spacing: 6) {
                ForEach(0..<totalPages, id: \.self) { i in
                    Circle()
                        .fill(i == currentPage
                              ? PassportPalette.goldHi
                              : PassportPalette.gold.opacity(0.35))
                        .frame(width: i == currentPage ? 7 : 5,
                               height: i == currentPage ? 7 : 5)
                        .animation(.easeOut(duration: 0.2), value: currentPage)
                }
            }
            Image(systemName: "arrow.right")
                .font(.system(size: 10, weight: .heavy))
                .foregroundStyle(PassportPalette.gold.opacity(currentPage < totalPages - 1 ? 0.85 : 0.25))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 7)
        .background(
            Capsule()
                .fill(.black.opacity(0.35))
                .overlay(Capsule().stroke(PassportPalette.gold.opacity(0.35), lineWidth: 0.8))
        )
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
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: "800x600")
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
