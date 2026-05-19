import SwiftUI
import Shared

@MainActor
@Observable
final class NotificationManagerViewModel {
    var notifyEvents = true
    var notifyMessages = true
    var notifyGeneral = true
    /// Multi-select set of region names. Persisted as a JSON string array
    /// under `notify_me_events` — matches Flutter's contract.
    var selectedRegions: Set<String> = []
    var loading = false
    var saving = false

    /// Session-stable: cambia solo a login/logout.
    private var userId: String {
        (koin.auth.currentUser.value as? UserModel)?.id ?? ""
    }

    static let regions: [String] = [
        "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
        "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
        "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
        "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto"
    ]

    func load() async {
        loading = true
        defer { loading = false }

        if !userId.isEmpty {
            _ = try? await koin.metadata.refresh(userId: userId)
        }
        let raw = koin.metadata.get(key: "notify_me_events") ?? ""
        self.selectedRegions = Self.parseRegions(raw)
        self.notifyEvents = (koin.metadata.get(key: "notify_events") ?? "true") == "true"
        self.notifyMessages = (koin.metadata.get(key: "notify_messages") ?? "true") == "true"
        self.notifyGeneral = (koin.metadata.get(key: "notify_general") ?? "true") == "true"
    }

    /// Flutter writes `notify_me_events` as a JSON array of region names.
    private static func parseRegions(_ raw: String) -> Set<String> {
        let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty,
              let data = trimmed.data(using: .utf8),
              let arr = try? JSONDecoder().decode([String].self, from: data)
        else { return [] }
        return Set(arr)
    }

    private static func encodeRegions(_ set: Set<String>) -> String {
        let arr = Array(set)
        guard let data = try? JSONEncoder().encode(arr),
              let s = String(data: data, encoding: .utf8)
        else { return "[]" }
        return s
    }

    func toggleRegion(_ region: String, enabled: Bool) {
        if enabled { selectedRegions.insert(region) } else { selectedRegions.remove(region) }
        let json = Self.encodeRegions(selectedRegions)
        Task { await save(key: "notify_me_events", value: json) }
    }

    func save(key: String, value: String) async {
        guard !userId.isEmpty else { return }
        saving = true
        defer { saving = false }
        try? await koin.metadata.set(userId: userId, key: key, value: value)
    }
}

struct NotificationManagerView: View {
    @State private var vm = NotificationManagerViewModel()

    var body: some View {
        Form {
            Section(tr("notifications.manager.section_types", fallback: "Tipi di notifica")) {
                Toggle(tr("notifications.manager.events", fallback: "Eventi"), isOn: Binding(
                    get: { vm.notifyEvents },
                    set: { v in
                        vm.notifyEvents = v
                        Task { await vm.save(key: "notify_events", value: String(v)) }
                    }
                ))
                Toggle(tr("notifications.manager.messages", fallback: "Messaggi"), isOn: Binding(
                    get: { vm.notifyMessages },
                    set: { v in
                        vm.notifyMessages = v
                        Task { await vm.save(key: "notify_messages", value: String(v)) }
                    }
                ))
                Toggle(tr("notifications.manager.general", fallback: "Generali"), isOn: Binding(
                    get: { vm.notifyGeneral },
                    set: { v in
                        vm.notifyGeneral = v
                        Task { await vm.save(key: "notify_general", value: String(v)) }
                    }
                ))
            }

            Section {
                ForEach(NotificationManagerViewModel.regions, id: \.self) { region in
                    Toggle(region, isOn: Binding(
                        get: { vm.selectedRegions.contains(region) },
                        set: { v in vm.toggleRegion(region, enabled: v) }
                    ))
                }
            } header: {
                Text(tr("notifications.manager.section_region", fallback: "Regioni eventi"))
            } footer: {
                Text(tr("notifications.manager.region_hint",
                        fallback: "Ricevi notifiche per eventi nelle regioni selezionate."))
            }
        }
        .navigationTitle(tr("notifications.manager.title", fallback: "Preferenze notifiche"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load() }
    }
}

#Preview {
    NavigationStack { NotificationManagerView() }
}
