import SwiftUI

/// "Crediti" — una sola attribuzione, centrata sullo schermo.
///
/// Niente hero, niente stack tecnologico, niente colophon: la pagina dice chi
/// ha scritto l'app e nient'altro. Il grigio su grigio e la coppia di pesi
/// diversi fanno tutto il lavoro tipografico, senza bordi ne' card.
struct CreditsView: View {
    var body: some View {
        ZStack {
            // Stesso grigio delle liste raggruppate: il testo ci si appoggia
            // sopra con un contrasto volutamente basso.
            Color(.systemGroupedBackground)
                .ignoresSafeArea()

            VStack(spacing: 6) {
                Text(tr("app.credits.developed_by", fallback: "Sviluppato da")) // i18n
                    .font(.footnote)
                    .foregroundStyle(.tertiary)

                // Nome proprio: non passa da `tr()` in nessuna lingua.
                Text("Matteo Sipione")
                    .font(.title3.weight(.medium))
                    .foregroundStyle(.secondary)
            }
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .combine)
        }
        .navigationTitle(tr("app.credits.title", fallback: "Crediti")) // i18n
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview {
    NavigationStack {
        CreditsView()
    }
}
