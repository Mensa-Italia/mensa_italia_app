import SwiftUI
import Shared

/// Gli auguri al socio, il giorno del suo compleanno. Solo elementi di
/// sistema: SF Symbols animati con `symbolEffect`, testo dinamico, il bottone
/// in vetro e un feedback aptico all'apertura. Quando mostrarla lo decide
/// `BirthdayGreeting` (:shared). Mirrors Android `BirthdayGreetingSheet`.
struct BirthdayGreetingSheet: View {
    let wish: BirthdayWish
    let onDone: () -> Void

    @State private var celebrate = false

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 12)

            ZStack {
                Circle()
                    .fill(AppTheme.Colors.brandTintAdaptive.opacity(0.10))
                    .frame(width: 132, height: 132)
                Image(systemName: "birthday.cake.fill")
                    .font(.system(size: 64, weight: .regular))
                    .symbolRenderingMode(.palette)
                    .foregroundStyle(.orange, AppTheme.Colors.brandTintAdaptive)
                    .symbolEffect(.bounce.up.byLayer, options: .repeat(2), value: celebrate)
            }
            .overlay(alignment: .topLeading) {
                decoration("party.popper.fill", .pink, size: 26, rotation: -18)
                    .offset(x: -30, y: -6)
            }
            .overlay(alignment: .topTrailing) {
                decoration("balloon.2.fill", .red, size: 28, rotation: 12)
                    .offset(x: 30, y: -14)
            }
            .overlay(alignment: .bottomLeading) {
                decoration("gift.fill", .purple, size: 22, rotation: -8)
                    .offset(x: -26, y: 4)
            }
            .overlay(alignment: .bottomTrailing) {
                decoration("sparkles", .yellow, size: 24, rotation: 0)
                    .offset(x: 28, y: 0)
            }
            .accessibilityHidden(true)
            .padding(.bottom, 26)

            Text(title)
                .font(.system(.largeTitle, design: .rounded).weight(.bold))
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            Text(subtitle)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.top, 10)

            Spacer(minLength: 20)

            Button(action: onDone) {
                Label(tr("birthday_greeting.done", fallback: "Grazie!"), systemImage: "heart.fill")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .compatGlassProminentButtonStyle()
            .tint(AppTheme.Colors.mensaBlue)
        }
        .padding(.horizontal, 32)
        .padding(.top, 20)
        .padding(.bottom, 20)
        .frame(maxWidth: .infinity)
        .sensoryFeedback(.success, trigger: celebrate)
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
        .task {
            // Un attimo dopo la salita della scheda, cosi' l'animazione si vede.
            try? await Task.sleep(nanoseconds: 350_000_000)
            celebrate = true
        }
    }

    private func decoration(_ symbol: String, _ color: Color, size: CGFloat, rotation: Double) -> some View {
        Image(systemName: symbol)
            .font(.system(size: size, weight: .semibold))
            .foregroundStyle(color.gradient)
            .rotationEffect(.degrees(rotation))
            .symbolEffect(.bounce, options: .repeat(2).speed(0.8), value: celebrate)
    }

    private var title: String {
        tr("birthday_greeting.title", fallback: "Buon compleanno, {name}!", ["name": wish.firstName])
    }

    private var subtitle: String {
        if let age = wish.age?.intValue {
            return tr("birthday_greeting.body_age",
                      fallback: "Oggi compi {age} anni: tutta Mensa Italia ti augura una splendida giornata.",
                      ["age": "\(age)"])
        }
        return tr("birthday_greeting.body",
                  fallback: "Tutta Mensa Italia ti augura una splendida giornata.")
    }
}
