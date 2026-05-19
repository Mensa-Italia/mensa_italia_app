import SwiftUI
import Shared

// MARK: - OnboardingView
//
// Motion design principles applied here (sourced from the HyperFrames skill):
//   • Stagger 60-120ms between sibling reveals (no two elements appear at the same instant)
//   • Vary easing across a scene — at least 3 distinct curves
//   • First entrance offset 0.15-0.3s after page lands (never t=0)
//   • Entrance animations only; the page change IS the exit (no fade-outs before transition)
//   • Layout before animation: every element has a defined static position, motion describes the journey
//   • Kinetic typography: per-word reveal, not blob-of-text fade
//   • Hero animations are unique per page — no repeated pattern within the same flow
//
// We don't use TabView(.page) because its binding behavior with `initialPage`
// is unreliable and its transition is generic. Custom carousel with drag-to-page
// gives us 3D rotation + spring + matched coordinated entrance.

struct OnboardingView: View {
    @State private var vm: OnboardingViewModel
    @State private var dragOffset: CGFloat = 0
    @State private var landedPages: Set<Int> = []   // pages whose entrance animation has fired

    init(initialPage: Int = 0, onComplete: @escaping () -> Void) {
        _vm = State(initialValue: OnboardingViewModel(initialPage: initialPage, onComplete: onComplete))
    }

    var body: some View {
        ZStack {
            AnimatedBackdrop(page: vm.currentPage)
            carousel
        }
        .safeAreaInset(edge: .bottom) {
            VStack(spacing: 18) {
                BreathingDots(count: vm.pages.count, current: vm.currentPage)
                ctaButton
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)
            .padding(.top, 8)
        }
        .onAppear {
            // Mark the initial page as "landed" after a beat so the entrance animation runs.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                _ = withAnimation { landedPages.insert(vm.currentPage) }
            }
        }
        .onChange(of: vm.currentPage) { _, newValue in
            // Each page registers its landing — entrance tweens key off `landedPages.contains(i)`
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) {
                _ = withAnimation { landedPages.insert(newValue) }
            }
        }
    }

    // MARK: Carousel

    private var carousel: some View {
        GeometryReader { proxy in
            let pageWidth = proxy.size.width
            HStack(spacing: 0) {
                ForEach(vm.pages.indices, id: \.self) { i in
                    OnboardingPageContainer(
                        page: vm.pages[i],
                        index: i,
                        currentIndex: vm.currentPage,
                        hasLanded: landedPages.contains(i)
                    )
                    .frame(width: pageWidth)
                    .rotation3DEffect(
                        .degrees(rotation(for: i, pageWidth: pageWidth)),
                        axis: (x: 0, y: 1, z: 0),
                        anchor: i < vm.currentPage ? .trailing : .leading,
                        perspective: 0.6
                    )
                    .opacity(opacity(for: i, pageWidth: pageWidth))
                }
            }
            .offset(x: -CGFloat(vm.currentPage) * pageWidth + dragOffset)
            .animation(.spring(response: 0.5, dampingFraction: 0.82), value: vm.currentPage)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 12)
                    .onChanged { value in
                        // resist drag past edges
                        let proposed = value.translation.width
                        let edge = (vm.currentPage == 0 && proposed > 0) ||
                                   (vm.currentPage == vm.pages.count - 1 && proposed < 0)
                        dragOffset = edge ? proposed * 0.25 : proposed
                    }
                    .onEnded { value in
                        let threshold = pageWidth * 0.22
                        let dx = value.translation.width
                        withAnimation(.spring(response: 0.5, dampingFraction: 0.82)) {
                            if dx < -threshold, vm.currentPage < vm.pages.count - 1 {
                                vm.currentPage += 1
                            } else if dx > threshold, vm.currentPage > 0 {
                                vm.currentPage -= 1
                            }
                            dragOffset = 0
                        }
                    }
            )
        }
    }

    private func rotation(for index: Int, pageWidth: CGFloat) -> Double {
        guard pageWidth > 0 else { return 0 }
        let pageDelta = CGFloat(index - vm.currentPage) - dragOffset / pageWidth
        return Double(pageDelta) * -15            // gentle hinge as a page leaves
    }

    private func opacity(for index: Int, pageWidth: CGFloat) -> Double {
        guard pageWidth > 0 else { return 1 }
        let pageDelta = abs(CGFloat(index - vm.currentPage) - dragOffset / pageWidth)
        return Double(max(0, 1 - pageDelta * 0.6))
    }

    // MARK: CTA

    private var ctaButton: some View {
        Button(action: handleCTA) {
            HStack(spacing: 8) {
                Text(vm.isLastPage
                     ? tr("onboarding.cta.start", fallback: "Inizia")
                     : tr("onboarding.cta.continue", fallback: "Continua"))
                    .font(.body.weight(.semibold))
                    .contentTransition(.numericText())
                Image(systemName: vm.isLastPage ? "arrow.right.circle.fill" : "arrow.right")
                    .font(.system(size: 14, weight: .bold))
                    .contentTransition(.symbolEffect(.replace))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .foregroundStyle(.white)
        }
        .buttonStyle(.glassProminent)
        .tint(AppTheme.Colors.mensaBlue)
        .controlSize(.large)
        .animation(.snappy(duration: 0.35), value: vm.isLastPage)
    }

    private func handleCTA() {
        if vm.isLastPage {
            vm.complete()
        } else {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.82)) {
                vm.next()
            }
        }
    }
}

