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
struct PassportInside: View {
    let width: CGFloat
    let height: CGFloat
    let stamps: [StampUserModel]
    let visible: Bool
    let stampsRevealed: Bool
    let onTapStamp: (StampUserModel) -> Void
    @Binding var currentPage: Int
    @Binding var totalPages: Int

    private let stampsPerPage = 6

    @State private var dragAngle: Double = 0   // -180...0 forward, 0...180 backward
    @State private var isDragging: Bool = false
    @State private var hasInteracted: Bool = false
    @State private var hintPhase: Int = 0       // 0 idle, 1 peek up
    @State private var hintTask: Task<Void, Never>?
    @State private var didHapticAt90: Bool = false

    private var pages: [[StampUserModel]] {
        guard !stamps.isEmpty else { return [[]] }
        return stride(from: 0, to: stamps.count, by: stampsPerPage)
            .map { Array(stamps[$0..<min($0 + stampsPerPage, stamps.count)]) }
    }

    private var hasNext: Bool { currentPage < pages.count - 1 }
    private var hasPrev: Bool { currentPage > 0 }

    var body: some View {
        ZStack {
            // Underlay = the destination page. It's the page that the user
            // will see once the current sheet has flipped away.
            //   forward (dragAngle < 0)  → next page underneath
            //   backward (dragAngle > 0) → previous page underneath (covered
            //     by the returning sheet)
            //   idle                     → current page (stable)
            Group {
                if dragAngle < 0, hasNext {
                    pageView(at: currentPage + 1)
                } else if dragAngle > 0, hasPrev {
                    pageView(at: currentPage - 1)
                } else {
                    pageView(at: currentPage)
                }
            }
            .frame(width: width, height: height)

            // Flipping page = the sheet currently being turned. Pivots on
            // the LEFT EDGE (the spine of the passport).
            if dragAngle != 0 || isDragging || hintPhase != 0 {
                flippingPage
            }

            // Edge curl hint on the bottom-right corner (forward) and a
            // smaller one on bottom-left if a previous page exists.
            edgeCurlHints

            // Tap zones near outer edges for tap-to-turn fallback.
            HStack(spacing: 0) {
                Color.clear
                    .frame(width: 28)
                    .contentShape(Rectangle())
                    .onTapGesture { turnBackward() }
                Spacer()
                Color.clear
                    .frame(width: 28)
                    .contentShape(Rectangle())
                    .onTapGesture { turnForward() }
            }
            .frame(width: width, height: height)
        }
        .frame(width: width, height: height)
        .gesture(flipDrag)
        .onChange(of: visible) { _, newValue in
            if newValue { scheduleHint() } else { cancelHint() }
        }
        .onChange(of: pages.count) { _, newValue in
            if totalPages != newValue { totalPages = newValue }
        }
        .onAppear {
            if totalPages != pages.count { totalPages = pages.count }
            if visible { scheduleHint() }
        }
        .onDisappear { cancelHint() }
    }

    // MARK: - Page view

    @ViewBuilder
    private func pageView(at index: Int) -> some View {
        let safeIndex = max(0, min(index, max(pages.count - 1, 0)))
        let chunk = pages.indices.contains(safeIndex) ? pages[safeIndex] : []
        let revealed = stampsRevealed || safeIndex != currentPage
        SinglePassportPage(
            width: width,
            height: height,
            pageIndex: safeIndex,
            totalPages: pages.count,
            chunk: chunk,
            totalStamps: stamps.count,
            stampsRevealed: revealed,
            onTapStamp: onTapStamp
        )
    }

    // MARK: - Flipping page (true book flip, anchor LEFT)

    private var flippingPage: some View {
        let angle: Double
        let frontIndex: Int
        let backIndex: Int

        if dragAngle < 0 {
            // Forward: current page lifts from the right, rotates around
            // the left edge from 0° to -180°. Front = current. Back face
            // shows the *next* page (mirrored), so the user sees a real
            // sheet flipping over onto the next.
            angle = dragAngle
            frontIndex = currentPage
            backIndex  = min(currentPage + 1, max(pages.count - 1, 0))
        } else if dragAngle > 0 {
            // Backward: previous page comes BACK from behind the spine.
            // Render at angle (dragAngle - 180) so it sweeps -180° → 0°.
            // Front = previous (visible once we cross 90°). Back face shows
            // the current page (mirrored) while it's still partly turned.
            angle = dragAngle - 180
            frontIndex = currentPage - 1
            backIndex  = currentPage
        } else {
            // Hint peek
            angle = (hintPhase == 1) ? -14 : 0
            frontIndex = currentPage
            backIndex  = min(currentPage + 1, max(pages.count - 1, 0))
        }

        let absAngle = abs(angle)
        let showingFront = absAngle < 90
        let lift = min(absAngle / 180.0, 1.0)

        return ZStack {
            // FRONT face (0..90°)
            pageView(at: max(frontIndex, 0))
                .overlay(
                    LinearGradient(
                        colors: [.black.opacity(lift * 0.28), .clear],
                        startPoint: .leading, endPoint: .trailing
                    )
                    .allowsHitTesting(false)
                )
                .opacity(showingFront ? 1 : 0)

            // BACK face (90..180°) — the next page peeking through, mirrored
            // so when rotation passes 180° it reads correctly.
            pageView(at: max(backIndex, 0))
                .overlay(
                    Rectangle()
                        .fill(.black.opacity(0.06))
                )
                .scaleEffect(x: -1, y: 1)
                .opacity(showingFront ? 0 : 1)
        }
        .frame(width: width, height: height)
        .rotation3DEffect(
            .degrees(angle),
            axis: (x: 0, y: 1, z: 0),
            anchor: .leading,
            perspective: 0.6
        )
        .shadow(color: .black.opacity(lift * 0.42),
                radius: 16, x: 6, y: 10)
    }

