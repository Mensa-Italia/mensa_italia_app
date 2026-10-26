import SwiftUI
import Shared

/// Parchment passport interior.
///
/// Design rules (current iteration):
///   • SINGLE visible page (not a two-page spread).
///   • Binding lives on the LEFT EDGE of the page (the spine of the passport).
///   • Spine rendered as subtle parchment-darker shadow + tiny gold thread
///     stitches — NEVER cobalt (cobalt belongs to the cover only).
///   • 6 stamps per page (3 rows × 2 cols).
///   • NO clipping anywhere — the flipping page is free to overflow the
///     passport silhouette during animation.
/// Quanti timbri stanno in una pagina del passaporto.
///
/// Era un `private let` dentro `PassportInside`, quindi `StampImagePrefetcher`
/// non aveva modo di saperlo: per questo scaldava tutta la collezione invece
/// della sola pagina vicina.
enum PassportLayout {
    static let stampsPerPage = 6

    /// Miniature richieste al server.
    ///
    /// Tutto il passaporto chiedeva `800x0`, anche la griglia, dove un timbro
    /// e' disegnato a ~73pt: a 3x sono ~220px, quindi si scaricavano circa 13
    /// volte i pixel necessari, sei volte per pagina.
    ///
    /// `gridThumb` deve restare identico fra griglia e prefetcher: l'URL e' la
    /// chiave di cache, e scaldare una misura per disegnarne un'altra rende il
    /// prefetch lavoro sprecato.
    ///
    /// Deve essere una misura dichiarata sul campo `stamp.image` del backend
    /// (`0x50`, `0x250`, `0x500`): PocketBase ignora le altre e manda
    /// l'originale. `320x0` non c'era, quindi ogni timbro arrivava a
    /// 1024×1024, ~850 KB da scaricare e 4 MB decodificato: la memory cache ne
    /// teneva una ventina e, sfogliando, i timbri si ricaricavano di continuo.
    /// `0x500` pesa ~145 KB e 1 MB in memoria, nitido ai ~430 px a cui si
    /// disegna.
    static let gridThumb = "0x500"
    static let detailThumb = "800x0"
}

/// A phone's passport: every stamp in one scroll, in order, on a single
/// parchment page. Paging six at a time made a long collection a chore to
/// leaf through; the two-page book with its curl stays for the unfolded
/// iPhone Duo (`PassportSpread`).
///
/// While it scrolls the stamps float: each trails the motion a little more
/// the farther it is from the middle of the view, so the gaps between them
/// stretch with the speed and settle back, wobbling, when the scroll stops.
struct PassportStampScroll: View {
    let width: CGFloat
    let height: CGFloat
    let stamps: [StampUserModel]
    let stampsRevealed: Bool
    let onTapStamp: (StampUserModel) -> Void
    /// The six-stamp page the scroll is at: what the prefetch warms around,
    /// and the spread an unfolding iPhone Duo opens on.
    @Binding var currentPage: Int
    /// See `SinglePassportPage.insets`.
    var insets = EdgeInsets()

    /// How far, in points, the stamps trail the scroll right now (signed).
    @State private var drift: CGFloat = 0

    private static let columnSpacing: CGFloat = 16
    private static let rowSpacing: CGFloat = 22
    private static let sidePadding: CGFloat = 22

    /// Two across, inside the side insets.
    private var stampSize: CGFloat {
        let inner = width - insets.leading - insets.trailing - 2 * Self.sidePadding
        return max(0, (inner - Self.columnSpacing) / 2)
    }

    /// The bottom band the stamps fade out in: the ENTRY button's area and
    /// a little above it.
    private var fadeHeight: CGFloat { insets.bottom + 80 }

    var body: some View {
        ZStack {
            // The paper, edge to edge. With stamps it has no gold rule: they
            // run the whole height of the phone, under the bars and the ENTRY
            // button, rather than inside a window.
            SinglePassportPage(
                width: width, height: height, pageIndex: 0, totalPages: 1,
                chunk: [], totalStamps: stamps.count, stampsRevealed: false,
                onTapStamp: { _ in }, blank: !stamps.isEmpty,
                outerRadius: 0, insets: insets, showsRule: stamps.isEmpty
            )
            if !stamps.isEmpty {
                scroll.mask(bottomFade)
            }
        }
        .frame(width: width, height: height)
    }