// MARK: - Page Container (hero + kinetic copy)

private struct OnboardingPageContainer: View {
    let page: OnboardingPage
    let index: Int
    let currentIndex: Int
    let hasLanded: Bool

    private var isActive: Bool { index == currentIndex }
    private var showCopy: Bool { hasLanded && isActive }

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            hero
                .frame(height: 280)
                .padding(.bottom, 36)
            kineticTitle
                .padding(.horizontal, 32)
                .padding(.bottom, 14)
            subtitle
                .padding(.horizontal, 36)
            Spacer(minLength: 0)
            Spacer(minLength: 0)
        }
        .padding(.top, 64)
    }

    // MARK: Hero (different per page)

    @ViewBuilder
    private var hero: some View {
        switch index {
        case 0: HeroWelcome(active: isActive && hasLanded)
        case 1: HeroEvents(active: isActive && hasLanded)
        case 2: HeroCard(active: isActive && hasLanded)
        default: HeroSearch(active: isActive && hasLanded)
        }
    }

    // MARK: Kinetic title (per-word stagger)

    private var kineticTitle: some View {
        let words = page.title.split(separator: " ", omittingEmptySubsequences: false).map(String.init)
        return HStack(spacing: 0) {
            // wrap with a custom flow that allows wrapping per word
            FlowingWords(words: words, font: .system(.largeTitle, design: .default).weight(.bold), tracking: 0.4, isVisible: showCopy)
        }
        .frame(maxWidth: .infinity)
        .multilineTextAlignment(.center)
    }

    private var subtitle: some View {
        Text(page.subtitle)
            .font(.title3.weight(.regular))
            .multilineTextAlignment(.center)
            .foregroundStyle(.secondary)
            .opacity(showCopy ? 1 : 0)
            .offset(y: showCopy ? 0 : 16)
            .blur(radius: showCopy ? 0 : 3)
            .animation(
                .spring(response: 0.55, dampingFraction: 0.85).delay(0.45),
                value: showCopy
            )
    }
}

// MARK: - Kinetic Typography

/// Words wrap naturally as a paragraph, but each word fades+rises independently
/// with a stagger. The whole block is centered via FlexibleHStack-like behaviour.
private struct FlowingWords: View {
    let words: [String]
    let font: Font
    let tracking: CGFloat
    let isVisible: Bool

