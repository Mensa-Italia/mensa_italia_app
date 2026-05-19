import SwiftUI
import Shared

/// Lista degli eventi pubblici (pre-login). `List` nativa con `.insetGrouped`:
/// iOS 26 applica Liquid Glass automaticamente alle righe.
struct PublicEventsView: View {
    @State private var events: [EventModel] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if isLoading && events.isEmpty {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = errorMessage, events.isEmpty {
                ContentUnavailableView {
                    Label(tr("public.events.error.title", fallback: "Errore di caricamento"),
                          systemImage: "wifi.exclamationmark")
                } description: {
                    Text(error)
                } actions: {
                    Button(tr("common.retry", fallback: "Riprova")) { Task { await loadEvents() } }
                }
            } else if events.isEmpty {
                ContentUnavailableView(
                    tr("public.events.empty.title", fallback: "Nessun evento pubblico"),
                    systemImage: "calendar",
                    description: Text(tr(
                        "public.events.empty.description",
                        fallback: "Al momento non ci sono eventi aperti a tutti."
                    ))
                )
            } else {
                List {
                    ForEach(events, id: \.id) { event in
                        EventPreviewCard(event: event)
                    }
                }
                .listStyle(.insetGrouped)
                .refreshable { await loadEvents() }
            }
        }
        .navigationTitle(tr("public.events.title", fallback: "Eventi pubblici"))
        .navigationBarTitleDisplayMode(.large)
        .task { await loadEvents() }
    }

    private func loadEvents() async {
        isLoading = events.isEmpty
        errorMessage = nil
        do {
            let result = try await koin.events.fetchPublicEvents()
            events = result as? [EventModel] ?? []
        } catch {
            errorMessage = (error as NSError).localizedDescription
        }
        isLoading = false
    }
}
