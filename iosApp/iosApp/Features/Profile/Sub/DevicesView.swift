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
                        DeviceRow(device: device)
                            .opacity(appeared ? 1 : 0)
                            .offset(y: appeared ? 0 : 14)
                            .animation(
                                .spring(response: 0.55, dampingFraction: 0.85)
                                    .delay(0.04 * Double(idx)),
                                value: appeared
                            )
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    Task { await vm.delete(device.id) }
                                } label: {
                                    Label(tr("app.devices.delete", fallback: "Elimina"),
                                          systemImage: "trash")
                                }
                            }
                    }
                } header: {
                    Text(tr("app.devices.section", fallback: "Dispositivi registrati"))
                } footer: {
                    Text(tr("app.devices.footer",
                            fallback: "Scorri verso sinistra per rimuovere un dispositivo."))
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

    private var subtitle: String {
        let lang = device.firebaseId.isEmpty ? "-" : String(device.firebaseId.prefix(10)) + "…"
        let when = formatItalianDate(device.updated)
        return "\(lang) · \(when)"
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: "iphone")
                .font(.system(size: 22, weight: .regular))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(device.deviceName.isEmpty
                     ? tr("app.devices.unknown", fallback: "Dispositivo")
                     : device.deviceName)
                    .font(.body.weight(.medium))
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

    private var sub: Closeable?

    func load() async {
        sub?.close()
        let flow = koin.devices.observeAll() as Kotlinx_coroutines_coreFlow
        sub = subscribeFlow(flow) { [weak self] (list: NSArray) in
            Task { @MainActor [weak self] in
                self?.devices = (list as? [DeviceModel]) ?? []
                self?.loading = false
            }
        }
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
            try await koin.devices.delete(id: id)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    NavigationStack { DevicesView() }
}