    var body: some View {
        let text = words.enumerated().reduce(Text("")) { partial, pair in
            let (_, w) = pair
            return partial + Text(w + " ")
        }
        // SwiftUI Text concatenation doesn't allow per-word animation, so we
        // use a Layout-friendly approach: render each word as a separate view
        // inside a wrapping HStack via .lineLimit(nil) + FlowLayout helper.
        FlowLayout(spacing: 6, lineSpacing: 6) {
            ForEach(words.indices, id: \.self) { i in
                Text(words[i])
                    .font(font)
                    .tracking(tracking)
                    .foregroundStyle(.primary)
                    .opacity(isVisible ? 1 : 0)
                    .offset(y: isVisible ? 0 : 22)
                    .blur(radius: isVisible ? 0 : 2.5)
                    .animation(
                        .spring(response: 0.55, dampingFraction: 0.82)
                            .delay(0.18 + Double(i) * 0.07),
                        value: isVisible
                    )
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(words.joined(separator: " "))
        .overlay { // hidden full text for screen-reader fallback
            text.opacity(0).accessibilityHidden(true)
        }
    }
}

/// A simple wrapping flow layout (HStack that wraps to next line).
private struct FlowLayout: Layout {
    var spacing: CGFloat = 6
    var lineSpacing: CGFloat = 6

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .greatestFiniteMagnitude
        let rows = layoutRows(in: maxWidth, subviews: subviews)
        let h = rows.map { $0.height }.reduce(0, +) + lineSpacing * CGFloat(max(0, rows.count - 1))
        let w = rows.map { $0.width }.max() ?? 0
        return CGSize(width: w, height: h)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let rows = layoutRows(in: bounds.width, subviews: subviews)
        var y = bounds.minY
        for row in rows {
            let xStart = bounds.minX + (bounds.width - row.width) / 2     // center each row
            var x = xStart
            for item in row.items {
                let size = item.sizeThatFits(.unspecified)
                item.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
                x += size.width + spacing
            }
            y += row.height + lineSpacing
        }
    }

    private struct Row { var items: [LayoutSubviews.Element] = []; var width: CGFloat = 0; var height: CGFloat = 0 }

    private func layoutRows(in maxWidth: CGFloat, subviews: Subviews) -> [Row] {
        var rows: [Row] = [Row()]
        for view in subviews {
            let s = view.sizeThatFits(.unspecified)
            let projected = rows[rows.count - 1].width + s.width + (rows[rows.count - 1].items.isEmpty ? 0 : spacing)
            if projected <= maxWidth || rows[rows.count - 1].items.isEmpty {
                if !rows[rows.count - 1].items.isEmpty { rows[rows.count - 1].width += spacing }
                rows[rows.count - 1].items.append(view)
                rows[rows.count - 1].width += s.width
                rows[rows.count - 1].height = max(rows[rows.count - 1].height, s.height)
            } else {
                var newRow = Row()
                newRow.items.append(view)
                newRow.width = s.width
                newRow.height = s.height
                rows.append(newRow)
            }
        }
        return rows
    }
}

// MARK: - Animated Backdrop

/// Slow-drifting mesh of brand cobalt and cyan. Subtle particle field that
/// drifts upward when the active page changes — "energy passing through".
private struct AnimatedBackdrop: View {
    let page: Int

    var body: some View {
        ZStack {
            Color(.systemGroupedBackground).ignoresSafeArea()

            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { context in
                let t = context.date.timeIntervalSinceReferenceDate
                if #available(iOS 18.0, *) {
                    meshGradient(t: t)
                        .ignoresSafeArea()
                } else {
                    fallbackGradient
                        .ignoresSafeArea()
                }
            }

            // Soft particle drift — Canvas, deterministic seeded
            ParticleDrift(pageSeed: UInt32(page) &* 977 &+ 0xC0DEF00D)
                .blendMode(.plusLighter)
                .allowsHitTesting(false)
        }
    }

    @available(iOS 18.0, *)
    private func meshGradient(t: TimeInterval) -> some View {
        // Two breathing hot spots that slowly rotate
        let s = CGFloat(sin(t * 0.4))
        let c = CGFloat(cos(t * 0.5))
        let blue = AppTheme.Colors.mensaBlue
        let cyan = AppTheme.Colors.mensaCyan
        let ink = AppTheme.Colors.backdropDark
        return MeshGradient(
            width: 3, height: 3,
            points: [
                .init(0.0, 0.0), .init(0.5, 0.0), .init(1.0, 0.0),
                .init(0.0, Float(0.42 + s * 0.05)), .init(Float(0.5 + c * 0.06), 0.45), .init(1.0, Float(0.42 - s * 0.05)),
                .init(0.0, 1.0), .init(0.5, 1.0), .init(1.0, 1.0)
            ],
            colors: [
                ink, blue.opacity(0.55), ink,
                blue.opacity(0.7), cyan.opacity(0.45), blue.opacity(0.6),
                .clear, .clear, .clear
            ],
            smoothsColors: true
        )
        .mask(
            LinearGradient(
                colors: [.black, .black.opacity(0.4), .clear],
                startPoint: .top, endPoint: .bottom
            )
        )
    }

    private var fallbackGradient: some View {
        LinearGradient(
            colors: [
                AppTheme.Colors.backdropDark,
                AppTheme.Colors.mensaBlue.opacity(0.4),
                .clear
            ],
            startPoint: .top, endPoint: .bottom
        )
    }
}

