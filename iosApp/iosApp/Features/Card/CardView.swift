import SwiftUI
import Shared
import CoreImage.CIFilterBuiltins

// MARK: - QR Code View

struct QRCodeView: View {
    let payload: String
    let size: CGFloat
    var body: some View {
        if !payload.isEmpty, let image = generate() {
            Image(uiImage: image)
                .interpolation(.none)
                .resizable()
                .scaledToFit()
                .frame(width: size, height: size)
                .background(.white)
                .clipShape(.rect(cornerRadius: 12))
        } else {
            Image(systemName: "qrcode")
                .resizable().scaledToFit()
                .frame(width: size, height: size)
                .foregroundStyle(.secondary)
        }
    }
    private func generate() -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(payload.utf8)
        filter.correctionLevel = "M"
        guard let outputImage = filter.outputImage else { return nil }
        let scaleX = size * UIScreen.main.scale / outputImage.extent.size.width
        let scaled = outputImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleX))
        let context = CIContext()
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cg)
    }
}

// MARK: - Printable Card View (for ShareLink export)

struct CardShareImage {
    let name: String
    let memberId: String
    let expiry: String
    let qrPayload: String
}

/// Renders the same membership hero used in-app at a fixed size for share-sheet export.
struct PrintableCardView: View {
    let data: CardShareImage
    var body: some View {
        ZStack {
            Color.white
            MembershipCardHero(
                fullName: data.name,
                memberSince: "",
                expiry: data.expiry,
                memberId: data.memberId,
                avatarURL: nil,
                isFullScreen: true,
                avatarSize: 96
            )
            .padding(20)
        }
        .frame(width: 540, height: 340)
    }
}

// MARK: - Card View

/// "La tua tessera" — rebuilt under iOS 26 / Liquid Glass HIG.
/// Apple Wallet-flavored layout: hero pass at the top, grouped information
/// rows below, glass action buttons at the bottom. Adapts to light & dark.
struct CardView: View {
    @State private var vm = CardViewModel()
    @State private var showWalletSheet = false
    @State private var walletLoading = false
    @State private var walletError: String? = nil

    private var renewalURL: URL? {
        URL(string: "https://cloud32.mensa.it/rinnovo")
    }

    private var membershipExpired: Bool {
        guard let inst = vm.user?.expireMembership else { return false }
        return inst.epochSeconds < Int64(Date().timeIntervalSince1970)
    }

