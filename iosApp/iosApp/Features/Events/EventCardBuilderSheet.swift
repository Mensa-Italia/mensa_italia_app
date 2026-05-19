import SwiftUI

/// Native SwiftUI replacement for the Flutter `EventCardGenerator` bottom sheet.
/// Generates an event cover PNG via the public `svc.mensa.it` endpoint and
/// returns the bytes to the caller on confirmation.
struct EventCardBuilderSheet: View {
    /// Called when the user taps "Perfetto!" with the generated PNG bytes.
    /// Caller dismisses the sheet.
    var onConfirmed: (Data) -> Void
    /// Called when the user cancels. Default no-op.
    var onCancelled: () -> Void = {}

    // MARK: - Form state

    @State private var title = ""
    @State private var date = ""
    @State private var time = ""
    @State private var location = ""
    @State private var address = ""
    @State private var city = ""

    // MARK: - Generation state

    @State private var isGenerating = false
    @State private var generatedImage: Data?
    @State private var error: String?

    private static let templateURL = URL(string: "https://svc.mensa.it/static/event_card_template.png")!
    private static let endpoint = "https://svc.mensa.it/api/cs/generate-event-card"

    private var allFieldsEmpty: Bool {
        [title, date, time, location, address, city]
            .allSatisfy { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    preview
                        .listRowInsets(EdgeInsets())
                        .listRowBackground(Color.clear)
                }

                Section(tr("events.cover.section.details", fallback: "Dettagli")) {
                    field(
                        title: tr("events.cover.field.title", fallback: "Titolo breve"),
                        text: $title
                    )
                    field(
                        title: tr("events.cover.field.date", fallback: "Lunedì 1 gennaio"),
                        text: $date
                    )
                    field(
                        title: tr("events.cover.field.time", fallback: "Ore 21:00"),
                        text: $time
                    )
                    field(
                        title: tr("events.cover.field.location", fallback: "Ristorante bellissimo"),
                        text: $location
                    )
                    field(
                        title: tr("events.cover.field.address", fallback: "Via Roma 1"),
                        text: $address
                    )
                    field(
                        title: tr("events.cover.field.city", fallback: "Milano (MI)"),
                        text: $city
                    )
                }

                Section {
                    Button {
                        Task { await generate() }
                    } label: {
                        ZStack {
                            if isGenerating {
                                ProgressView().tint(.white)
                            } else {
                                Label(
                                    tr("events.cover.generate", fallback: "Genera"),
                                    systemImage: "wand.and.stars"
                                )
                                .fontWeight(.semibold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.Colors.brandPrimary)
                    .controlSize(.large)
                    .disabled(isGenerating || allFieldsEmpty)
                    .accessibilityHint(tr(
                        "events.cover.generate_hint",
                        fallback: "Crea l'anteprima con i dati inseriti"
                    ))
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 4, trailing: 16))

                    if let data = generatedImage {
                        Button {
                            onConfirmed(data)
                        } label: {
                            Text(tr("events.cover.confirm", fallback: "Perfetto!"))
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                        .tint(.green)
                        .controlSize(.large)
                        .listRowBackground(Color.clear)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 8, trailing: 16))
                    }
                }
            }
            .navigationTitle(tr("events.cover.title", fallback: "Crea copertina"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("app.cancel", fallback: "Annulla")) {
                        onCancelled()
                    }
                }
            }
        }
    }

    // MARK: - Subviews

    @ViewBuilder
    private var preview: some View {
        ZStack {
            if let error {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(.regularMaterial)
                ContentUnavailableView(
                    tr("events.cover.error.title", fallback: "Anteprima non disponibile"),
                    systemImage: "photo.badge.exclamationmark",
                    description: Text(error)
                )
            } else if isGenerating {
                RoundedRectangle(cornerRadius: 24, style: .continuous)
                    .fill(.regularMaterial)
                ProgressView()
                    .controlSize(.large)
            } else if let data = generatedImage, let ui = UIImage(data: data) {
                Image(uiImage: ui)
                    .resizable()
                    .scaledToFill()
            } else {
                CachedAsyncImage(url: Self.templateURL) { img in
                    img.resizable().scaledToFill()
                } placeholder: {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(.regularMaterial)
                        .overlay(ProgressView())
                }
            }
        }
        .aspectRatio(1600.0 / 900.0, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.08), lineWidth: 0.5)
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .accessibilityLabel(tr(
            "events.cover.preview_label",
            fallback: "Anteprima copertina evento"
        ))
    }

    private func field(title: String, text: Binding<String>) -> some View {
        TextField(title, text: text)
            .textInputAutocapitalization(.characters)
            .autocorrectionDisabled()
    }

    // MARK: - Generation

    private func generate() async {
        isGenerating = true
        error = nil
        defer { isGenerating = false }

        var components = URLComponents(string: Self.endpoint)!
        components.queryItems = [
            URLQueryItem(name: "title", value: title.trimmingCharacters(in: .whitespacesAndNewlines)),
            URLQueryItem(name: "line0", value: date.trimmingCharacters(in: .whitespacesAndNewlines)),
            URLQueryItem(name: "line1", value: time.trimmingCharacters(in: .whitespacesAndNewlines)),
            URLQueryItem(name: "line2", value: location.trimmingCharacters(in: .whitespacesAndNewlines)),
            URLQueryItem(name: "line3", value: address.trimmingCharacters(in: .whitespacesAndNewlines)),
            URLQueryItem(name: "line4", value: city.trimmingCharacters(in: .whitespacesAndNewlines))
        ]

        guard let url = components.url else {
            error = tr("events.cover.error.generate", fallback: "Generazione non riuscita. Riprova.")
            return
        }

        do {
            let (data, response) = try await URLSession.shared.data(for: URLRequest(url: url))
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                error = tr("events.cover.error.generate", fallback: "Generazione non riuscita. Riprova.")
                return
            }
            generatedImage = data
        } catch {
            self.error = tr("events.cover.error.generate", fallback: "Generazione non riuscita. Riprova.")
        }
    }
}
