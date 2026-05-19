import SwiftUI
import Shared
import StripePaymentSheet

/// Inline payment-method picker. Mirrors Flutter's `PaymentMethodPicker`:
///
///   - On open, shows the default method + a "Paga {amount}" CTA.
///   - "Cambia metodo" toggles to a radio list of saved methods —
///     selecting one calls `/api/payment/default`.
///   - "Aggiungi metodo" runs Stripe's PaymentSheet in setup-intent mode
///     and reloads on success.
///   - On confirm, the caller receives the chosen payment method id and
///     triggers the donation (PaymentIntent + confirmPayment).
struct PaymentMethodPickerSheet: View {
    let amountLabel: String
    let onConfirm: (_ paymentMethodId: String) -> Void
    let onDismiss: () -> Void

    @State private var vm = PaymentMethodPickerViewModel()

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(tr("app.payments.title",
                                    fallback: "Metodo di pagamento"))
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(action: onDismiss) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.secondary)
                        }
                    }
                }
        }
        .task { await vm.load() }
        .alert("Errore",
               isPresented: Binding(
                get: { vm.errorMessage != nil },
                set: { if !$0 { vm.errorMessage = nil } }
               )) {
            Button("OK", role: .cancel) { vm.errorMessage = nil }
        } message: {
            Text(vm.errorMessage ?? "")
        }
    }

    @ViewBuilder
    private var content: some View {
        ScrollView {
            VStack(spacing: 12) {
                if vm.loading {
                    ProgressView()
                        .padding(.vertical, 40)
                } else {
                    let defaultMethod = vm.defaultMethod()

                    if defaultMethod == nil || vm.showPicker {
                        addMethodTile
                    }

                    if let m = defaultMethod, !vm.showPicker {
                        PaymentMethodRow(method: m, selected: true, showRadio: false)
                            .padding(.horizontal, 4)
                    }

                    if defaultMethod != nil && vm.showPicker {
                        VStack(spacing: 6) {
                            ForEach(vm.methods, id: \.id) { m in
                                PaymentMethodRow(
                                    method: m,
                                    selected: m.id == vm.defaultId,
                                    showRadio: true
                                )
                                .padding(.horizontal, 4)
                                .contentShape(Rectangle())
                                .onTapGesture { Task { await vm.selectMethod(id: m.id) } }
                            }
                        }
                    }

                    if !vm.showPicker {
                        Button {
                            if let m = defaultMethod { onConfirm(m.id) }
                        } label: {
                            Text(tr("app.payments.pay_amount",
                                    fallback: "Paga {amount}",
                                    ["amount": amountLabel]))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(AppTheme.brandGradient,
                                            in: RoundedRectangle(cornerRadius: 14))
                                .foregroundStyle(.white)
                                .fontWeight(.semibold)
                        }
                        .disabled(defaultMethod == nil)
                        .opacity(defaultMethod == nil ? 0.5 : 1.0)
                        .padding(.top, 8)

                        if !vm.methods.isEmpty {
                            Button(action: { vm.showPicker.toggle() }) {
                                Text(tr("app.payments.change_method",
                                        fallback: "Cambia metodo di pagamento"))
                                    .font(.footnote)
                            }
                            .padding(.top, 2)
                        }
                    } else {
                        Button(action: { vm.showPicker.toggle() }) {
                            Text(tr("app.common.back", fallback: "Indietro"))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }
                        .buttonStyle(.bordered)
                        .padding(.top, 8)
                    }
                }
            }
            .padding(20)
        }
    }

    private var addMethodTile: some View {
        Button {
            vm.addMethod()
        } label: {
            HStack {
                Text(tr("app.payments.add",
                        fallback: "Aggiungi metodo di pagamento"))
                    .fontWeight(.semibold)
                Spacer()
                Image(systemName: "plus.circle.fill")
                    .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            }
            .padding(16)
            .background(Color(.secondarySystemBackground),
                        in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(.primary)
        }
        .disabled(vm.adding)
    }
}

private struct PaymentMethodRow: View {
    let method: PaymentMethodModel
    let selected: Bool
    let showRadio: Bool

    var body: some View {
        HStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(Color.accentColor.opacity(0.15))
                Image(systemName: "creditcard.fill")
                    .foregroundStyle(.tint)
            }
            .frame(width: 36, height: 36)

            VStack(alignment: .leading, spacing: 2) {
                Text(method.brand.isEmpty
                     ? "Carta"
                     : method.brand.prefix(1).uppercased() + method.brand.dropFirst())
                    .font(.subheadline.weight(.semibold))
                Text(method.display.isEmpty ? "•••• ••••" : method.display)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if showRadio {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle")
                    .foregroundStyle(selected ? Color.accentColor : Color.secondary)
            }
        }
        .padding(14)
        .background(
            (selected
             ? Color.accentColor.opacity(0.1)
             : Color(.secondarySystemBackground)),
            in: RoundedRectangle(cornerRadius: 12)
        )
    }
}

// MARK: - View model

@MainActor
@Observable
final class PaymentMethodPickerViewModel {
    var loading = true
    var methods: [PaymentMethodModel] = []
    var defaultId: String?
    var showPicker = false
    var adding = false
    var errorMessage: String?

    func load() async {
        loading = true
        defer { loading = false }
        async let methodsTask: [PaymentMethodModel]? = try? await koin.paymentMethods.refresh()
        async let customerTask: StripeCustomerObject? = try? await koin.paymentMethods.customer()
        let m = await methodsTask ?? []
        let c = await customerTask
        methods = m
        defaultId = c?.defaultPaymentMethodId
    }

    func defaultMethod() -> PaymentMethodModel? {
        guard let id = defaultId else { return nil }
        return methods.first { $0.id == id }
    }

    func selectMethod(id: String) async {
        loading = true
        defer { loading = false }
        do {
            try await koin.paymentMethods.setDefault(paymentMethodId: id)
            defaultId = id
            showPicker = false
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func addMethod() {
        if adding { return }
        adding = true
        StripeService.addPaymentMethod { [weak self] result in
            guard let self else { return }
            adding = false
            switch result {
            case .completed:
                Task { await self.load() }
            case .canceled:
                break
            case .failed(let error):
                errorMessage = error.localizedDescription
            }
        }
    }
}
