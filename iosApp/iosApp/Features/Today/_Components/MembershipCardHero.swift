import SwiftUI
#if os(iOS)
import CoreMotion
#endif

// Mensa primary blue (#184295) — inlined here to keep this component
// independent from AppTheme so SourceKit can resolve it across targets.
private let mensaBlue = Color(red: 24/255, green: 66/255, blue: 149/255)

// MARK: - MotionManager

/// Small wrapper around CMMotionManager publishing low-pass filtered pitch/roll
/// for driving subtle 3D parallax and shine on the tessera card.
@MainActor
final class TesseraMotionManager: ObservableObject {
    @Published var pitch: Double = 0   // rotation around X axis (top/bottom)
    @Published var roll: Double = 0    // rotation around Y axis (left/right)

    #if os(iOS)
    private let manager = CMMotionManager()
    private let queue = OperationQueue()
    #endif
    private let alpha: Double = 0.15   // low-pass smoothing
    private var refCount = 0

    init() {
        #if os(iOS)
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        queue.qualityOfService = .userInteractive
        #endif
    }

    func start() {
        refCount += 1
        guard refCount == 1 else { return }
        #if os(iOS)
        guard manager.isDeviceMotionAvailable else { return }
        manager.startDeviceMotionUpdates(to: queue) { [weak self] motion, _ in
            guard let self, let m = motion else { return }
            let newPitch = m.attitude.pitch
            let newRoll = m.attitude.roll
            Task { @MainActor in
                self.pitch = self.pitch + (newPitch - self.pitch) * self.alpha
                self.roll = self.roll + (newRoll - self.roll) * self.alpha
            }
        }
        #endif
    }

    func stop() {
        refCount = max(0, refCount - 1)
        if refCount == 0 {
            #if os(iOS)
            manager.stopDeviceMotionUpdates()
            #endif
        }
    }
}

// MARK: - MembershipCardHero (public API preserved)

/// Faithful digital reproduction of the physical Mensa Italia tessera.
/// Tap to flip; gyroscope drives subtle parallax + shine.
struct MembershipCardHero: View {
    var fullName: String
    var memberSince: String
    var expiry: String
    var memberId: String
    var avatarURL: URL? = nil          // accepted for source compat; unused
    var isFullScreen: Bool = false
    var avatarSize: CGFloat = 72       // accepted for source compat; unused

    @StateObject private var motion = TesseraMotionManager()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var flipped: Bool = false

    // ISO/IEC 7810 ID-1 card aspect ratio (Flutter source)
    private let aspect: CGFloat = 1.586

    // Max tilt clamp (radians ≈ 6°) — subtle per HIG motion guidance.
    private let maxTilt: Double = .pi / 30

    // Reference card width used in the Flutter source for the literal pt values
    // (padding 20, corner 20, font 20/14). All sizes are pixel-locked to the
    // actual rendered card via `u = size.width / referenceWidth`.
    fileprivate static let referenceWidth: CGFloat = 343

    var body: some View {
        GeometryReader { geo in
            // Unit scale — everything inside the card derives from this.
            let u = geo.size.width / Self.referenceWidth
            let cornerRadius = 20 * u
            // Clamp gyro pitch/roll for parallax
            let pitchClamped = reduceMotion ? 0 : max(-maxTilt, min(maxTilt, motion.pitch))
            let rollClamped  = reduceMotion ? 0 : max(-maxTilt, min(maxTilt, motion.roll))
            // Normalized roll → 0...1 for shine sweep (matches Flutter)
            let normalizedRotation = reduceMotion
                ? 0.5
                : (max(-Double.pi / 2, min(Double.pi / 2, motion.roll)) + .pi / 2) / .pi

            ZStack {
                // FRONT
                FrontFace(
                    size: geo.size,
                    cornerRadius: cornerRadius,
                    normalizedRotation: normalizedRotation
                )
                .opacity(visibleFront ? 1 : 0)

                // BACK (counter-rotated so the content reads correctly when flipped)
                BackFace(
                    fullName: fullName,
                    memberId: memberId,
                    size: geo.size,
                    cornerRadius: cornerRadius
                )
                .rotation3DEffect(.degrees(180), axis: (x: 0, y: 1, z: 0))
                .opacity(visibleFront ? 0 : 1)
            }
            // Flip
            .rotation3DEffect(
                .degrees(flipped ? 180 : 0),
                axis: (x: 0, y: 1, z: 0),
                anchor: .center,
                anchorZ: 0,
                perspective: 0.6
            )
            // Gyro parallax (pitch → X tilt, roll → Y tilt)
            .rotation3DEffect(
                .radians(pitchClamped),
                axis: (x: 1, y: 0, z: 0),
                perspective: 0.6
            )
            .rotation3DEffect(
                .radians(-rollClamped),
                axis: (x: 0, y: 1, z: 0),
                perspective: 0.6
            )
            .animation(.easeOut(duration: 0.18), value: motion.pitch)
            .animation(.easeOut(duration: 0.18), value: motion.roll)
            .contentShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .onTapGesture {
                withAnimation(.spring(response: 0.7, dampingFraction: 0.78)) {
                    flipped.toggle()
                }
            }
        }
        .aspectRatio(aspect, contentMode: .fit)
        .onAppear { motion.start() }
        .onDisappear { motion.stop() }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(tr(
            "card.accessibility.label",
            fallback: "Tessera Mensa, {name}, ID {id}, scadenza {expiry}",
            ["name": fullName, "id": memberId, "expiry": expiry]
        )))
        .accessibilityAddTraits(.isButton)
        .accessibilityAction(named: Text(tr("card.flip_action", fallback: "Gira tessera"))) {
            withAnimation(.spring(response: 0.7, dampingFraction: 0.78)) { flipped.toggle() }
        }
    }

    /// Front is visible while the flip rotation is in the first/last quarter.
    private var visibleFront: Bool { !flipped }
}