    var body: some View {
        ScrollView(.vertical, showsIndicators: false) {
            VStack(spacing: 24) {
                heroSection
                qrSection
                membershipSection
                navigationTilesSection
                actionButtons
                    .padding(.top, 4)
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle(tr("app.card.title", fallback: "La tua tessera"))
        .navigationBarTitleDisplayMode(.large)
        .tint(AppTheme.Colors.brandTintAdaptive)
        .sheet(isPresented: $showWalletSheet) {
            walletComingSoonSheet
        }
        .alert(
            tr("card.wallet.error_title", fallback: "Wallet"),
            isPresented: Binding(
                get: { walletError != nil },
                set: { if !$0 { walletError = nil } }
            ),
            presenting: walletError
        ) { _ in
            Button(tr("app.ok", fallback: "OK"), role: .cancel) { walletError = nil }
        } message: { msg in
            Text(msg)
        }
        .task { await vm.load() }
    }

    // MARK: - Hero

    private var heroSection: some View {
        MembershipCardHero(
            fullName: vm.fullName,
            memberSince: vm.memberSince,
            expiry: vm.expiry,
            memberId: vm.memberId,
            avatarURL: vm.avatarURL,
            isFullScreen: true,
            avatarSize: 90
        )
        .frame(height: 230)
        .shadow(color: .black.opacity(0.18), radius: 18, x: 0, y: 8)
    }

    // MARK: - QR

    private var qrSection: some View {
        VStack(spacing: 14) {
            Text(tr("app.card.show_coordinator", fallback: "Mostra al coordinatore"))
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .textCase(.uppercase)
                .tracking(1)

            QRCodeView(payload: vm.qrPayload, size: 180)
                .padding(16)
                .background(Color.white, in: RoundedRectangle(cornerRadius: 20, style: .continuous))

            Text("ID \(vm.memberId)")
                .font(.system(.footnote, design: .monospaced))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
        .padding(.horizontal, 20)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
        )
    }

    // MARK: - Membership info section (inset-grouped style)

    private var membershipSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionHeader(tr("card.membership", fallback: "Membership"))

            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("card.expires_on", fallback: "Scadenza"))
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Text(vm.expiry)
                        .font(.headline)
                        .foregroundStyle(.primary)
                }
                Spacer(minLength: 8)
                membershipStatusTrailing
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(.secondarySystemGroupedBackground))
            )
        }
    }

    @ViewBuilder
    private var membershipStatusTrailing: some View {
        if membershipExpired, let url = renewalURL {
            Link(destination: url) {
                Text(tr("card.renew", fallback: "Rinnova"))
                    .font(.subheadline.weight(.semibold))
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.regular)
            .tint(AppTheme.Colors.mensaBlue)
        } else {
            HStack(spacing: 6) {
                Image(systemName: "checkmark.seal.fill")
                    .font(.caption.weight(.semibold))
                Text(tr("card.active", fallback: "Attiva"))
                    .font(.caption.weight(.semibold))
            }
            .foregroundStyle(.green)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(Capsule().fill(Color.green.opacity(0.15)))
        }
    }

    // MARK: - Navigation tiles (inset-grouped style)

    private var navigationTilesSection: some View {
        VStack(spacing: 0) {
            NavigationLink {
                TicketsListView()
            } label: {
                navRow(icon: "ticket.fill",
                       tint: AppTheme.Colors.mensaBlue,
                       title: tr("card.my_tickets", fallback: "I miei ticket"))
            }
            .buttonStyle(.plain)

            Divider()
                .padding(.leading, 60)

            NavigationLink {
                ReceiptsListView()
            } label: {
                navRow(icon: "doc.text.fill",
                       tint: .orange,
                       title: tr("card.my_receipts", fallback: "Le mie ricevute"))
            }
            .buttonStyle(.plain)
        }
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(Color(.secondarySystemGroupedBackground))
        )
    }

    private func navRow(icon: String, tint: Color, title: String) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .fill(tint)
                    .frame(width: 30, height: 30)
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white)
            }
            Text(title)
                .font(.body)
                .foregroundStyle(.primary)
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(.tertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .contentShape(Rectangle())
    }

    // MARK: - Action buttons (Wallet + Share)

    @ViewBuilder
    private var actionButtons: some View {
        VStack(spacing: 12) {
            if WalletService.canAddPasses {
                walletButton
            }
            if vm.user != nil {
                shareButton
            }
        }
    }

    private var walletButton: some View {
        Button {
            Task { await addToWallet() }
        } label: {
            HStack(spacing: 8) {
                if walletLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    Image(systemName: "wallet.pass.fill")
                }
                Text(tr("card.add_to_wallet", fallback: "Aggiungi al Wallet"))
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .tint(AppTheme.Colors.mensaBlue)
        .disabled(walletLoading)
    }

    private var shareButton: some View {
        let img = cardImage()
        return ShareLink(
            item: img,
            preview: SharePreview("Tessera \(vm.fullName)", image: img)
        ) {
            Label(
                tr("app.card.share", fallback: "Condividi tessera"),
                systemImage: "square.and.arrow.up"
            )
            .fontWeight(.semibold)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .controlSize(.large)
        .tint(AppTheme.Colors.mensaBlue)
    }

    // MARK: - Helpers

    private func sectionHeader(_ text: String) -> some View {
        Text(text.uppercased())
            .font(.footnote.weight(.semibold))
            .foregroundStyle(.secondary)
            .tracking(0.3)
            .padding(.horizontal, 16)
            .padding(.bottom, 2)
            .accessibilityAddTraits(.isHeader)
    }

    private var walletComingSoonSheet: some View {
        VStack(spacing: 20) {
            Image(systemName: "wallet.pass.fill")
                .font(.system(size: 48))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .padding(.top, 40)
            Text(tr("card.wallet.coming_title", fallback: "Apple Wallet"))
                .font(.title2.bold())
            Text(tr("card.wallet.coming_body", fallback: "Presto potrai aggiungere la tessera al tuo Wallet."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
            Button {
                showWalletSheet = false
            } label: {
                Text(tr("app.ok", fallback: "OK"))
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .tint(AppTheme.Colors.brandPrimary)
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .presentationDetents([.medium])
    }

    @MainActor
    private func addToWallet() async {
        guard !walletLoading else { return }
        walletLoading = true
        defer { walletLoading = false }
        do {
            try await WalletService.presentAddMembershipPass()
        } catch WalletService.WalletError.notAvailable,
                WalletService.WalletError.fetchFailed(404),
                WalletService.WalletError.invalidPass {
            showWalletSheet = true
        } catch let err as WalletService.WalletError {
            walletError = err.errorDescription
        } catch {
            walletError = error.localizedDescription
        }
    }

    @MainActor
    private func cardImage() -> Image {
        let content = PrintableCardView(data: CardShareImage(
            name: vm.fullName,
            memberId: vm.memberId,
            expiry: vm.expiry,
            qrPayload: vm.qrPayload
        ))
        let renderer = ImageRenderer(content: content)
        renderer.scale = UIScreen.main.scale
        renderer.proposedSize = .init(width: 540, height: 340)
        if let uiImage = renderer.uiImage { return Image(uiImage: uiImage) }
        return Image(systemName: "person.text.rectangle")
    }
}

#Preview {
    NavigationStack {
        CardView()
    }
}