    /// Stamps fade into the paper at the bottom, so they don't crowd the
    /// ENTRY button floating over them.
    private var bottomFade: some View {
        let start = max(0, 1 - fadeHeight / max(height, 1))
        return LinearGradient(
            stops: [
                .init(color: .black, location: 0),
                .init(color: .black, location: start),
                .init(color: .black.opacity(0.25), location: start + (1 - start) * 0.55),
                .init(color: .clear, location: 1)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    private var scroll: some View {
        ScrollView {
            VStack(spacing: 22) {
                header
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: Self.columnSpacing),
                        GridItem(.flexible(), spacing: Self.columnSpacing)
                    ],
                    spacing: Self.rowSpacing
                ) {
                    ForEach(Array(stamps.enumerated()), id: \.element.id) { index, stamp in
                        stampView(stamp, at: index)
                    }
                }
            }
            .padding(.leading, insets.leading + Self.sidePadding)
            .padding(.trailing, insets.trailing + Self.sidePadding)
            // Starts clear of the bars, ends clear of the button; in between
            // the stamps scroll under both.
            .padding(.top, insets.top + 12)
            .padding(.bottom, insets.bottom + 24)
        }
        .scrollIndicators(.hidden)
        .ignoresSafeArea()
        .modifier(DriftTracking(
            drift: $drift,
            currentPage: $currentPage,
            rowHeight: stampSize + Self.rowSpacing
        ))
    }

    private var header: some View {
        HStack {
            Text(tr("addons.stamp.passport.collection", fallback: "COLLEZIONE"))
                .tracking(3)
            Spacer()
            Text("№ \(String(format: "%03d", stamps.count))")
                .tracking(2)
        }
        .font(.system(size: 9, weight: .heavy, design: .serif))
        .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.55))
    }

    private func stampView(_ stamp: StampUserModel, at index: Int) -> some View {
        let seed = deterministicSeed(stamp.id)
        return PassportStampDecal(
            url: stampImageURL(stamp),
            size: stampSize,
            rotation: (seed.angle - 0.5) * 14,
            showsCancel: index % 3 == 0
        )
        .scaleEffect(stampsRevealed ? 1 : 0.7)
        .opacity(stampsRevealed ? 1 : 0)
        .animation(
            .spring(response: 0.55, dampingFraction: 0.78).delay(Double(min(index, 6)) * 0.08),
            value: stampsRevealed
        )
        .modifier(FloatingStamp(drift: drift, phase: seed.dx + 0.5))
        .onTapGesture { onTapStamp(stamp) }
    }
}

/// Trails the scroll in proportion to the distance from the middle of the
/// view, with a hint of tilt that differs per stamp.
private struct FloatingStamp: ViewModifier {
    let drift: CGFloat
    /// 0…1, fixed per stamp: no two float alike.
    let phase: Double

    func body(content: Content) -> some View {
        content.visualEffect { [drift, phase] content, proxy in
            let viewport = proxy.bounds(of: .scrollView) ?? CGRect(origin: .zero, size: proxy.size)
            // -0.5 at the top of the view, 0 in the middle, 0.5 at the bottom.
            let position = (proxy.size.height / 2 - viewport.midY) / max(viewport.height, 1)
            let reach = 0.8 + 0.4 * phase
            return content
                .offset(y: drift * position * 2 * reach)
                .rotationEffect(.degrees(Double(drift) * 0.08 * (phase - 0.5)))
        }
    }
}

/// Drift from the scroll's speed, eased by a spring and let go with a
/// looser one when the scroll stops, so the stamps bob back into line. Also
/// reports the page the scroll is at. iOS 18 (scroll geometry and phase):
/// before it the scroll is plain.
private struct DriftTracking: ViewModifier {
    @Binding var drift: CGFloat
    @Binding var currentPage: Int
    let rowHeight: CGFloat

    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content
                .onScrollGeometryChange(for: CGFloat.self) { geometry in
                    geometry.contentOffset.y + geometry.contentInsets.top
                } action: { old, new in
                    let target = max(-26, min(26, (new - old) * 1.4))
                    withAnimation(.interactiveSpring(response: 0.28, dampingFraction: 0.7)) {
                        drift = target
                    }
                    let row = max(0, Int(new / max(rowHeight, 1)))
                    let page = row * 2 / PassportLayout.stampsPerPage
                    if page != currentPage { currentPage = page }
                }
                .onScrollPhaseChange { _, phase in
                    if phase == .idle {
                        withAnimation(.spring(response: 0.55, dampingFraction: 0.45)) { drift = 0 }
                    }
                }
        } else {
            content
        }
    }
}

private func stampImageURL(_ stamp: StampUserModel) -> URL? {
    guard let r = stamp.stampRecord, !r.image.isEmpty else { return nil }
    return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: PassportLayout.gridThumb)
}

// MARK: - SinglePassportPage (one parchment page, 6 stamps)

