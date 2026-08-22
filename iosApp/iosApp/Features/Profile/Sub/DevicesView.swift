import SwiftUI
import Shared

/// Lists registered devices for the current user. Cache-first via SQLDelight Flow.
struct DevicesView: View {
    @State private var vm = DevicesViewModel()
    @State private var appeared = false

    var body: some View {
        List {
            if vm.devices.isEmpty && !vm.loading {
                Section {
                    emptyState
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            } else {
                Section {
                    ForEach(Array(vm.devices.enumerated()), id: \.element.id) { idx, device in
                        let isCurrent = vm.isCurrent(device)
                        DeviceRow(device: device, isCurrent: isCurrent)
                            .opacity(appeared ? 1 : 0)
                            .offset(y: appeared ? 0 : 14)
                            .animation(
                                .spring(response: 0.55, dampingFraction: 0.85)
                                    .delay(0.04 * Double(idx)),
                                value: appeared
                            )
                            // Niente swipe-to-delete sull'iPhone in uso:
                            // sganciarsi da soli lascia senza notifiche e la
                            // riga torna al primo `ensureRegistered`. Per
                            // sganciare questo telefono si usa un altro
                            // dispositivo.
                            .swipeActions(edge: .trailing, allowsFullSwipe: !isCurrent) {
                                if !isCurrent {
                                    Button(role: .destructive) {
                                        Task { await vm.delete(device.id) }
                                    } label: {
                                        Label(tr("app.devices.delete", fallback: "Elimina"),
                                              systemImage: "trash")
                                    }
                                }
                            }
                    }
                } header: {
                    Text(tr("app.devices.section", fallback: "Dispositivi registrati"))
                } footer: {
                    Text(tr("app.devices.footer",
                            fallback: "Scorri verso sinistra per rimuovere un dispositivo. Quello in uso non si può rimuovere da qui."))
                }
            }
        }
        .navigationTitle(tr("views.devices.title", fallback: "Dispositivi"))
        .navigationBarTitleDisplayMode(.large)
        .overlay {
            if vm.loading && vm.devices.isEmpty {
                ProgressView().controlSize(.large)
            }
        }
        .refreshable { await vm.refresh() }
        .task {
            await vm.load()
            withAnimation { appeared = true }
        }
        .alert(tr("app.error.title", fallback: "Errore"),
               isPresented: .init(get: { vm.errorMessage != nil },
                                  set: { if !$0 { vm.errorMessage = nil } })) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "iphone.slash")
                .font(.system(size: 48))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            Text(tr("app.devices.empty.title", fallback: "Nessun dispositivo"))
                .font(.headline)
            Text(tr("app.devices.empty.message",
                    fallback: "I tuoi dispositivi registrati appariranno qui."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 36)
    }
}

private struct DeviceRow: View {
    let device: DeviceModel
    let isCurrent: Bool

    private var subtitle: String {
        let lang = device.firebaseId.isEmpty ? "-" : String(device.firebaseId.prefix(10)) + "…"
        let when = formatAppDate(device.updated)
        return "\(lang) · \(when)"
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: isCurrent ? "iphone.badge.play" : "iphone")
                .font(.system(size: 22, weight: .regular))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(device.deviceName.isEmpty
                         ? tr("app.devices.unknown", fallback: "Dispositivo")
                         : device.deviceName)
                        .font(.body.weight(.medium))
                    if isCurrent {
                        Text(tr("app.devices.current", fallback: "Questo dispositivo"))
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 7).padding(.vertical, 2)
                            .background(AppTheme.Colors.brandTintAdaptive.opacity(0.15), in: Capsule())
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                }
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer()
        }
        .padding(.vertical, 4)
    }
}

@MainActor
@Observable
final class DevicesViewModel {
    var devices: [DeviceModel] = []
    var loading = true
    var errorMessage: String?

    /// Token FCM di questo iPhone. La riga che ce l'ha è "questo dispositivo"
    /// e non offre la rimozione.
    var currentFirebaseId: String?

    private var sub: Closeable?

    func isCurrent(_ device: DeviceModel) -> Bool {
        guard let mine = currentFirebaseId, !mine.isEmpty else { return false }
        return device.firebaseId == mine
    }

    func load() async {
        sub?.close()
        let flow = koin.devices.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.devices = (list as? [DeviceModel]) ?? []
                self?.loading = false
            }
        }
        currentFirebaseId = try? await koin.devices.currentFirebaseId()
        await refresh()
    }

    func refresh() async {
        do {
            try await koin.devices.refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    func delete(_ id: String) async {
        do {
            // Risponde false sul dispositivo in uso; per quello la lista non
            // offre nemmeno lo swipe, quindi qui il valore non serve.
            _ = try await koin.devices.delete(id: id)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    NavigationStack { DevicesView() }
}
