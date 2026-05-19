import SwiftUI
import Shared
import StripePaymentSheet

/// Donation form. Stripe SDK is not integrated yet — donate() is called on the
/// repository but UX is gated behind a confirmation alert.
struct MakeDonationView: View {
    @State private var vm = MakeDonationViewModel()
    @State private var heartScale: CGFloat = 1.0
    @State private var appeared = false
    @State private var showResult = false
    @State private var resultMessage = ""
    @State private var showPicker = false

    private let presets: [Int] = [5, 10, 25, 50, 100]

    var body: some View {
        ScrollView {
            VStack(spacing: 28) {
                hero
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8), value: appeared)

                amountPicker
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.08),
                        value: appeared
                    )

                customAmountField
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.16),
                        value: appeared
                    )

                paymentPicker
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.24),
                        value: appeared
                    )

                donateButton
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(
                        .spring(response: 0.6, dampingFraction: 0.8).delay(0.32),
                        value: appeared
                    )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .navigationTitle(tr("views.make_donation.title", fallback: "Donazione"))
        .navigationBarTitleDisplayMode(.inline)
        .background(
            LinearGradient(
                colors: [
                    AppTheme.Colors.brandPrimary.opacity(0.05),
                    Color(.systemBackground)
                ],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .task {
            await vm.load()
            withAnimation { appeared = true }
            heartBeat()
        }
        .alert(tr("views.make_donation.title", fallback: "Donazione"),
               isPresented: $showResult) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(resultMessage)
        }
        .sheet(isPresented: $showPicker) {
            PaymentMethodPickerSheet(
                amountLabel: vm.amountString,
                onConfirm: { methodId in
                    showPicker = false
                    runDonation(paymentMethodId: methodId)
                },
                onDismiss: { showPicker = false }
            )
            .presentationDetents([.medium, .large])
        }
    }

    private var hero: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(AppTheme.brandGradient)
                    .frame(width: 110, height: 110)
                    .opacity(0.18)
                Image(systemName: "heart.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(
                        LinearGradient(
                            colors: [.pink, AppTheme.Colors.brandPrimary],
                            startPoint: .top, endPoint: .bottom
                        )
                    )
                    .scaleEffect(heartScale)
            }
            Text(tr("app.donate.headline",
                    fallback: "Supporta Mensa Italia"))
                .font(.title3.weight(.semibold))
            Text(tr("app.donate.subhead",
                    fallback: "Il tuo contributo aiuta a sostenere eventi, community e l'associazione."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 12)
        }
    }

    private var amountPicker: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(tr("app.donate.amount", fallback: "Importo"))
                .font(.headline)
            HStack(spacing: 10) {
                ForEach(presets, id: \.self) { value in
                    Button {
                        vm.selectPreset(value)
                    } label: {
                        Text("€\(value)")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(
                                vm.amount == value && !vm.usingCustom
                                ? AppTheme.Colors.brandPrimary
                                : Color(.secondarySystemBackground),
                                in: RoundedRectangle(cornerRadius: 14)
                            )
                            .foregroundStyle(
                                vm.amount == value && !vm.usingCustom
                                ? .white : .primary
                            )
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var customAmountField: some View {
        HStack {
            Text("€")
                .font(.title3.weight(.semibold))
                .foregroundStyle(.secondary)
            TextField(
                tr("app.donate.custom", fallback: "Importo personalizzato"),
                text: $vm.customAmountText
            )
            .keyboardType(.numberPad)
            .font(.title3.weight(.semibold))
            .onChange(of: vm.customAmountText) { _, _ in vm.applyCustom() }
        }
        .padding(14)
        .background(Color(.secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: 14))
    }

    private var paymentPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tr("app.donate.method", fallback: "Metodo di pagamento"))
                .font(.headline)
            if vm.methods.isEmpty {
                Text(tr("app.donate.no_methods",
                        fallback: "Nessun metodo disponibile. Verrà richiesto in fase di pagamento."))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground),
                                in: RoundedRectangle(cornerRadius: 14))
            } else {
                Picker(selection: $vm.selectedMethodId) {
                    ForEach(vm.methods, id: \.id) { m in
                        Text("\(m.brand.capitalized) \(m.display)").tag(m.id as String?)
                    }
                } label: {
                    Text(tr("app.donate.method", fallback: "Metodo"))
                }
                .pickerStyle(.menu)
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground),
                            in: RoundedRectangle(cornerRadius: 14))
            }
        }
    }

    private var donateButton: some View {
        Button {
            showPicker = true
        } label: {
            HStack {
                if vm.submitting {
                    ProgressView().tint(.white)
                } else {
                    Image(systemName: "heart.fill")
                    Text(tr("app.donate.cta", fallback: "Dona {amount}", ["amount": vm.amountString]))
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(AppTheme.brandGradient,
                        in: RoundedRectangle(cornerRadius: AppTheme.Radius.button))
            .foregroundStyle(.white)
        }
        .disabled(vm.submitting || vm.amount <= 0)
        .opacity(vm.amount <= 0 ? 0.5 : 1.0)
    }

    /// Two-step flow matching Flutter's MakeDonationViewModel.doTheDonation:
    /// 1. `/api/payment/donate` creates a PaymentIntent attached to the
    ///    customer's default method (off-session).
    /// 2. `STPPaymentHandler.confirmPayment` finalizes the charge, popping
    ///    a 3DS sheet only when the bank requires it.
    /// `paymentMethodId` is the one the picker confirmed — already set as
    /// default on the backend before we get here.
    private func runDonation(paymentMethodId: String) {
        vm.submitting = true
        Task { @MainActor in
            do {
                let resp = try await koin.paymentMethods.donate(
                    amountCents: Int32(vm.amount * 100)
                )
                guard !resp.paymentIntent.isEmpty else {
                    vm.submitting = false
                    resultMessage = tr("app.payments.error.no_payment_intent",
                                       fallback: "Impossibile inizializzare il pagamento.")
                    showResult = true
                    return
                }
                StripeService.confirmPayment(clientSecret: resp.paymentIntent) { result in
                    vm.submitting = false
                    switch result {
                    case .completed:
                        resultMessage = tr("app.donate.success",
                                           fallback: "Grazie per il tuo supporto!")
                        showResult = true
                    case .canceled:
                        break
                    case .failed(let error):
                        resultMessage = error.localizedDescription
                        showResult = true
                    }
                }
            } catch {
                vm.submitting = false
                resultMessage = error.localizedDescription
                showResult = true
            }
        }
    }

    private func heartBeat() {
        withAnimation(.easeInOut(duration: 0.6).repeatForever(autoreverses: true)) {
            heartScale = 1.12
        }
    }
}

@MainActor
@Observable
final class MakeDonationViewModel {
    var amount: Int = 10
    var customAmountText: String = ""
    var usingCustom = false
    var methods: [PaymentMethodModel] = []
    var selectedMethodId: String?
    var submitting = false
    var errorMessage: String?

    var amountString: String { "€\(amount)" }

    func load() async {
        do {
            let list = try await koin.paymentMethods.refresh()
            self.methods = list
            self.selectedMethodId = list.first?.id
        } catch {
            // silent — still allow donate flow
        }
    }

    func selectPreset(_ v: Int) {
        amount = v
        usingCustom = false
        customAmountText = ""
    }

    func applyCustom() {
        let digits = customAmountText.filter { $0.isNumber }
        if digits != customAmountText {
            customAmountText = digits
        }
        if let v = Int(digits), v > 0 {
            amount = v
            usingCustom = true
        } else if digits.isEmpty {
            usingCustom = false
            amount = 10
        }
    }

    @discardableResult
    func donate() async -> Bool {
        submitting = true
        defer { submitting = false }
        do {
            _ = try await koin.paymentMethods.donate(amountCents: Int32(amount * 100))
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }
}

#Preview {
    NavigationStack { MakeDonationView() }
}
