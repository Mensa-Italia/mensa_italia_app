import SwiftUI

/// One medal taken out of the cabinet: large, on its pedestal under the
/// spotlight, turned by dragging to show the engraved reverse. Below, what
/// it's for and when it was earned (or how far along it is).
struct MedalShowcaseView: View {
    let medal: Medal

    @Environment(\.dismiss) private var dismiss
    @State private var angle: Double = 0
    @State private var dragStartAngle: Double?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 26) {
                    stage
                    info
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 40)
            }
            .background(MuseumBackdrop().ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Label(tr("app.close", fallback: "Chiudi"), systemImage: "xmark")
                    }
                }
            }
        }
        .environment(\.colorScheme, .dark)
        .presentationDragIndicator(.visible)
    }

    // MARK: Stage

    private var stage: some View {
        ZStack(alignment: .bottom) {
            if medal.isEarned {
                SpotlightCone()
                    .fill(LinearGradient(
                        colors: [Color(red: 1, green: 0.95, blue: 0.82).opacity(0.32), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    ))
                    .blur(radius: 14)
                    .padding(.horizontal, 20)
            }
            VStack(spacing: 0) {
                SpinningMedal(
                    angle: angle,
                    medal: medal,
                    size: 200
                )
                .padding(.top, 40)
                .padding(.bottom, 26)
                Ellipse()
                    .fill(.black.opacity(0.55))
                    .frame(width: 130, height: 12)
                    .blur(radius: 6)
                    .padding(.bottom, 6)
                MedalPedestal(plaque: medal.title, scale: 1.5)
                    .frame(width: 260)
            }
        }
        .frame(maxWidth: .infinity)
        .contentShape(Rectangle())
        .gesture(spin)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(medal.title))
        .accessibilityHint(Text(tr("medals.spin_hint", fallback: "Trascina per girarla")))
    }

    /// Follows the finger; let go, it settles on whichever face is nearer,
    /// carried round by the fling.
    private var spin: some Gesture {
        DragGesture(minimumDistance: 4)
            .onChanged { value in
                let start = dragStartAngle ?? angle
                if dragStartAngle == nil { dragStartAngle = angle }
                angle = start + value.translation.width * 0.8
            }
            .onEnded { value in
                let thrown = angle + (value.predictedEndTranslation.width - value.translation.width) * 0.5
                let target = (thrown / 180).rounded() * 180
                dragStartAngle = nil
                withAnimation(.spring(response: 0.7, dampingFraction: 0.72)) {
                    angle = target
                }
            }
    }

    // MARK: Info

    private var info: some View {
        VStack(spacing: 12) {
            Text(medal.tier.name.uppercased())
                .font(.caption.weight(.bold))
                .tracking(2.5)
                .foregroundStyle(medal.tier.metal.hi)
            Text(medal.title)
                .font(.system(size: 30, weight: .bold, design: .serif))
                .multilineTextAlignment(.center)
                .foregroundStyle(.white)
            Text(medal.detail)
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.7))

            progressBlock
                .padding(.top, 6)

            Text(tr("medals.spin_hint", fallback: "Trascina per girarla"))
                .font(.caption)
                .foregroundStyle(.white.opacity(0.35))
                .padding(.top, 10)
        }
    }

    /// Earned or not, how far it is to the next metal; nothing more to aim
    /// for at the top.
    @ViewBuilder
    private var progressBlock: some View {
        if medal.isEarned {
            Label(tr("medals.earned", fallback: "Ottenuta"), systemImage: "checkmark.seal.fill")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Brass.hi)
        }
        if let next = medal.next {
            VStack(spacing: 8) {
                ProgressView(value: medal.progress)
                    .tint(next.tier.metal.mid)
                Text(tr("medals.next", fallback: "Prossimo traguardo: %1$@ a %2$d")
                    .replacingOccurrences(of: "%1$@", with: next.tier.name)
                    .replacingOccurrences(of: "%2$d", with: "\(next.threshold)"))
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.7))
                Text(tr("medals.progress", fallback: "%1$d su %2$d")
                    .replacingOccurrences(of: "%1$d", with: "\(medal.value)")
                    .replacingOccurrences(of: "%2$d", with: "\(next.threshold)"))
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.white.opacity(0.45))
            }
            .frame(maxWidth: 260)
        } else if medal.isEarned {
            Text(tr("medals.max_tier", fallback: "Livello massimo raggiunto"))
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.white.opacity(0.55))
        }
    }
}

/// The medal turned about its vertical axis. Animatable, so a released spin
/// swaps to the reverse exactly when it's edge-on, and the coin's rim shows
/// its thickness on the way round.
private struct SpinningMedal: View, Animatable {
    var angle: Double
    let medal: Medal
    let size: CGFloat

    var animatableData: Double {
        get { angle }
        set { angle = newValue }
    }

    var body: some View {
        let radians = angle * .pi / 180
        let showsBack = cos(radians) < 0
        let metal = medal.tier.metal
        ZStack {
            // The rim, seen as the coin turns edge-on.
            Capsule()
                .fill(LinearGradient(colors: [metal.lo, metal.mid, metal.lo], startPoint: .top, endPoint: .bottom))
                .frame(width: max(0, abs(sin(radians)) * size * 0.07), height: size * 0.96)

            MedalArtwork(
                tier: medal.tier,
                symbol: medal.symbol,
                size: size,
                locked: !medal.isEarned,
                sheen: CGSize(width: sin(radians), height: 0),
                back: showsBack ? (medal.title, medal.tier.name.uppercased()) : nil
            )
            // The reverse is drawn mirrored, so it reads right once turned.
            .scaleEffect(x: showsBack ? -1 : 1, y: 1)
            .rotation3DEffect(.degrees(angle), axis: (x: 0, y: 1, z: 0), perspective: 0.35)
        }
        .shadow(color: .black.opacity(0.5), radius: 14, y: 10)
    }
}