// MARK: - Front face

private struct FrontFace: View {
    let size: CGSize
    let cornerRadius: CGFloat
    let normalizedRotation: Double  // 0...1

    var body: some View {
        let u = size.width / MembershipCardHero.referenceWidth
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        let logoSide = size.height / 3
        // Chevron metrics scaled to card width (Flutter: 24pt Material icon, 10/10 margin
        // off the bottom-right of the Row's right Expanded — visually ≈ 18pt at the
        // reference card size, with ~14/12 pt insets).
        let chevronFont = 18 * u
        let chevronTrailing = 14 * u
        let chevronBottom = 12 * u
        let strokeWidth = max(0.5, 1 * u)
        let shadowRadius = 12 * u
        let shadowY = 6 * u

        ZStack {
            // Solid Mensa blue
            shape.fill(mensaBlue)

            // Mensa logo centered, white, with diagonal shine mask matching Flutter
            Image("TesseraLogoMark")
                .renderingMode(.template)
                .resizable()
                .scaledToFit()
                .frame(width: logoSide, height: logoSide)
                .foregroundStyle(.white)
                .overlay(
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: clampStop((1 - normalizedRotation) - 0.5)),
                            .init(color: .white.opacity(0.55), location: clampStop(1 - normalizedRotation)),
                            .init(color: .clear, location: clampStop((1 - normalizedRotation) + 0.5))
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .blendMode(.plusLighter)
                    .mask(
                        Image("TesseraLogoMark")
                            .renderingMode(.template)
                            .resizable()
                            .scaledToFit()
                            .frame(width: logoSide, height: logoSide)
                            .foregroundStyle(.white)
                    )
                )

            // Chevron bottom-right
            VStack {
                Spacer()
                HStack {
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: chevronFont, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.trailing, chevronTrailing)
                        .padding(.bottom, chevronBottom)
                }
            }

            // Full-card shine sweep
            LinearGradient(
                stops: [
                    .init(color: .clear, location: clampStop((1 - normalizedRotation) - 0.5)),
                    .init(color: .white.opacity(0.10), location: clampStop(1 - normalizedRotation)),
                    .init(color: .clear, location: clampStop((1 - normalizedRotation) + 0.5))
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .allowsHitTesting(false)
        }
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(
                LinearGradient(
                    colors: [.white.opacity(0.45), .white.opacity(0.05)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: strokeWidth
            )
        )
        .shadow(color: .black.opacity(0.18), radius: shadowRadius, x: 0, y: shadowY)
    }

    private func clampStop(_ v: Double) -> CGFloat {
        CGFloat(min(1.0, max(0.0, v)))
    }
}

// MARK: - Back face

private struct BackFace: View {
    let fullName: String
    let memberId: String
    let size: CGSize
    let cornerRadius: CGFloat

    // PNG `lettering_horizzontal_white.png` intrinsic aspect (586 × 235), from Flutter.
    private let letteringAspect: CGFloat = 586.0 / 235.0

