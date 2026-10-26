import SwiftUI

/// The medal collection as a museum room: each medal floats in its own glass
/// case, on a stone pedestal with a brass plaque, under a spotlight. Earned
/// medals are lit; the rest wait in the dark as dull silhouettes.
struct MedalCabinetView: View {
    @State private var store = MedalStore.shared
    @State private var selected: Medal?

    private var medals: [Medal] { store.medals }

    private var earnedCount: Int { medals.filter(\.isEarned).count }

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                if let hinge = geo.verticalHinge {
                    foldedOpen(width: geo.size.width, hinge: hinge)
                } else {
                    VStack(spacing: 30) {
                        header
                        LazyVGrid(
                            columns: [GridItem(.adaptive(minimum: 150, maximum: 210), spacing: 22)],
                            spacing: 36
                        ) {
                            ForEach(medals) { caseButton($0) }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 40)
                }
            }
        }
        .background(MuseumBackdrop().ignoresSafeArea())
        .environment(\.colorScheme, .dark)
        // Kept for the back button's menu and VoiceOver, not shown: the
        // room speaks for itself.
        .navigationTitle(tr("medals.title", fallback: "Medagliere"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { Color.clear.frame(width: 1, height: 1) }
        }
        .toolbarColorScheme(.dark, for: .navigationBar)
        .sheet(item: $selected) { medal in
            MedalShowcaseView(medal: medal)
        }
        .overlay {
            if medals.isEmpty {
                if store.failed {
                    ContentUnavailableView(
                        tr("medals.title", fallback: "Medagliere"),
                        systemImage: "medal",
                        description: Text(tr("medals.load_error", fallback: "Non riesco a leggere le medaglie. Riprova più tardi."))
                    )
                } else {
                    ProgressView().tint(.white)
                }
            }
        }
        .task { await store.refresh() }
    }

    /// iPhone Duo unfolded: each row is laid out half by half, with the
    /// hinge as the gap between them, so no case is creased by it; the header
    /// sits in the left half rather than across the fold.
    private func foldedOpen(width: CGFloat, hinge: CGRect) -> some View {
        let margin: CGFloat = 20
        let spacing: CGFloat = 22
        let leftWidth = hinge.minX - margin * 2
        let rightWidth = width - hinge.maxX - margin * 2
        let perSide = max(1, Int((min(leftWidth, rightWidth) + spacing) / (150 + spacing)))
        let rows = stride(from: 0, to: medals.count, by: perSide * 2).map {
            Array(medals[$0..<min($0 + perSide * 2, medals.count)])
        }
        // One size for every case, from the narrower half (the right one
        // gives up room to the vertical bar).
        let caseWidth = (min(leftWidth, rightWidth) - spacing * CGFloat(perSide - 1)) / CGFloat(perSide)
        func side(_ items: ArraySlice<Medal>, width: CGFloat) -> some View {
            HStack(alignment: .top, spacing: spacing) {
                ForEach(Array(items)) { caseButton($0).frame(width: caseWidth) }
            }
            .frame(width: width)
        }
        return VStack(spacing: 36) {
            HStack(spacing: 0) {
                header.frame(width: leftWidth).padding(.horizontal, margin)
                Spacer(minLength: 0)
            }
            ForEach(rows.indices, id: \.self) { index in
                let row = rows[index]
                HStack(spacing: 0) {
                    side(row.prefix(perSide), width: leftWidth).padding(.horizontal, margin)
                    Color.clear.frame(width: hinge.width)
                    side(row.dropFirst(perSide), width: rightWidth).padding(.horizontal, margin)
                }
            }
        }
        .padding(.top, 8)
        .padding(.bottom, 40)
    }

    private func caseButton(_ medal: Medal) -> some View {
        Button {
            selected = medal
        } label: {
            MedalDisplayCase(medal: medal)
        }
        .buttonStyle(CasePressStyle())
        .accessibilityLabel(Text(medal.title))
        .accessibilityValue(Text(medal.isEarned
            ? tr("medals.earned", fallback: "Ottenuta")
            : tr("medals.locked", fallback: "Non ancora ottenuta")))
    }

    private var header: some View {
        VStack(spacing: 10) {
            // The app's own face, as in Today's greeting; the brass is
            // what ties it to the room.
            Text("\(earnedCount) / \(medals.count)")
                .font(.system(size: 46, weight: .heavy).monospacedDigit())
                .foregroundStyle(LinearGradient(
                    colors: [Brass.hi, Brass.mid],
                    startPoint: .top,
                    endPoint: .bottom
                ))
                .shadow(color: .black.opacity(0.6), radius: 0, x: 0, y: 1)
            Text(tr("medals.count_caption", fallback: "Medaglie in teca").uppercased())
                .font(.caption.weight(.semibold))
                .tracking(2.5)
                .foregroundStyle(.white.opacity(0.5))
            HStack(spacing: 18) {
                ForEach(Medal.Tier.allCases, id: \.self) { tier in
                    HStack(spacing: 6) {
                        MedalArtwork(tier: tier, symbol: "star.fill", size: 18)
                        Text("\(medals.filter { $0.tier == tier && $0.isEarned }.count)")
                            .font(.footnote.weight(.semibold).monospacedDigit())
                            .foregroundStyle(.white.opacity(0.75))
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel(Text(tier.name))
                }
            }
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
    }
}

/// A pressed case sinks a touch, as if set back on its pedestal.
private struct CasePressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

// MARK: - Case

/// One medal in its glass case on a pedestal.
///
/// Drawn to stay cheap with dozens on screen: the medal is flattened into
/// one layer and its bob is a repeating animation the render server plays,
/// so nothing here is recomputed frame by frame; soft light and shadows are
/// gradients rather than blurs.
struct MedalDisplayCase: View {
    let medal: Medal

    var body: some View {
        VStack(spacing: 0) {
            vitrine
                .frame(height: 168)
                .padding(.horizontal, 8)
            MedalPedestal(plaque: medal.title)
        }
    }

    private var vitrine: some View {
        ZStack(alignment: .bottom) {
            if medal.isEarned {
                // Light pooling down from the top of the case: soft all
                // round without a blur.
                EllipticalGradient(
                    colors: [Color(red: 1, green: 0.95, blue: 0.82).opacity(0.26), .clear],
                    center: .top,
                    startRadiusFraction: 0,
                    endRadiusFraction: 0.75
                )
            }

            FloatingMedal(medal: medal, size: 84)
                .frame(maxHeight: .infinity)

            GlassCase()
        }
    }
}

/// The medal hovering above the pedestal, bobbing slowly, its shadow on the
/// floor of the case shrinking as it rises.
private struct FloatingMedal: View {
    let medal: Medal
    let size: CGFloat

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var lifted = false

    /// So no two medals bob in step: 0…2.4 s.
    private var delay: Double {
        Double(medal.id.unicodeScalars.reduce(0) { $0 &+ Int($1.value) } % 240) / 100
    }

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 0)
            MedalArtwork(
                tier: medal.tier,
                symbol: medal.symbol,
                size: size,
                locked: !medal.isEarned
            )
            .drawingGroup()
            .shadow(color: .black.opacity(0.45), radius: 6, y: 5)
            .offset(y: lifted ? -6 : 0)
            Spacer(minLength: 0)
            Ellipse()
                .fill(RadialGradient(
                    colors: [.black.opacity(medal.isEarned ? 0.55 : 0.35), .clear],
                    center: .center,
                    startRadius: 0,
                    endRadius: size * 0.34
                ))
                .frame(width: size * 0.8, height: 12)
                .scaleEffect(x: lifted ? 0.88 : 1)
                .padding(.bottom, 2)
        }
        .onAppear {
            guard !reduceMotion else { return }
            withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true).delay(delay)) {
                lifted = true
            }
        }
    }
}

