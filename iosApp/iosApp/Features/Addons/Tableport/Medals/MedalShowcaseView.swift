import SwiftUI

/// One medal brought out of its case, in a sheet: large, in a halo of its
/// metal with slowly turning rays once earned, turned by dragging to show
/// the engraved reverse; then its name, what it's for, the ladder of its
/// metals and how far along the next one is. Mirrors Android `MedalShowcase`.
struct MedalShowcaseView: View {
    let medal: Medal

    @Environment(\.dismiss) private var dismiss
    @State private var angle: Double = 0
    @State private var dragStartAngle: Double?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 0) {
                    ZStack {
                        Halo(color: medal.isEarned ? medal.tier.metal.mid : Color(white: 0.36), rays: medal.isEarned)
                        SpinningMedal(angle: angle, medal: medal, size: 196)
                    }
                    .frame(height: 300)
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                    .gesture(spin)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Text(medal.title))
                    .accessibilityHint(Text(tr("medals.spin_hint", fallback: "Trascina per girarla")))

                    TierBadge(medal: medal)
                    // A region medal is "Name 2026": each year is its own.
                    Text(medal.region.map { "\(medal.title) \($0.year)" } ?? medal.title)
                        .font(.system(.title, design: .serif).weight(.bold))
                        .multilineTextAlignment(.center)
                        .padding(.top, 14)
                        .padding(.horizontal, 24)
                    Text(medal.detail)
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, 8)
                        .padding(.horizontal, 32)
                    if let region = medal.region {
                        if region.witnessed {
                            WitnessBadge()
                                .padding(.top, 14)
                        }
                        RegionTierLadder(medal: medal, region: region)
                            .padding(.top, 28)
                            .padding(.horizontal, 28)
                        RegionStatus(medal: medal, region: region)
                            .padding(.top, 24)
                            .padding(.horizontal, 32)
                        RegionRuleCard(rule: region.rule)
                            .padding(.top, 24)
                            .padding(.horizontal, 24)
                    } else {
                        if medal.levels.count > 1, !medal.isHidden {
                            TierLadder(medal: medal)
                                .padding(.top, 28)
                                .padding(.horizontal, 28)
                        }
                        status
                            .padding(.top, 24)
                            .padding(.horizontal, 32)
                        if medal.countsStamps, !medal.isHidden {
                            StampRules()
                                .padding(.top, 24)
                                .padding(.horizontal, 24)
                        }
                    }
                    Text(tr("medals.spin_hint", fallback: "Trascina per girarla"))
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.35))
                        .padding(.top, 20)
                        .padding(.bottom, 32)
                }
            }
            .scrollBounceBehavior(.basedOnSize)
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
        .presentationDragIndicator(.visible)
        .presentationBackground(Stage.surface)
        .preferredColorScheme(.dark)
    }

    /// Where the member stands with it: how far the next metal is, or that
    /// it's done.
    @ViewBuilder
    private var status: some View {
        if let next = medal.next, !medal.isHidden {
            VStack(spacing: 8) {
                Text(tr("medals.next", fallback: "Prossimo traguardo: %1$@ a %2$d")
                    .replacingOccurrences(of: "%1$@", with: next.tier.name)
                    .replacingOccurrences(of: "%2$d", with: "\(next.threshold)"))
                    .font(.subheadline.weight(.semibold))
                ProgressView(value: medal.progress)
                    .tint(next.tier.metal.mid)
                Text(tr("medals.progress", fallback: "%1$d su %2$d")
                    .replacingOccurrences(of: "%1$d", with: "\(medal.value)")
                    .replacingOccurrences(of: "%2$d", with: "\(next.threshold)"))
                    .font(.footnote.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: 320)
        } else if medal.isEarned, medal.levels.count > 1 {
            Text(tr("medals.max_tier", fallback: "Livello massimo raggiunto"))
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(medal.tier.metal.hi)
        }
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
}

/// Which stamps a stamp-counting medal sees: the ranking's one-point rule
/// with figures by region size, then the organizer and staff exclusions.
/// The same for every such medal, so the question is answered where it
/// comes up. Mirrors Android `StampRules`.
private struct StampRules: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("medals.stamp_rules.title", fallback: "Quali timbri contano"))
                .font(.subheadline.weight(.semibold))
            rule("checkmark.seal.fill",
                 tr("medals.stamp_rules.threshold_v3", fallback: "Conta il timbro che in classifica vale almeno un punto: chi ha timbrato all'evento, in rapporto ai soci attivi della regione in cui si è svolto, deve raggiungere la soglia da cui la classifica comincia ad assegnare punti. È una quota, non un numero fisso: una regione con più soci chiede più timbratori, ma meno che in proporzione."))
            rule("megaphone.fill",
                 tr("medals.stamp_rules.organizer_v2", fallback: "I timbri dei tuoi eventi vanno nella medaglia Organizzatore, non qui."))
            rule("person.3.fill",
                 tr("medals.stamp_rules.staff_v2", fallback: "Lo staff di una regione, finché in carica, non conta i timbri degli eventi non nazionali della propria regione."))
            mathCaption(tr("medals.stamp_rules.more_v3", fallback: "Il criterio esatto, formula compresa, è nella scheda ⓘ della classifica, sezione Matematica."))
                .font(.footnote)
                .foregroundStyle(.white.opacity(0.45))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .background(.white.opacity(0.06), in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }

    private func rule(_ symbol: String, _ text: String) -> some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 15))
                .foregroundStyle(.white.opacity(0.6))
                .frame(width: 18)
                .padding(.top, 2)
            mathCaption(text)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }
}