    var body: some View {
        // Pixel-lock everything to the rendered card width: a card 2× the size
        // gets 2× padding, 2× corner, 2× type — exactly like Flutter scaling
        // would behave if the layout box itself grew.
        let u = size.width / MembershipCardHero.referenceWidth
        let padding = 20 * u
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        // Inner content box, after the 20pt padding (matches Flutter LayoutBuilder constraints).
        let iw = max(0, size.width  - padding * 2)
        let ih = max(0, size.height - padding * 2)
        // Lettering: width = innerWidth × 2/3, height derived from the PNG's intrinsic aspect.
        let letteringW = iw * 2.0 / 3.0
        let letteringH = letteringW / letteringAspect
        // Lower content area takes the remainder of the inner height.
        let lowerH = max(0, ih - letteringH)
        // The inner-Column has left margin = innerWidth × 2/7 (Flutter Container.margin).
        let leftIndent = iw * 2.0 / 7.0
        // Lower-section bands (Flutter: two Expanded → 1/2 each; bottom one splits 1/2 → 1/4 each).
        let nameBandH    = lowerH / 2.0
        let tesseraBandH = lowerH / 4.0
        let footerBandH  = lowerH / 4.0
        // Footer row width: innerWidth − leftIndent = innerWidth × 5/7.
        let lowerBandW = iw - leftIndent
        // Split on first space, like Flutter (replaceFirst(" ", "~~~").split("~~~")).
        let nameParts = splitNameFirstSpace(fullName)
        // Font sizes: Flutter literals (20 / 14) scaled by `u`. minimumScaleFactor(0.05)
        // still lets them shrink on overflow, mirroring AutoSizeText(minFontSize: 0).
        let nameFont    = 20 * u
        let tesseraFont = 14 * u
        let idFont      = 20 * u
        let mensaItFont = 14 * u

        ZStack {
            // Background: mensaBlue base + photo wash (white 30% blend, like Flutter ColorFilter).
            shape.fill(mensaBlue)
            Image("TesseraBackground")
                .resizable()
                .scaledToFill()
                .frame(width: size.width, height: size.height)
                .overlay(Color.white.opacity(0.30))
                .clipped()

            // Strict pixel-exact layout: position each block by absolute offset within the
            // inner content box. Matches Flutter's Column(spaceBetween) + Expanded(Expanded(...)).
            ZStack(alignment: .topLeading) {
                // Top-left: lettering, width = iw × 2/3, height from PNG aspect.
                Image("TesseraLettering")
                    .resizable()
                    .scaledToFit()
                    .frame(width: letteringW, height: letteringH, alignment: .topLeading)
                    .offset(x: 0, y: 0)

                // Name band: lower section first half. Each name part fills equal vertical share
                // (Flutter: List.generate inside Expanded → Expanded children).
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(nameParts.enumerated()), id: \.offset) { _, part in
                        Text(part.uppercased())
                            .font(GothamFont.bold(size: nameFont))
                            .foregroundStyle(.black)
                            .lineLimit(1)
                            .minimumScaleFactor(0.05)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                    }
                }
                .frame(width: lowerBandW, height: nameBandH, alignment: .topLeading)
                .offset(x: leftIndent, y: letteringH)

                // "Tessera" label: bottom-left of its 1/4 band (Flutter Container alignment bottomLeft).
                Text("Tessera")
                    .font(GothamFont.bold(size: tesseraFont))
                    .foregroundStyle(.black)
                    .lineLimit(1)
                    .minimumScaleFactor(0.05)
                    .frame(width: lowerBandW, height: tesseraBandH, alignment: .bottomLeading)
                    .offset(x: leftIndent, y: letteringH + nameBandH)

                // Footer row: id (left, fontSize 20) + MENSA.IT (right, fontSize 14), bottom-aligned.
                HStack(alignment: .bottom, spacing: 8) {
                    Text(memberId)
                        .font(GothamFont.bold(size: idFont))
                        .foregroundStyle(.black)
                        .lineLimit(1)
                        .minimumScaleFactor(0.05)
                    Spacer(minLength: 0)
                    Text("MENSA.IT")
                        .font(GothamFont.bold(size: mensaItFont))
                        .foregroundStyle(.black)
                        .lineLimit(1)
                        .minimumScaleFactor(0.05)
                }
                .frame(width: lowerBandW, height: footerBandH, alignment: .bottomLeading)
                .offset(x: leftIndent, y: letteringH + nameBandH + tesseraBandH)
            }
            .frame(width: iw, height: ih, alignment: .topLeading)
            .padding(padding)
        }
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(
                LinearGradient(
                    colors: [.white.opacity(0.45), .white.opacity(0.05)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                lineWidth: max(0.5, 1 * u)
            )
        )
        .shadow(color: .black.opacity(0.18), radius: 12 * u, x: 0, y: 6 * u)
    }

    private func splitNameFirstSpace(_ name: String) -> [String] {
        let trimmed = name.trimmingCharacters(in: .whitespaces)
        guard let idx = trimmed.firstIndex(of: " ") else {
            return [trimmed]
        }
        let first = String(trimmed[..<idx]).trimmingCharacters(in: .whitespaces)
        let rest = String(trimmed[trimmed.index(after: idx)...]).trimmingCharacters(in: .whitespaces)
        return rest.isEmpty ? [first] : [first, rest]
    }
}

// MARK: - Preview

#Preview {
    VStack(spacing: 24) {
        MembershipCardHero(
            fullName: "Matteo Sipione",
            memberSince: "2024",
            expiry: "2026-12-31",
            memberId: "M-12345"
        )
        .frame(height: 180)
        .padding(.horizontal)

        MembershipCardHero(
            fullName: "Matteo Sipione",
            memberSince: "2024",
            expiry: "2026-12-31",
            memberId: "M-12345",
            isFullScreen: true,
            avatarSize: 90
        )
        .frame(height: 260)
        .padding(.horizontal)
    }
    .background(Color.black.opacity(0.9))
}
