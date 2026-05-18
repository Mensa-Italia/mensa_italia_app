import SwiftUI
import Shared
import StripePaymentSheet

/// Saved payment methods. `koin.paymentMethods.list()` is in-memory; we call
/// `refresh()` on appear and observe the underlying StateFlow.
struct PaymentMethodsView: View {
    @State private var vm = PaymentMethodsViewModel()
    @State private var appeared = false
    @State private var stripeMessage: String? = nil

    var body: some View {
        List {
            if vm.methods.isEmpty && !vm.loading {
                Section {
                    emptyState
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            } else {
                Section(tr("app.payments.section", fallback: "Metodi salvati")) {
                    ForEach(Array(vm.methods.enumerated()), id: \.element.id) { idx, m in
                        PaymentMethodRow(
                            method: m,
                            isDefault: vm.defaultId == m.id,
                            setDefault: { Task { await vm.setDefault(m.id) } }
                        )
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 12)
                        .animation(
                            .spring(response: 0.55, dampingFraction: 0.85)
                                .delay(0.05 * Double(idx)),
                            value: appeared
                        )
                    }
                }
            }

            Section {
                Button {
                    addMethod()
                } label: {
                    HStack {
                        if vm.adding {
                            ProgressView().controlSize(.small)
                        } else {
                            Image(systemName: "plus.circle.fill")
                        }
                        Text(tr("app.payments.add",
                                fallback: "Aggiungi metodo di pagamento"))
                            .fontWeight(.medium)
                    }
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                .disabled(vm.adding)
            }
        }
        .navigationTitle(tr("app.payments.title", fallback: "Pagamenti"))
        .navigationBarTitleDisplayMode(.large)
        .overlay {
            if vm.loading && vm.methods.isEmpty {
                ProgressView().controlSize(.large)
            }
        }
        .refreshable { await vm.refresh() }
        .task {
            await vm.load()
            withAnimation { appeared = true }
        }
        .alert(tr("app.payments.add", fallback: "Aggiungi metodo"),
               isPresented: .init(get: { stripeMessage != nil },
                                  set: { if !$0 { stripeMessage = nil } })) {
            Button("OK", role: .cancel) { stripeMessage = nil }
        } message: {
            Text(stripeMessage ?? "")
        }
        .alert(tr("app.error.title", fallback: "Errore"),
               isPresented: .init(get: { vm.errorMessage != nil },
                                  set: { if !$0 { vm.errorMessage = nil } })) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    private func addMethod() {
        vm.adding = true
        StripeService.addPaymentMethod { result in
            vm.adding = false
            switch result {
            case .completed:
                Task { await vm.refresh() }
                stripeMessage = tr("app.payments.added",
                                   fallback: "Metodo di pagamento aggiunto.")
            case .canceled:
                break
            case .failed(let error):
                vm.errorMessage = error.localizedDescription
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "creditcard")
                .font(.system(size: 48))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            Text(tr("app.payments.empty.title", fallback: "Nessun metodo salvato"))
                .font(.headline)
            Text(tr("app.payments.empty.message",
                    fallback: "Aggiungi una carta per gestire i pagamenti."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 36)
    }
}

private struct PaymentMethodRow: View {
    let method: PaymentMethodModel
    let isDefault: Bool
    let setDefault: () -> Void

    private var iconName: String {
        switch method.brand.lowercased() {
        case "visa": return "creditcard.fill"
        case "mastercard": return "creditcard.fill"
        case "amex", "american express": return "creditcard.fill"
        default: return "creditcard"
        }
    }

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: iconName)
                .font(.system(size: 22))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(method.brand.isEmpty ? "Carta" : method.brand.capitalized)
                        .font(.body.weight(.medium))
                    if isDefault {
                        Text(tr("app.payments.default", fallback: "Predefinita"))
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(AppTheme.Colors.brandSecondary.opacity(0.2),
                                        in: Capsule())
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                }
                Text(method.display.isEmpty ? "•••• ••••" : method.display)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if !isDefault {
                Button(tr("app.payments.make_default", fallback: "Default")) {
                    setDefault()
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .tint(AppTheme.Colors.brandTintAdaptive)
            }
        }
        .padding(.vertical, 4)
    }
}

@MainActor
@Observable
final class PaymentMethodsViewModel {
    var methods: [PaymentMethodModel] = []
    var defaultId: String? = nil
    var loading = true
    var adding = false
    var errorMessage: String? = nil

    func load() async {
        await refresh()
    }

    func refresh() async {
        do {
            let list = try await koin.paymentMethods.refresh()
            self.methods = list
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    func setDefault(_ id: String) async {
        do {
            try await koin.paymentMethods.setDefault(paymentMethodId: id)
            defaultId = id
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    NavigationStack { PaymentMethodsView() }
}