    // MARK: - Edge curl hints

    @ViewBuilder
    private var edgeCurlHints: some View {
        ZStack {
            if hasNext {
                PageCurlCorner(size: hintPhase != 0 ? 36 : 32,
                               opacity: hintPhase != 0 ? 0.9 : 0.5,
                               corner: .bottomTrailing)
                    .frame(width: width, height: height, alignment: .bottomTrailing)
                    .opacity(hasInteracted ? 0 : 1)
                    .animation(.easeOut(duration: 0.3), value: hasInteracted)
                    .onTapGesture { turnForward() }
            }
            if hasPrev {
                PageCurlCorner(size: 26, opacity: 0.4, corner: .bottomLeading)
                    .frame(width: width, height: height, alignment: .bottomLeading)
                    .onTapGesture { turnBackward() }
            }
        }
        .frame(width: width, height: height)
        .allowsHitTesting(true)
    }

    // MARK: - Gestures

    private var flipDrag: some Gesture {
        DragGesture(minimumDistance: 8)
            .onChanged { value in
                if !isDragging { isDragging = true }
                if !hasInteracted { hasInteracted = true; cancelHint() }
                let dx = value.translation.width
                let raw = Double(dx / width) * 180.0
                let clamped: Double
                if raw < 0 {
                    clamped = hasNext ? max(raw, -180) : max(raw * 0.25, -25)
                } else {
                    clamped = hasPrev ? min(raw, 180) : min(raw * 0.25, 25)
                }
                dragAngle = clamped
                if !didHapticAt90, abs(clamped) > 90 {
                    didHapticAt90 = true
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                }
            }
            .onEnded { value in
                let dx = value.translation.width
                let vx = value.predictedEndTranslation.width - value.translation.width
                let pct = abs(dx) / width
                let fast = abs(vx) > 80
                let shouldComplete = pct > 0.45 || fast

                isDragging = false
                didHapticAt90 = false

                if shouldComplete {
                    if dx < 0, hasNext {
                        completeFlip(to: -180) { currentPage += 1; dragAngle = 0 }
                    } else if dx > 0, hasPrev {
                        completeFlip(to: 180) { currentPage -= 1; dragAngle = 0 }
                    } else {
                        snapBack()
                    }
                } else {
                    snapBack()
                }
            }
    }

    private func turnForward() {
        guard hasNext else { return }
        hasInteracted = true; cancelHint()
        completeFlip(to: -180) { currentPage += 1; dragAngle = 0 }
    }

    private func turnBackward() {
        guard hasPrev else { return }
        hasInteracted = true; cancelHint()
        completeFlip(to: 180) { currentPage -= 1; dragAngle = 0 }
    }

    private func completeFlip(to target: Double, finalize: @escaping () -> Void) {
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        withAnimation(.spring(response: 0.55, dampingFraction: 0.78)) {
            dragAngle = target
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            finalize()
        }
    }

    private func snapBack() {
        withAnimation(.spring(response: 0.45, dampingFraction: 0.8)) {
            dragAngle = 0
        }
    }

    // MARK: - Hint orchestration

    private func scheduleHint() {
        guard !hasInteracted, pages.count > 1 else { return }
        cancelHint()
        hintTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 600_000_000)
            if hasInteracted { return }
            withAnimation(.easeOut(duration: 0.6)) { hintPhase = 1 }
            try? await Task.sleep(nanoseconds: 600_000_000)
            if hasInteracted { return }
            withAnimation(.easeInOut(duration: 0.45)) { hintPhase = 0 }
        }
    }

    private func cancelHint() {
        hintTask?.cancel(); hintTask = nil
        if hintPhase != 0 {
            withAnimation(.easeOut(duration: 0.3)) { hintPhase = 0 }
        }
    }
}