struct SinglePassportPage: View {
    let width: CGFloat
    let height: CGFloat
    let pageIndex: Int
    let totalPages: Int
    let chunk: [StampUserModel]
    let totalStamps: Int
    let stampsRevealed: Bool
    let onTapStamp: (StampUserModel) -> Void
    /// The bound edge. A two-page spread binds its left page on the trailing
    /// edge (its top page on the bottom one, held upright) and squares the
    /// corners on the spine, where the pages meet.
    var spineEdge: Edge = .leading
    var squareSpine: Bool = false
    /// The page after the last stamps of a spread: parchment only, no page
    /// number and no "no stamps yet" hint.
    var blank: Bool = false
    /// Corner radius away from the spine. 0 when the page fills the screen
    /// edge to edge: the display's own corners round it.
    var outerRadius: CGFloat = 18
    /// Where the page runs under something (status bar, camera, bars, the
    /// hinge, the ENTRY button): the paper goes there, its contents don't.
    var insets = EdgeInsets()
    /// The gold rule framing the page's contents. Off under a scroll that
    /// runs past the page's edges.
    var showsRule = true

    /// The six stamps go two across and three down on an upright page, three
    /// across and two down on a page lying on its side (an upright, unfolded
    /// iPhone Duo stacks two such pages).
    private var columnCount: Int { width > height * 1.15 ? 3 : 2 }