/// Slow particle drift on top of the gradient. Deterministic per page seed
/// so the layout is stable for a given page; switches softly when paged.
private struct ParticleDrift: View {
    let pageSeed: UInt32

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { context in
            Canvas { ctx, size in
                let t = context.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 60)
                var rng = Mulberry32(seed: pageSeed)
                for _ in 0..<28 {
                    let baseX = CGFloat(rng.nextDouble()) * size.width
                    let baseY = CGFloat(rng.nextDouble()) * size.height
                    let drift = CGFloat(sin(t * 0.4 + Double(baseX) * 0.005)) * 14
                    let yShift = CGFloat(t.truncatingRemainder(dividingBy: 6)) * 8
                    let y = (baseY + yShift).truncatingRemainder(dividingBy: size.height)
                    let r = 1.0 + CGFloat(rng.nextDouble()) * 2.5
                    let alpha = 0.06 + CGFloat(rng.nextDouble()) * 0.10
                    let rect = CGRect(x: baseX + drift, y: y, width: r, height: r)
                    ctx.fill(Path(ellipseIn: rect), with: .color(.white.opacity(alpha)))
                }
            }
        }
    }
}

/// Tiny deterministic PRNG so the particle layout is stable across renders.
private struct Mulberry32 {
    var state: UInt32
    init(seed: UInt32) { self.state = seed }
    mutating func nextDouble() -> Double {
        state &+= 0x6D2B79F5
        var z = state
        z = (z ^ (z >> 15)) &* (z | 1)
        z ^= z &+ ((z ^ (z >> 7)) &* (z | 61))
        return Double(z ^ (z >> 14)) / Double(UInt32.max)
    }
}

// MARK: - Hero animations (one per page)

/// Welcome: the official Mensa mark assembles — squircle drops in, glyph
/// emerges with a soft scale, and a cyan halo pulses once it lands.
private struct HeroWelcome: View {
    let active: Bool

    @State private var pulse: CGFloat = 0

    var body: some View {
        ZStack {
            // Halo
            Circle()
                .stroke(AppTheme.Colors.mensaCyan.opacity(0.5), lineWidth: 2)
                .frame(width: 230, height: 230)
                .scaleEffect(active ? 1 + pulse * 0.15 : 0.6)
                .opacity(active ? max(0, 0.7 - Double(pulse)) : 0)
                .blur(radius: 6)
                .onAppear {
                    withAnimation(.easeOut(duration: 2.4).repeatForever(autoreverses: false)) {
                        pulse = 1
                    }
                }

            // MARK:
            MensaMark(size: 180, inBlueBadge: true)
                .shadow(color: AppTheme.Colors.mensaBlue.opacity(0.5), radius: 24, y: 12)
                .scaleEffect(active ? 1 : 0.65)
                .opacity(active ? 1 : 0)
                .rotationEffect(.degrees(active ? 0 : -8))
                .animation(.spring(response: 0.65, dampingFraction: 0.7).delay(0.05), value: active)
        }
    }
}

/// Events: a calendar tile with date dots pulsing in sequence around it,
/// suggesting events distributed across time and place.
private struct HeroEvents: View {
    let active: Bool

    @State private var bounce = false

    var body: some View {
        ZStack {
            // Orbiting event dots
            ForEach(0..<5, id: \.self) { i in
                let angle = Double(i) / 5 * .pi * 2 - .pi / 2
                let r: CGFloat = 130
                Circle()
                    .fill(AppTheme.Colors.mensaCyan)
                    .frame(width: 14, height: 14)
                    .shadow(color: AppTheme.Colors.mensaCyan.opacity(0.7), radius: 6)
                    .offset(x: cos(angle) * r, y: sin(angle) * r)
                    .scaleEffect(active ? 1 : 0)
                    .opacity(active ? 1 : 0)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.7)
                            .delay(0.35 + Double(i) * 0.08),
                        value: active
                    )
            }

            // Center calendar tile
            ZStack {
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .fill(.regularMaterial)
                    .frame(width: 160, height: 160)
                    .overlay {
                        RoundedRectangle(cornerRadius: 32, style: .continuous)
                            .strokeBorder(AppTheme.Colors.mensaCyan.opacity(0.35), lineWidth: 1)
                    }
                    .shadow(color: AppTheme.Colors.mensaBlue.opacity(0.25), radius: 18, y: 8)

                Image(systemName: "calendar.badge.plus")
                    .font(.system(size: 78, weight: .light))
                    .foregroundStyle(AppTheme.Colors.mensaCyan, AppTheme.Colors.mensaBlue)
                    .symbolRenderingMode(.palette)
                    .symbolEffect(.bounce, options: .repeat(.continuous), value: bounce)
            }
            .scaleEffect(active ? 1 : 0.8)
            .opacity(active ? 1 : 0)
            .animation(.spring(response: 0.55, dampingFraction: 0.78).delay(0.15), value: active)
            .onChange(of: active) { _, newValue in
                if newValue {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { bounce.toggle() }
                }
            }
        }
    }
}

