import SwiftUI

/// The ranking's settings, from the gear beside ⓘ: one switch, to leave the
/// members' ranking. Saved in `users_metadata` through the shared
/// repository; the view then skips the member. Mirrors Android
/// `LeaderboardSettingsSheet`.
struct LeaderboardSettingsSheet: View {
    @State private var store = LeaderboardStore.shared
    @State private var saving = false
    @State private var failed = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Toggle(isOn: Binding(
                        get: { store.hidden },
                        set: { value in
                            Task {
                                saving = true
                                failed = await !store.setHidden(value)
                                saving = false
                            }
                        }
                    )) {
                        Text(tr("leaderboard.settings.hide", fallback: "Non apparire nella classifica dei soci"))
                    }
                    .disabled(saving)
                } footer: {
                    Text(tr("leaderboard.settings.hide.detail", fallback: "La tua riga non viene calcolata né mostrata, a nessuno: chi è dietro di te sale di posizione. I tuoi timbri contano comunque per le medaglie e per le classifiche di regioni e segretari. Puoi rientrare quando vuoi."))
                }
                if failed {
                    Text(tr("leaderboard.settings.failed", fallback: "Non sono riuscito a salvare. Riprova."))
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .listRowBackground(Color.clear)
                }
            }
            .navigationTitle(tr("leaderboard.settings", fallback: "Impostazioni"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { dismiss() } label: {
                        Label(tr("app.close", fallback: "Chiudi"), systemImage: "xmark")
                    }
                }
            }
            .task { await store.refreshHidden() }
        }
        .presentationDetents([.medium])
        .preferredColorScheme(.dark)
    }
}
