import SwiftUI
import CoreLocation
import Shared

/// Lightweight `CLLocationManager` wrapper for one-shot, foreground location.
/// We don't need streaming updates — the user opens the sheet, picks a radius,
/// and applies. The most recent coordinate is cached on `EventFilterState`.
@MainActor @Observable
final class EventLocationProvider: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    var authorization: CLAuthorizationStatus
    var lastLocation: CLLocation?
    /// `true` while we're awaiting a coordinate after authorization.
    var requesting: Bool = false

    override init() {
        self.authorization = CLLocationManager().authorizationStatus
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    /// Request authorization if needed, then a single location fix.
    func requestLocation() {
        let status = manager.authorizationStatus
        authorization = status
        switch status {
        case .notDetermined:
            requesting = true
            manager.requestWhenInUseAuthorization()
        case .authorizedWhenInUse, .authorizedAlways:
            requesting = true
            manager.requestLocation()
        default:
            requesting = false
        }
    }

    // MARK: - CLLocationManagerDelegate

    nonisolated func locationManager(_ manager: CLLocationManager,
                                     didChangeAuthorization status: CLAuthorizationStatus) {
        Task { @MainActor in
            self.authorization = status
            if status == .authorizedWhenInUse || status == .authorizedAlways {
                self.requesting = true
                manager.requestLocation()
            } else {
                self.requesting = false
            }
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            self.authorization = manager.authorizationStatus
            if self.authorization == .authorizedWhenInUse || self.authorization == .authorizedAlways {
                self.requesting = true
                manager.requestLocation()
            } else {
                self.requesting = false
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager,
                                     didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        Task { @MainActor in
            self.lastLocation = loc
            self.requesting = false
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager,
                                     didFailWithError error: Error) {
        Task { @MainActor in
            self.requesting = false
        }
    }
}

/// The full-blown filter sheet. Edits a local copy of `EventFilterState` and
/// commits it to the binding on "Applica" — so dismissing without applying
/// behaves like cancel.
struct EventFiltersSheet: View {
    @Binding var state: EventFilterState
    @Environment(\.dismiss) private var dismiss

    @State private var draft: EventFilterState
    @State private var locationProvider = EventLocationProvider()

    init(state: Binding<EventFilterState>) {
        self._state = state
        self._draft = State(initialValue: state.wrappedValue)
    }

    /// Index of the currently selected distance step (last = unlimited).
    private var distanceStepIndex: Int {
        if let km = draft.maxDistanceKm,
           let i = DistanceSteps.kmValues.firstIndex(of: km) {
            return i
        }
        return DistanceSteps.kmValues.count // unlimited slot
    }

    var body: some View {
        NavigationStack {
            Form {
                typeSection
                distanceSection
                regionSection
            }
            .navigationTitle(tr("events.filter.title", fallback: "Filtri"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(tr("events.filter.reset", fallback: "Reset"), role: .destructive) {
                        draft.reset()
                    }
                    .disabled(draft.isEmpty)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(tr("events.filter.apply", fallback: "Applica")) {
                        state = draft
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
            .onChange(of: locationProvider.lastLocation) { _, loc in
                if let loc {
                    draft.userLatitude = loc.coordinate.latitude
                    draft.userLongitude = loc.coordinate.longitude
                }
            }
        }
    }

    // MARK: - Type section
    //
    // Pattern HIG canonico per multi-select inline: una riga `Toggle` per
    // ogni opzione (vedi Impostazioni → Notifiche → Permettere notifiche).
    // Le capsule/chip non sono uno standard Apple per i Form.

    private var typeSection: some View {
        Section {
            ForEach(EventType.allCases, id: \.self) { type in
                Toggle(isOn: typeBinding(type)) {
                    Label(type.label, systemImage: type.systemImage)
                }
            }
        } header: {
            Text(tr("events.filter.section.type", fallback: "Tipo evento"))
        } footer: {
            if draft.types.isEmpty {
                Text(tr("events.filter.type.hint",
                        fallback: "Nessuna selezione = mostra tutto"))
            }
        }
    }

    private func typeBinding(_ type: EventType) -> Binding<Bool> {
        Binding(
            get: { draft.types.contains(type) },
            set: { newValue in
                if newValue { draft.types.insert(type) } else { draft.types.remove(type) }
            }
        )
    }

    // MARK: - Distance section

    private var distanceSection: some View {
        Section {
            Toggle(isOn: Binding(
                get: { draft.useMyLocation },
                set: { newValue in
                    draft.useMyLocation = newValue
                    if newValue {
                        locationProvider.requestLocation()
                        if draft.maxDistanceKm == nil {
                            draft.maxDistanceKm = 50
                        }
                    }
                }
            )) {
                Label(tr("events.filter.distance.use_my_location",
                         fallback: "Usa la mia posizione"),
                      systemImage: "location.fill")
            }

            if draft.useMyLocation {
                switch locationProvider.authorization {
                case .denied, .restricted:
                    locationDeniedRow
                case .notDetermined:
                    HStack(spacing: 8) {
                        ProgressView().controlSize(.small)
                        Text(tr("events.filter.distance.requesting_permission",
                                fallback: "Richiesta permesso in corso…"))
                            .foregroundStyle(.secondary)
                    }
                default:
                    distanceSlider
                }
            }
        } header: {
            Text(tr("events.filter.section.distance", fallback: "Distanza"))
        } footer: {
            if draft.useMyLocation,
               locationProvider.authorization == .authorizedWhenInUse ||
               locationProvider.authorization == .authorizedAlways {
                Text(tr("events.filter.distance.footer",
                        fallback: "Gli eventi online vengono nascosti quando filtri per distanza."))
            }
        }
    }

    private var locationDeniedRow: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tr("events.filter.distance.denied.title",
                    fallback: "Posizione disattivata"))
                .font(.subheadline.weight(.semibold))
            Text(tr("events.filter.distance.denied.body",
                    fallback: "Attiva la posizione in Impostazioni per filtrare gli eventi vicini a te."))
                .font(.footnote)
                .foregroundStyle(.secondary)
            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Label(tr("events.filter.distance.denied.cta", fallback: "Apri Impostazioni"),
                      systemImage: "gearshape")
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private var distanceSlider: some View {
        VStack(alignment: .leading, spacing: 8) {
            LabeledContent(
                tr("events.filter.distance.radius", fallback: "Raggio")
            ) {
                Text(DistanceSteps.label(for: draft.maxDistanceKm))
                    .monospacedDigit()
            }

            Slider(
                value: Binding(
                    get: { Double(distanceStepIndex) },
                    set: { newValue in
                        let i = Int(newValue.rounded())
                        if i >= DistanceSteps.kmValues.count {
                            draft.maxDistanceKm = nil
                        } else {
                            draft.maxDistanceKm = DistanceSteps.kmValues[i]
                        }
                    }
                ),
                in: 0...Double(DistanceSteps.kmValues.count),
                step: 1
            ) {
                Text(tr("events.filter.distance.radius", fallback: "Raggio"))
            } minimumValueLabel: {
                Text(tr("events.filter.distance.min", fallback: "5 km"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } maximumValueLabel: {
                Text(tr("events.filter.distance.unlimited", fallback: "∞"))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
    }

    // MARK: - Region section
    //
    // 20 regioni in multi-select → pattern HIG: NavigationLink che apre una
    // List con righe Toggle (come Impostazioni → Lingua, Mail → Filtri).
    // La riga riassuntiva mostra lo stato corrente.

    private var regionSection: some View {
        Section {
            NavigationLink {
                RegionPicker(selection: $draft.regions)
            } label: {
                LabeledContent(
                    tr("events.filter.section.region", fallback: "Regione")
                ) {
                    Text(regionSummary)
                        .foregroundStyle(.secondary)
                }
            }
        } footer: {
            Text(tr("events.filter.region.footer",
                    fallback: "Confrontato con l'indirizzo dell'evento."))
        }
    }

    private var regionSummary: String {
        switch draft.regions.count {
        case 0:  return tr("events.filter.region.all", fallback: "Tutte")
        case 1:  return draft.regions.first ?? ""
        default: return String(
            format: tr("events.filter.region.count", fallback: "%d selezionate"),
            draft.regions.count
        )
        }
    }
}

// MARK: - Region picker

/// Sub-screen `List` con righe Toggle per ogni regione, searchable.
/// Pattern HIG (Impostazioni → Lingua e regione).
private struct RegionPicker: View {
    @Binding var selection: Set<String>
    @State private var query = ""

    private var filtered: [String] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return ItalianRegions.all }
        let needle = q.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
        return ItalianRegions.all.filter { r in
            r.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: .current)
                .contains(needle)
        }
    }

    var body: some View {
        List {
            if !selection.isEmpty {
                Section {
                    Button("Deseleziona tutte", role: .destructive) {
                        selection.removeAll()
                    }
                }
            }
            Section {
                ForEach(filtered, id: \.self) { region in
                    Button {
                        if selection.contains(region) {
                            selection.remove(region)
                        } else {
                            selection.insert(region)
                        }
                    } label: {
                        HStack {
                            Text(region)
                                .foregroundStyle(.primary)
                            Spacer()
                            if selection.contains(region) {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(Color.accentColor)
                                    .fontWeight(.semibold)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .automatic))
        .navigationTitle(tr("events.filter.section.region", fallback: "Regione"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
