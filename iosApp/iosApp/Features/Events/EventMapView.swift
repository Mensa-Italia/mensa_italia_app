import SwiftUI
import MapKit
import Shared

@MainActor @Observable
final class EventMapViewModel {
    var events: [EventModel] = []
    private var sub: Closeable?

    var geoEvents: [EventModel] {
        let now = Int64(Date().timeIntervalSince1970)
        return events.filter { $0.position != nil && $0.whenEnd.epochSeconds >= now }
    }

    func start() {
        guard sub == nil else { return }
        let flow = koin.events.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.events = (list as? [EventModel]) ?? []
            }
        }
        Task { try? await koin.events.refresh(filter: nil, sort: "when_end") }
    }

    func stop() { sub?.close(); sub = nil }
}

struct EventMapView: View {
    @State private var vm = EventMapViewModel()
    @State private var selected: EventModel?
    @Environment(\.dismiss) private var dismiss

    @State private var camera: MapCameraPosition = .region(
        MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: 41.9, longitude: 12.5),
            span: MKCoordinateSpan(latitudeDelta: 10, longitudeDelta: 10)
        )
    )

    var body: some View {
        Map(position: $camera, selection: Binding(
            get: { selected?.id },
            set: { id in
                selected = vm.geoEvents.first { $0.id == id }
            }
        )) {
            ForEach(vm.geoEvents, id: \.id) { e in
                if let pos = e.position {
                    Marker(e.name, systemImage: "calendar",
                           coordinate: CLLocationCoordinate2D(latitude: pos.lat, longitude: pos.lon))
                        .tint(e.isNational ? AppTheme.Colors.brandPrimary : AppTheme.Colors.brandSecondary)
                        .tag(e.id)
                }
            }
        }
        .mapStyle(.standard(elevation: .realistic))
        .ignoresSafeArea(edges: .bottom)
        .navigationTitle(tr("events.map.title", fallback: "Mappa eventi"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("app.done", fallback: "Fine")) { dismiss() }
            }
        }
        .sheet(item: $selected) { e in
            NavigationStack { previewSheet(e) }
                .presentationDetents([.fraction(0.45), .large])
        }
        .task { vm.start() }
        .onDisappear { vm.stop() }
    }

    @ViewBuilder
    private func previewSheet(_ e: EventModel) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            EventRowCard(event: e)
                .padding(.horizontal)
            NavigationLink {
                EventDetailView(eventId: e.id)
            } label: {
                HStack {
                    Image(systemName: "arrow.up.right")
                    Text(tr("events.map.open_detail", fallback: "Vedi dettagli"))
                }
                .frame(maxWidth: .infinity).frame(height: 50)
            }
            .buttonStyle(.glassProminent)
            .padding(.horizontal)
            Spacer()
        }
        .padding(.top, 20)
        .navigationTitle(e.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}

extension EventModel: @retroactive Identifiable {}

#Preview {
    NavigationStack { EventMapView() }
}