/// The glass box: faintly tinted, lit along its edges, with reflections.
struct GlassCase: View {
    var cornerRadius: CGFloat = 12

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        shape
            .fill(LinearGradient(
                colors: [.white.opacity(0.10), .white.opacity(0.03), .white.opacity(0.06)],
                startPoint: .top,
                endPoint: .bottom
            ))
            .overlay {
                // Reflections: a broad streak and a thin one, across the pane.
                GeometryReader { geo in
                    let w = geo.size.width
                    ZStack {
                        Rectangle()
                            .fill(LinearGradient(colors: [.clear, .white.opacity(0.16), .clear], startPoint: .leading, endPoint: .trailing))
                            .frame(width: w * 0.22)
                            .offset(x: -w * 0.18)
                        Rectangle()
                            .fill(.white.opacity(0.10))
                            .frame(width: w * 0.04)
                            .offset(x: w * 0.06)
                    }
                    .frame(width: w, height: geo.size.height * 1.6)
                    .rotationEffect(.degrees(22))
                    .position(x: w / 2, y: geo.size.height / 2)
                }
                .clipShape(shape)
                .allowsHitTesting(false)
            }
            // Edges catching the light: bright on top, fading down.
            .overlay(
                shape.strokeBorder(
                    LinearGradient(
                        colors: [.white.opacity(0.6), .white.opacity(0.18), .white.opacity(0.08)],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 1
                )
            )
            // The top pane, seen edge-on.
            .overlay(alignment: .top) {
                Capsule()
                    .fill(.white.opacity(0.35))
                    .frame(height: 1.5)
                    .padding(.horizontal, cornerRadius)
                    .padding(.top, 2)
            }
            .allowsHitTesting(false)
    }
}