/// Card: a Mensa-branded card lifts in from below with a Liquid Glass tilt
/// and a tiny QR-mesh pattern draws itself onto it.
/// Layout: everything is centered inside a single ZStack of fixed-size card.
/// Internal content is laid out with HStack/VStack inside the card frame, so
/// nothing relies on absolute positions and the whole hero centers correctly
/// inside its 280pt height slot.
private struct HeroCard: View {
    let active: Bool

    @State private var qrProgress: CGFloat = 0

    private let cardWidth: CGFloat = 260
    private let cardHeight: CGFloat = 162

    var body: some View {
        ZStack {
            // Subtle ground shadow under the card (offset down so it reads as floor)
            Ellipse()
                .fill(AppTheme.Colors.mensaBlue.opacity(0.45))
                .frame(width: 220, height: 28)
                .blur(radius: 14)
                .offset(y: cardHeight * 0.55)
                .opacity(active ? 0.85 : 0)
                .animation(.easeOut(duration: 0.6).delay(0.2), value: active)

            // The card
            cardSurface
                .frame(width: cardWidth, height: cardHeight)
                .rotation3DEffect(
                    .degrees(active ? -8 : -22),
                    axis: (x: 1, y: -0.4, z: 0),
                    perspective: 0.5
                )
                .scaleEffect(active ? 1 : 0.85)
                .offset(y: active ? 0 : 70)
                .opacity(active ? 1 : 0)
                .shadow(color: AppTheme.Colors.mensaBlue.opacity(0.4), radius: 20, y: 14)
                .animation(.spring(response: 0.6, dampingFraction: 0.78).delay(0.12), value: active)
        }
        .frame(maxWidth: .infinity)
        .onChange(of: active) { _, newValue in
            if newValue {
                qrProgress = 0
                withAnimation(.easeInOut(duration: 0.9).delay(0.65)) { qrProgress = 1 }
            } else {
                qrProgress = 0
            }
        }
    }

    private var cardSurface: some View {
        HStack(alignment: .top, spacing: 14) {
            // Left half: mark + wordmark
            VStack(alignment: .leading, spacing: 6) {
                MensaMark(size: 30, inBlueBadge: false)
                Spacer(minLength: 0)
                VStack(alignment: .leading, spacing: 2) {
                    Text("MENSA")
                        .font(.system(size: 18, weight: .heavy))
                        .tracking(2)
                    Text("ITALIA")
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(4)
                        .opacity(0.75)
                }
                .foregroundStyle(.white)
            }
            Spacer(minLength: 0)
            // Right half: QR mesh
            QRMesh(progress: qrProgress)
                .frame(width: 56, height: 56)
                .foregroundStyle(.white)
                .padding(.top, 4)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [AppTheme.Colors.mensaBlueDeep, AppTheme.Colors.mensaBlue],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .strokeBorder(.white.opacity(0.18), lineWidth: 1)
                )
        )
    }
}

private struct QRMesh: View {
    var progress: CGFloat   // 0...1

    private let cells: [[Bool]] = [
        [true, true, true, false, true, true, true],
        [true, false, true, false, false, false, true],
        [true, true, true, false, true, false, false],
        [false, false, false, true, false, true, true],
        [true, false, true, true, true, false, false],
        [false, true, false, false, false, true, true],
        [true, true, true, false, true, true, false]
    ]

    var body: some View {
        GeometryReader { geo in
            let cell = geo.size.width / CGFloat(cells.count)
            ZStack {
                ForEach(0..<cells.count, id: \.self) { row in
                    ForEach(0..<cells[row].count, id: \.self) { col in
                        if cells[row][col] {
                            let idx = row * cells[0].count + col
                            let total = cells.count * cells[0].count
                            let reveal = min(1, max(0, progress * CGFloat(total) - CGFloat(idx)))
                            Rectangle()
                                .frame(width: cell * 0.9, height: cell * 0.9)
                                .position(x: cell * (CGFloat(col) + 0.5),
                                          y: cell * (CGFloat(row) + 0.5))
                                .opacity(Double(reveal))
                                .scaleEffect(reveal)
                        }
                    }
                }
            }
        }
    }
}