    private var columns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 12), count: columnCount)
    }

    /// Sized to fit both ways inside the grid's padding. Sized by width
    /// alone, three rows outgrew a passport-proportioned page, so the page
    /// drew taller than its frame (and than the cover).
    private var stampSize: CGFloat {
        let cols = CGFloat(columnCount)
        let rows = CGFloat(PassportLayout.stampsPerPage / columnCount)
        let byWidth = (width - insets.leading - insets.trailing - 64 - 12 * (cols - 1)) / cols
        let byHeight = (height - insets.top - insets.bottom - 56 - 50 - 14 * (rows - 1)) / rows
        return max(0, min(byWidth, byHeight))
    }

    private var pageShape: UnevenRoundedRectangle {
        let outer = outerRadius
        let spine: CGFloat = squareSpine ? min(2, outer) : outer
        func radius(_ edges: Edge...) -> CGFloat { edges.contains(spineEdge) ? spine : outer }
        return UnevenRoundedRectangle(
            topLeadingRadius: radius(.top, .leading),
            bottomLeadingRadius: radius(.bottom, .leading),
            bottomTrailingRadius: radius(.bottom, .trailing),
            topTrailingRadius: radius(.top, .trailing),
            style: .continuous
        )
    }

    var body: some View {
        ZStack {
            // Parchment background, on solid paper: the gradient's middle
            // stop is translucent, and a page curling up showed through it.
            pageShape
                .fill(PassportPalette.parchment)
            pageShape
                .fill(
                    LinearGradient(
                        colors: [
                            PassportPalette.parchment,
                            PassportPalette.parchmentEdge.opacity(0.85),
                            PassportPalette.parchment
                        ],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )

            // Parchment grain
            Canvas { ctx, size in
                var seed: UInt64 = UInt64(0x9E3779B97F4A7C15) &+ UInt64(pageIndex &* 1_000_003)
                for _ in 0..<240 {
                    seed = seed &* 6364136223846793005 &+ 1442695040888963407
                    let x = CGFloat(seed % UInt64(max(Int(size.width), 1)))
                    seed = seed &* 6364136223846793005 &+ 1442695040888963407
                    let y = CGFloat(seed % UInt64(max(Int(size.height), 1)))
                    seed = seed &* 6364136223846793005 &+ 1442695040888963407
                    let r = CGFloat(seed % 3) * 0.4
                    ctx.fill(
                        Path(ellipseIn: CGRect(x: x, y: y, width: r, height: r)),
                        with: .color(.black.opacity(0.05))
                    )
                }
            }
            .clipShape(pageShape)
            .allowsHitTesting(false)

            // Watermark "M"
            Text("M")
                .font(.system(size: min(width, height) * 0.7, weight: .black, design: .serif))
                .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.05))
                .allowsHitTesting(false)

            // Gold rule inset
            if showsRule {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(PassportPalette.gold.opacity(0.55), lineWidth: 0.8)
                    .padding(12)
                    .padding(insets)
                    .allowsHitTesting(false)
            }

            // Header + footer chrome
            if !blank {
                VStack {
                    HStack {
                        Text(tr("addons.stamp.passport.collection", fallback: "COLLEZIONE"))
                            .font(.system(size: 9, weight: .heavy, design: .serif))
                            .tracking(3)
                            .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.55))
                        Spacer()
                        Text("№ \(String(format: "%03d", totalStamps))")
                            .font(.system(size: 9, weight: .heavy, design: .serif))
                            .tracking(2)
                            .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.55))
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 22)
                    Spacer()
                    Text("· \(pageIndex + 1) / \(max(totalPages, 1)) ·")
                        .font(.system(size: 9, weight: .semibold, design: .serif))
                        .tracking(2)
                        .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.45))
                        .padding(.bottom, 22)
                }
                .padding(insets)
            }

            // Stamps 3×2 grid (6 per page)
            if blank {
                EmptyView()
            } else if chunk.isEmpty {
                emptyPlaceholder
                    .padding(insets)
            } else {
                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(Array(chunk.enumerated()), id: \.element.id) { idx, stamp in
                        let seed = deterministicSeed(stamp.id)
                        let rotation = (seed.angle - 0.5) * 14
                        PassportStampDecal(
                            url: imageURL(for: stamp),
                            size: stampSize,
                            rotation: rotation,
                            showsCancel: (pageIndex + idx) % 3 == 0
                        )
                        .scaleEffect(stampsRevealed ? 1 : 0.7)
                        .opacity(stampsRevealed ? 1 : 0)
                        .animation(
                            .spring(response: 0.55, dampingFraction: 0.78)
                                .delay(Double(min(idx, 6)) * 0.08),
                            value: stampsRevealed
                        )
                        .onTapGesture { onTapStamp(stamp) }
                    }
                }
                .padding(.horizontal, 28)
                .padding(.top, 56)
                .padding(.bottom, 50)
                .padding(insets)
            }

            // SPINE on the bound edge of the page — soft inner shadow + tiny
            // gold thread stitches. NO cobalt, no leather strip.
            spineDecor
                .allowsHitTesting(false)
        }
        .frame(width: width, height: height)
    }

    @ViewBuilder
    private var emptyPlaceholder: some View {
        VStack(spacing: 12) {
            Image(systemName: "seal")
                .font(.system(size: 48))
                .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.45))
            Text(tr("addons.stamp.passport.empty", fallback: "Nessun timbro ancora."))
                .font(.system(size: 13, weight: .semibold, design: .serif))
                .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.7))
            Text(tr("addons.stamp.passport.empty_hint", fallback: "Scansiona ENTRY per iniziare."))
                .font(.system(size: 11, design: .serif))
                .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.5))
        }
        .frame(maxWidth: .infinity)
    }

    private var spineDecor: some View {
        let alongSide = spineEdge == .leading || spineEdge == .trailing
        let (from, to, alignment): (UnitPoint, UnitPoint, Alignment) = switch spineEdge {
        case .leading: (.leading, .trailing, .leading)
        case .trailing: (.trailing, .leading, .trailing)
        case .top: (.top, .bottom, .top)
        case .bottom: (.bottom, .top, .bottom)
        }
        return ZStack(alignment: alignment) {
            // Inner shadow gradient falling onto the page from the spine.
            LinearGradient(colors: [.black.opacity(0.28), .clear], startPoint: from, endPoint: to)

            // Gold thread stitches — three tiny capsules anchored close
            // to the binding edge to suggest the sewing of a real book.
            Group {
                if alongSide {
                    VStack(spacing: 60) { stitch(across: true); stitch(across: true); stitch(across: true) }
                } else {
                    HStack(spacing: 60) { stitch(across: false); stitch(across: false); stitch(across: false) }
                }
            }
            .padding(Edge.Set(spineEdge), 5)
        }
        .frame(width: alongSide ? 22 : nil, height: alongSide ? nil : 22)
        .frame(width: width, height: height, alignment: alignment)
    }

    /// A stitch crosses the spine: horizontal on a side spine, vertical on
    /// one along the top or bottom.
    private func stitch(across horizontal: Bool) -> some View {
        Capsule()
            .fill(PassportPalette.gold.opacity(0.85))
            .frame(width: horizontal ? 6 : 1.4, height: horizontal ? 1.4 : 6)
            .shadow(color: PassportPalette.goldDeep.opacity(0.6), radius: 0.5, x: 0, y: 0.5)
    }

    private func imageURL(for stamp: StampUserModel) -> URL? {
        guard let r = stamp.stampRecord, !r.image.isEmpty else { return nil }
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: PassportLayout.gridThumb)
    }
}

// Deterministic pseudo-random from id.
private struct Seed { let angle: Double; let dx: Double; let dy: Double }

private func deterministicSeed(_ id: String) -> Seed {
    var h: UInt64 = 1469598103934665603
    for b in id.utf8 { h ^= UInt64(b); h &*= 1099511628211 }
    let a = Double(h & 0xFFFF) / Double(0xFFFF)
    let b = Double((h >> 16) & 0xFFFF) / Double(0xFFFF)
    let c = Double((h >> 32) & 0xFFFF) / Double(0xFFFF)
    return Seed(angle: a, dx: b - 0.5, dy: c - 0.5)
}