/// Light falling from a narrow source above onto the pedestal.
struct SpotlightCone: Shape {
    func path(in rect: CGRect) -> Path {
        Path { path in
            path.move(to: CGPoint(x: rect.midX - rect.width * 0.1, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.midX + rect.width * 0.1, y: rect.minY))
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))
            path.addLine(to: CGPoint(x: rect.minX, y: rect.maxY))
            path.closeSubpath()
        }
    }
}

// MARK: - Pedestal

enum Brass {
    static let hi = Color(red: 0.95, green: 0.84, blue: 0.56)
    static let mid = Color(red: 0.72, green: 0.57, blue: 0.29)
    static let lo = Color(red: 0.42, green: 0.31, blue: 0.12)
}

private enum Stone {
    static let hi = Color(white: 0.34)
    static let mid = Color(white: 0.21)
    static let lo = Color(white: 0.11)
}

/// Dark stone: a lip the case stands on, the block with its brass plaque, a
/// wider base. `scale` sizes it for the showcase.
struct MedalPedestal: View {
    let plaque: String
    var scale: CGFloat = 1

    var body: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 3 * scale, style: .continuous)
                .fill(LinearGradient(colors: [Stone.hi, Stone.mid], startPoint: .top, endPoint: .bottom))
                .frame(height: 9 * scale)
            ZStack {
                Rectangle()
                    .fill(LinearGradient(colors: [Stone.mid, Stone.lo], startPoint: .top, endPoint: .bottom))
                    // Rounded sides: light on the left face, dark on the right.
                    .overlay(LinearGradient(
                        colors: [.white.opacity(0.07), .clear, .black.opacity(0.25)],
                        startPoint: .leading,
                        endPoint: .trailing
                    ))
                    // The lip's shadow on the block.
                    .overlay(alignment: .top) {
                        LinearGradient(colors: [.black.opacity(0.45), .clear], startPoint: .top, endPoint: .bottom)
                            .frame(height: 6 * scale)
                    }
                BrassPlaque(text: plaque, scale: scale)
                    .padding(.horizontal, 14 * scale)
            }
            .frame(height: 46 * scale)
            .padding(.horizontal, 8 * scale)
            RoundedRectangle(cornerRadius: 3 * scale, style: .continuous)
                .fill(LinearGradient(colors: [Stone.mid, Stone.lo], startPoint: .top, endPoint: .bottom))
                .frame(height: 9 * scale)
                .padding(.horizontal, -4 * scale)
                .shadow(color: .black.opacity(0.6), radius: 8 * scale, y: 6 * scale)
        }
    }
}

/// An engraved brass plate with two screws.
private struct BrassPlaque: View {
    let text: String
    let scale: CGFloat

    var body: some View {
        HStack(spacing: 0) {
            screw
            Text(text.uppercased())
                .font(.system(size: 8.5 * scale, weight: .bold, design: .serif))
                .tracking(1 * scale)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(Brass.lo)
                // Engraved: the lower lip of each letter catches the light.
                .shadow(color: Brass.hi.opacity(0.9), radius: 0, x: 0, y: 0.6 * scale)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 4 * scale)
            screw
        }
        .padding(.horizontal, 4 * scale)
        .frame(height: 20 * scale)
        .background(
            RoundedRectangle(cornerRadius: 2 * scale, style: .continuous)
                .fill(LinearGradient(colors: [Brass.hi, Brass.mid], startPoint: .top, endPoint: .bottom))
                .overlay(
                    RoundedRectangle(cornerRadius: 2 * scale, style: .continuous)
                        .strokeBorder(Brass.lo.opacity(0.6), lineWidth: 0.5)
                )
                .shadow(color: .black.opacity(0.5), radius: 1.5 * scale, y: 1 * scale)
        )
    }

    private var screw: some View {
        Circle()
            .fill(RadialGradient(colors: [Brass.hi, Brass.lo], center: .topLeading, startRadius: 0, endRadius: 3 * scale))
            .frame(width: 3.5 * scale, height: 3.5 * scale)
    }
}

// MARK: - Room

/// A dim gallery: dark walls, a warm wash from the ceiling, the floor falling
/// into shadow.
struct MuseumBackdrop: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [
                    Color(red: 0.10, green: 0.105, blue: 0.13),
                    Color(red: 0.05, green: 0.055, blue: 0.07),
                    Color(red: 0.025, green: 0.03, blue: 0.04)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            RadialGradient(
                colors: [Color(red: 1, green: 0.92, blue: 0.78).opacity(0.10), .clear],
                center: .top,
                startRadius: 0,
                endRadius: 520
            )
        }
    }
}