/// Search: a glass search-field with chip results materializing inside it.
/// Composition: the field is the hero — it sits centered with the magnifier
/// icon on the left and the result chips flowing in from the right. Reads
/// like a live search inside a real search bar.
private struct HeroSearch: View {
    let active: Bool

    @State private var sweep = false

    var body: some View {
        VStack(spacing: 14) {
            searchField
            chipResults
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 24)
        .onChange(of: active) { _, newValue in
            if newValue {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.55) { sweep.toggle() }
            }
        }
    }

    private var searchField: some View {
        HStack(spacing: 12) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 22, weight: .semibold))
                .foregroundStyle(AppTheme.Colors.mensaCyan)
                .symbolEffect(.variableColor.iterative.reversing, options: .repeat(.continuous), value: sweep)
            // animated typing cursor
            HStack(spacing: 1) {
                Text("Mensa")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.white)
                Rectangle()
                    .fill(AppTheme.Colors.mensaCyan)
                    .frame(width: 2, height: 18)
                    .opacity(sweep ? 0.2 : 1)
                    .animation(.easeInOut(duration: 0.5).repeatForever(autoreverses: true), value: sweep)
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 18)
        .frame(height: 56)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .strokeBorder(AppTheme.Colors.mensaCyan.opacity(0.4), lineWidth: 1)
                )
        )
        .shadow(color: AppTheme.Colors.mensaBlue.opacity(0.35), radius: 18, y: 8)
        .scaleEffect(active ? 1 : 0.9)
        .opacity(active ? 1 : 0)
        .animation(.spring(response: 0.55, dampingFraction: 0.78).delay(0.12), value: active)
    }

    private var chipResults: some View {
        VStack(alignment: .leading, spacing: 8) {
            chip(label: tr("onboarding.search.chip.events", fallback: "Eventi · 12"), iconColor: AppTheme.Colors.mensaCyan, delay: 0.32)
            chip(label: tr("onboarding.search.chip.deals", fallback: "Convenzioni · 7"), iconColor: .white.opacity(0.9), delay: 0.42)
            chip(label: tr("onboarding.search.chip.people", fallback: "Persone · 3"), iconColor: AppTheme.Colors.mensaCyan, delay: 0.52)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 6)
    }

    private func chip(label: String, iconColor: Color, delay: Double) -> some View {
        HStack(spacing: 8) {
            Circle()
                .fill(iconColor)
                .frame(width: 6, height: 6)
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.white.opacity(0.92))
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12).padding(.vertical, 9)
        .background(
            Capsule()
                .fill(.ultraThinMaterial)
                .overlay(Capsule().strokeBorder(.white.opacity(0.14), lineWidth: 1))
        )
        .opacity(active ? 1 : 0)
        .offset(x: active ? 0 : 30)
        .blur(radius: active ? 0 : 4)
        .animation(.spring(response: 0.5, dampingFraction: 0.82).delay(delay), value: active)
    }
}

// MARK: - Breathing Dots

/// Capsule indicator where the current dot "breathes" via a subtle scale loop.
struct BreathingDots: View {
    let count: Int
    let current: Int

    var body: some View {
        HStack(spacing: 8) {
            ForEach(0..<count, id: \.self) { i in
                Capsule()
                    .fill(i == current
                          ? AnyShapeStyle(AppTheme.Colors.mensaBlue.gradient)
                          : AnyShapeStyle(Color.secondary.opacity(0.32)))
                    .frame(width: i == current ? 26 : 7, height: 7)
                    .shadow(color: i == current ? AppTheme.Colors.mensaBlue.opacity(0.6) : .clear,
                            radius: i == current ? 6 : 0)
                    .animation(.spring(response: 0.45, dampingFraction: 0.72), value: current)
            }
        }
    }
}

// MARK: - Preview

#Preview("Page 1") { OnboardingView(initialPage: 0, onComplete: {}) }
#Preview("Page 2") { OnboardingView(initialPage: 1, onComplete: {}) }
#Preview("Page 3") { OnboardingView(initialPage: 2, onComplete: {}) }
#Preview("Page 4 (last)") { OnboardingView(initialPage: 3, onComplete: {}) }