/// A glow of the medal's colour, and rays turning slowly behind an earned one.
private struct Halo: View {
    let color: Color
    let rays: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(paused: !rays || reduceMotion)) { timeline in
            let turn = timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 60) / 60 * 2 * .pi
            Canvas { context, size in
                let c = CGPoint(x: size.width / 2, y: size.height / 2)
                // Faded out by the box's nearer edges, so nothing is cut off.
                let r = min(size.width, size.height) * 0.5
                context.fill(
                    Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)),
                    with: .radialGradient(Gradient(colors: [color.opacity(0.45), color.opacity(0.10), .clear]),
                                          center: c, startRadius: 0, endRadius: r)
                )
                guard rays else { return }
                let count = 18
                let spread = Double.pi / Double(count) * 0.55
                let reach = r
                for i in 0..<count {
                    let a = 2 * Double.pi * Double(i) / Double(count) + turn
                    var ray = Path()
                    ray.move(to: c)
                    ray.addLine(to: CGPoint(x: c.x + cos(a - spread) * reach, y: c.y + sin(a - spread) * reach))
                    ray.addLine(to: CGPoint(x: c.x + cos(a + spread) * reach, y: c.y + sin(a + spread) * reach))
                    ray.closeSubpath()
                    context.fill(ray, with: .radialGradient(Gradient(colors: [color.opacity(0.22), .clear]),
                                                            center: c, startRadius: 0, endRadius: reach))
                }
            }
        }
        .allowsHitTesting(false)
    }
}

/// The metal, in a capsule of its colour; "not yet" for a locked medal.
private struct TierBadge: View {
    let medal: Medal

    var body: some View {
        let metal = medal.tier.metal
        HStack(spacing: 6) {
            if medal.isEarned {
                Image(systemName: "checkmark.seal.fill").foregroundStyle(metal.hi)
            }
            Text(medal.isEarned
                 ? medal.tier.name.uppercased()
                 : tr("medals.locked", fallback: "Non ancora ottenuta").uppercased())
                .font(.caption.weight(.bold))
                .tracking(2)
                .foregroundStyle(medal.isEarned ? metal.hi : .white.opacity(0.55))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
        .background(medal.isEarned ? metal.mid.opacity(0.14) : .clear, in: Capsule())
        .overlay(Capsule().strokeBorder(medal.isEarned ? metal.mid : .white.opacity(0.2), lineWidth: 1))
    }
}

/// Each metal of the medal from the first to the last, with the value that
/// earns it: the ones reached lit, the next one ringed by how far along it is.
private struct TierLadder: View {
    let medal: Medal

    var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(Array(medal.levels.enumerated()), id: \.offset) { index, level in
                let reached = medal.value >= level.threshold
                let isNext = medal.next?.tier == level.tier
                if index > 0 {
                    Capsule()
                        .fill(reached ? level.tier.metal.mid : .white.opacity(0.14))
                        .frame(height: 2)
                        .padding(.top, 27)
                        .frame(maxWidth: .infinity)
                }
                VStack(spacing: 2) {
                    ZStack {
                        if isNext {
                            Circle()
                                .stroke(.white.opacity(0.12), lineWidth: 4)
                            Circle()
                                .trim(from: 0, to: medal.progress)
                                .stroke(level.tier.metal.mid, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                        }
                        MedalArtwork(tier: level.tier, symbol: medal.symbol, size: 38, locked: !reached)
                    }
                    .frame(width: 56, height: 56)
                    Text(level.tier.name)
                        .font(.caption2)
                        .foregroundStyle(reached ? level.tier.metal.hi : .white.opacity(0.45))
                        .padding(.top, 4)
                    Text("\(level.threshold)")
                        .font(.subheadline.weight(.bold).monospacedDigit())
                        .foregroundStyle(reached || isNext ? .white : .white.opacity(0.45))
                }
                .accessibilityElement(children: .combine)
            }
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
        .shadow(color: .black.opacity(0.5), radius: 18, y: 10)
    }
}