// MARK: - SinglePassportPage (one parchment page, 6 stamps)

private struct SinglePassportPage: View {
    let width: CGFloat
    let height: CGFloat
    let pageIndex: Int
    let totalPages: Int
    let chunk: [StampUserModel]
    let totalStamps: Int
    let stampsRevealed: Bool
    let onTapStamp: (StampUserModel) -> Void

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        ZStack {
            // Parchment background
            RoundedRectangle(cornerRadius: 18, style: .continuous)
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
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .allowsHitTesting(false)

            // Watermark "M"
            Text("M")
                .font(.system(size: width * 0.7, weight: .black, design: .serif))
                .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.05))
                .allowsHitTesting(false)

            // Gold rule inset
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(PassportPalette.gold.opacity(0.55), lineWidth: 0.8)
                .padding(12)
                .allowsHitTesting(false)

            // Header + footer chrome
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

            // Stamps 3×2 grid (6 per page)
            if chunk.isEmpty {
                emptyPlaceholder
            } else {
                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(Array(chunk.enumerated()), id: \.element.id) { idx, stamp in
                        let seed = deterministicSeed(stamp.id)
                        let rotation = (seed.angle - 0.5) * 14
                        PassportStampDecal(
                            url: imageURL(for: stamp),
                            size: (width - 76) / 2,
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
            }

            // SPINE on the LEFT edge of the page — soft inner shadow + tiny
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
        HStack(spacing: 0) {
            ZStack {
                // Inner shadow gradient falling onto the page from the spine.
                LinearGradient(
                    colors: [.black.opacity(0.28), .clear],
                    startPoint: .leading, endPoint: .trailing
                )
                .frame(width: 22)

                // Gold thread stitches — three tiny capsules anchored close
                // to the binding edge to suggest the sewing of a real book.
                VStack(spacing: 60) {
                    stitch()
                    stitch()
                    stitch()
                }
                .padding(.leading, 5)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(width: 22)
            Spacer()
        }
        .frame(width: width, height: height)
    }

    private func stitch() -> some View {
        Capsule()
            .fill(PassportPalette.gold.opacity(0.85))
            .frame(width: 6, height: 1.4)
            .shadow(color: PassportPalette.goldDeep.opacity(0.6), radius: 0.5, x: 0, y: 0.5)
    }

    private func imageURL(for stamp: StampUserModel) -> URL? {
        guard let r = stamp.stampRecord, !r.image.isEmpty else { return nil }
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: "600x400")
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

// MARK: - Page curl corner (decorative + tap target)

private struct PageCurlCorner: View {
    let size: CGFloat
    let opacity: Double
    enum Corner { case bottomTrailing, bottomLeading }
    let corner: Corner

    var body: some View {
        Canvas { ctx, canvasSize in
            var path = Path()
            switch corner {
            case .bottomTrailing:
                path.move(to: CGPoint(x: canvasSize.width, y: canvasSize.height))
                path.addLine(to: CGPoint(x: canvasSize.width - size, y: canvasSize.height))
                path.addQuadCurve(
                    to: CGPoint(x: canvasSize.width, y: canvasSize.height - size),
                    control: CGPoint(x: canvasSize.width - size * 0.4,
                                     y: canvasSize.height - size * 0.4)
                )
            case .bottomLeading:
                path.move(to: CGPoint(x: 0, y: canvasSize.height))
                path.addLine(to: CGPoint(x: size, y: canvasSize.height))
                path.addQuadCurve(
                    to: CGPoint(x: 0, y: canvasSize.height - size),
                    control: CGPoint(x: size * 0.4, y: canvasSize.height - size * 0.4)
                )
            }
            path.closeSubpath()
            let grad = Gradient(colors: [
                PassportPalette.parchmentEdge.opacity(opacity),
                PassportPalette.parchment.opacity(opacity * 0.6),
                Color.black.opacity(opacity * 0.35)
            ])
            ctx.fill(path, with: .linearGradient(
                grad,
                startPoint: corner == .bottomTrailing
                    ? CGPoint(x: canvasSize.width - size, y: canvasSize.height - size)
                    : CGPoint(x: size, y: canvasSize.height - size),
                endPoint: corner == .bottomTrailing
                    ? CGPoint(x: canvasSize.width, y: canvasSize.height)
                    : CGPoint(x: 0, y: canvasSize.height)
            ))
            ctx.stroke(path, with: .color(PassportPalette.goldDeep.opacity(opacity * 0.5)),
                       lineWidth: 0.6)
        }
        .frame(width: size, height: size)
        .allowsHitTesting(true)
        .contentShape(Rectangle())
    }
}
