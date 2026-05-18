import SwiftUI
import Shared
import CoreImage
import CoreImage.CIFilterBuiltins
import UIKit

struct DealDetailView: View {
    let dealId: String

    @State private var vm = DealDetailViewModel()
    @State private var appeared = false
    @State private var copiedCode = false
    @State private var qrSheet = false
    @State private var editSheet = false
    @State private var confirmDelete = false
    @Environment(\.dismiss) private var dismiss

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        f.timeStyle = .none
        return f
    }()

    /// Mirrors `EventDetailView.canEditEvent`: read the auth StateFlow
    /// **synchronously** so the toolbar is correct on the very first frame
    /// — no waiting for a Flow collector to fire, no pop-in. Powers come
    /// from the JWT and never change mid-session.
    private var canEditDeal: Bool {
        let user = koin.auth.currentUser.value as? UserModel
        return hasAnyPower(["super", "admin", "deals", "deals_admin"], user: user)
    }

    var body: some View {
        Group {
            if vm.loading && vm.deal == nil {
                LoadingDots()
            } else if let deal = vm.deal {
                dealContent(deal)
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                LoadingDots()
            }
        }
        .navigationTitle(vm.deal?.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            // Share is ALWAYS in the toolbar — falls back to a placeholder
            // until vm.deal loads, mirroring EventDetailView.
            ToolbarItem(placement: .topBarTrailing) {
                ShareLink(item: shareText(vm.deal)) {
                    Image(systemName: "square.and.arrow.up")
                }
                .disabled(vm.deal == nil)
            }
            // Edit / Delete menu visible from the first frame for users with
            // the `deals` (or super/admin) power. Buttons are disabled until
            // vm.deal arrives so we have something to edit / delete.
            ToolbarItem(placement: .topBarTrailing) {
                if canEditDeal {
                    Menu {
                        Button {
                            editSheet = true
                        } label: {
                            Label(
                                tr("app.edit", fallback: "Modifica"),
                                systemImage: "pencil"
                            )
                        }
                        .disabled(vm.deal == nil)
                        Button(role: .destructive) {
                            confirmDelete = true
                        } label: {
                            Label(
                                tr("app.delete", fallback: "Elimina"),
                                systemImage: "trash"
                            )
                        }
                        .disabled(vm.deal == nil)
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                    .disabled(vm.deleting)
                }
            }
        }
        .sheet(isPresented: $editSheet) {
            if let d = vm.deal {
                NavigationStack { AddDealView(deal: d) }
            }
        }
        .confirmationDialog(
            tr("addons.deals.delete.confirm",
               fallback: "Eliminare questa convenzione?"),
            isPresented: $confirmDelete,
            titleVisibility: .visible
        ) {
            Button(tr("app.delete", fallback: "Elimina"), role: .destructive) {
                Task {
                    let ok = await vm.delete()
                    if ok { dismiss() }
                }
            }
            Button(tr("app.cancel", fallback: "Annulla"), role: .cancel) { }
        } message: {
            Text(tr(
                "addons.deals.delete.confirm_msg",
                fallback: "L'azione non è reversibile."
            ))
        }
        .task {
            vm.start(id: dealId)
            withAnimation(.easeOut(duration: 0.45)) { appeared = true }
        }
        .onDisappear { vm.stop() }
    }

    /// Share payload — uses the deal URL when the model has arrived,
    /// otherwise an empty placeholder so the toolbar slot keeps its size.
    private func shareText(_ deal: DealModel?) -> String {
        guard let d = deal else { return "" }
        if let link = d.link, !link.isEmpty { return link }
        return d.name
    }

    private func hasAnyPower(_ keys: [String], user: UserModel?) -> Bool {
        guard let u = user else { return false }
        let set = Set(keys)
        return u.powers.contains { set.contains($0) }
    }

    // MARK: - Layout

    @ViewBuilder
    private func dealContent(_ deal: DealModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                heroImage(for: deal)

                titleBlock(deal)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(.spring(response: 0.55, dampingFraction: 0.85).delay(0.08), value: appeared)

                if let details = deal.details, !details.isEmpty {
                    section(
                        title: tr("addons.deals.details.subblock.description.title", fallback: "Descrizione"), // i18n
                        icon: "text.alignleft"
                    ) {
                        Text(details).font(.body)
                    }
                    .modifier(StaggerIn(appeared: appeared, delay: 0.16))
                }

                if let who = deal.who, !who.isEmpty {
                    section(
                        title: tr("addons.deals.details.subblock.who.title", fallback: "A chi è rivolto"), // i18n
                        icon: "person.2"
                    ) {
                        Text(who).font(.body)
                    }
                    .modifier(StaggerIn(appeared: appeared, delay: 0.24))
                }

                validityBlock(deal)
                    .modifier(StaggerIn(appeared: appeared, delay: 0.32))

                if let howToGet = deal.howToGet, !howToGet.isEmpty {
                    section(
                        title: tr(
                            "addons.deals.details.subblock.howtoget.title",
                            fallback: "Come ottenere il deal"
                        ), // i18n
                        icon: "checkmark.seal"
                    ) {
                        Text(howToGet).font(.body)
                    }
                    .modifier(StaggerIn(appeared: appeared, delay: 0.40))
                }

                contactsBlock
                    .modifier(StaggerIn(appeared: appeared, delay: 0.48))

                actionsBlock(deal)
                    .modifier(StaggerIn(appeared: appeared, delay: 0.56))
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 32)
        }
        // Title / toolbar / edit-delete sheets live on the outer `body` so
        // they don't pop in after the deal fetch resolves.
        .sheet(isPresented: $qrSheet) {
            if let code = discountCode(for: deal) {
                qrSheetView(code: code, deal: deal)
                    .presentationDetents([.medium, .large])
            }
        }
    }

    // MARK: - Hero

    @ViewBuilder
    private func heroImage(for deal: DealModel) -> some View {
        let url: URL? = {
            guard let att = deal.attachment, !att.isEmpty else { return nil }
            return Files.url(collection: "deals", recordId: deal.id, filename: att, thumb: "1000x600")
        }()

        CachedAsyncImage(url: url) { img in
            img.resizable().aspectRatio(contentMode: .fill)
        } placeholder: {
            AppTheme.brandGradient
                .overlay(
                    Image(systemName: "tag.fill")
                        .font(.system(size: 64, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.85))
                )
        }
        .frame(maxWidth: .infinity)
        .frame(height: 220)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .opacity(appeared ? 1 : 0)
        .scaleEffect(appeared ? 1 : 1.04)
        .animation(.easeOut(duration: 0.5), value: appeared)
    }

    @ViewBuilder
    private func titleBlock(_ deal: DealModel) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(deal.name)
                .font(.title2.bold())

            HStack(spacing: 8) {
                if !deal.commercialSector.isEmpty {
                    Label(deal.commercialSector, systemImage: "building.2")
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(AppTheme.Colors.mensaBlue.opacity(0.10), in: Capsule())
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
                if deal.isActive {
                    Text(tr("app.deals.active", fallback: "Attivo")) // i18n
                        .font(.caption.weight(.medium))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(.green.opacity(0.18), in: Capsule())
                        .foregroundStyle(.green)
                }
                if let pill = discountPill(for: deal) {
                    Text(pill)
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(AppTheme.Colors.mensaCyan.opacity(0.25), in: Capsule())
                        .foregroundStyle(AppTheme.Colors.mensaBlueDeep)
                }
            }

            if let pos = deal.position {
                let label = pos.address.isEmpty ? pos.name : "\(pos.name) – \(pos.address)"
                Label(label, systemImage: "mappin.and.ellipse")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private func validityBlock(_ deal: DealModel) -> some View {
        if deal.starting != nil || deal.ending != nil {
            section(
                title: tr("app.deals.validity", fallback: "Validità"), // i18n
                icon: "calendar"
            ) {
                VStack(alignment: .leading, spacing: 4) {
                    if let start = deal.starting {
                        Text("\(tr("app.deals.from", fallback: "Dal")): \(formatted(start))") // i18n
                            .font(.subheadline)
                    }
                    if let end = deal.ending {
                        Text("\(tr("app.deals.until", fallback: "Fino al")): \(formatted(end))") // i18n
                            .font(.subheadline)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var contactsBlock: some View {
        if !vm.contacts.isEmpty {
            section(
                title: tr("addons.contacts.title", fallback: "Contatti"), // i18n
                icon: "person.crop.circle"
            ) {
                VStack(spacing: 10) {
                    ForEach(vm.contacts, id: \.id) { contact in
                        ContactRow(contact: contact)
                    }
                }
            }
        } else if vm.loadingContacts {
            HStack {
                ProgressView()
                Text(tr("app.deals.loading_contacts", fallback: "Carico contatti…")) // i18n
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 4)
        }
    }

    @ViewBuilder
    private func actionsBlock(_ deal: DealModel) -> some View {
        VStack(spacing: 12) {
            if let link = deal.link, !link.isEmpty, let url = URL(string: link) {
                Link(destination: url) {
                    HStack(spacing: 8) {
                        Image(systemName: "safari")
                        Text(tr("app.open_link", fallback: "Apri link")) // i18n
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                }
                .buttonStyle(.glassProminent)
            }

            if let code = discountCode(for: deal) {
                Button {
                    UIPasteboard.general.string = code
                    copiedCode = true
                    Task {
                        try? await Task.sleep(nanoseconds: 1_500_000_000)
                        copiedCode = false
                    }
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: copiedCode ? "checkmark" : "doc.on.doc")
                        Text(copiedCode
                             ? tr("app.copied", fallback: "Copiato")
                             : tr("app.deals.copy_code", fallback: "Copia codice {code}", ["code": code]) // i18n
                        )
                    }
                    .font(.subheadline.weight(.medium))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                }
                .buttonStyle(.glass)

                Button {
                    qrSheet = true
                } label: {
                    HStack(spacing: 8) {
                        Image(systemName: "qrcode")
                        Text(tr("app.deals.show_qr", fallback: "Mostra QR sconto")) // i18n
                    }
                    .font(.subheadline.weight(.medium))
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
                }
                .buttonStyle(.glass)
            }
        }
        .padding(.top, 4)
    }

    // MARK: - QR

    @ViewBuilder
    private func qrSheetView(code: String, deal: DealModel) -> some View {
        VStack(spacing: 20) {
            Text(deal.name)
                .font(.headline)
                .multilineTextAlignment(.center)
                .padding(.top, 24)

            if let img = generateQRCode(from: code) {
                Image(uiImage: img)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 240, height: 240)
                    .padding(16)
                    .background(.white, in: RoundedRectangle(cornerRadius: 20))
                    .shadow(color: .black.opacity(0.08), radius: 10, y: 4)
            }

            Text(code)
                .font(.title3.monospaced())
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(AppTheme.Colors.mensaBlue.opacity(0.08), in: Capsule())

            Text(tr("app.deals.qr_hint", fallback: "Mostra questo codice in cassa per ricevere lo sconto.")) // i18n
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.Colors.parchment.ignoresSafeArea())
    }

    // MARK: - Helpers

    private func formatted(_ instant: Kotlinx_datetimeInstant) -> String {
        let date = Date(timeIntervalSince1970: Double(instant.epochSeconds))
        return Self.dateFormatter.string(from: date)
    }

    /// Pull a `NN%` style pill out of the deal text.
    private func discountPill(for deal: DealModel) -> String? {
        let candidates = [deal.details, deal.who].compactMap { $0 }
        for text in candidates {
            if let range = text.range(of: #"(\d{1,3})\s?%"#, options: .regularExpression) {
                return String(text[range]).replacingOccurrences(of: " ", with: "")
            }
        }
        return nil
    }

    /// Heuristic discount-code extraction: look for `code: XXXX`,
    /// `codice: XXXX`, or an uppercase alphanumeric token of >=4 chars
    /// inside `howToGet`. Returns `nil` when nothing reliable is found.
    private func discountCode(for deal: DealModel) -> String? {
        let sources = [deal.howToGet, deal.details].compactMap { $0 }
        for text in sources {
            if let m = text.range(
                of: #"(?:codice|code)[:\s]+([A-Z0-9_-]{4,20})"#,
                options: [.regularExpression, .caseInsensitive]
            ) {
                let snippet = String(text[m])
                if let inner = snippet.range(of: #"[A-Z0-9_-]{4,20}$"#, options: .regularExpression) {
                    return String(snippet[inner])
                }
            }
        }
        return nil
    }

    private func generateQRCode(from string: String) -> UIImage? {
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(string.utf8)
        filter.correctionLevel = "M"
        guard let output = filter.outputImage else { return nil }
        let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))
        let context = CIContext()
        guard let cg = context.createCGImage(scaled, from: scaled.extent) else { return nil }
        return UIImage(cgImage: cg)
    }

    // MARK: - Section helper

    @ViewBuilder
    private func section<Content: View>(
        title: String,
        icon: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: icon)
                .font(.headline)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Contact row

private struct ContactRow: View {
    let contact: DealsContactModel

    var body: some View {
        GlassCard(padding: 14, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 6) {
                if !contact.name.isEmpty {
                    Text(contact.name)
                        .font(.subheadline.weight(.semibold))
                }
                if let note = contact.note, !note.isEmpty {
                    Text(note)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                HStack(spacing: 10) {
                    if !contact.email.isEmpty,
                       let url = URL(string: "mailto:\(contact.email)") {
                        Link(destination: url) {
                            Label(contact.email, systemImage: "envelope.fill")
                                .font(.caption)
                                .labelStyle(.titleAndIcon)
                        }
                    }
                    if let phone = contact.phoneNumber, !phone.isEmpty,
                       let url = URL(string: "tel:\(phone.replacingOccurrences(of: " ", with: ""))") {
                        Link(destination: url) {
                            Label(phone, systemImage: "phone.fill")
                                .font(.caption)
                                .labelStyle(.titleAndIcon)
                        }
                    }
                }
                .padding(.top, 2)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

// MARK: - Stagger helper

private struct StaggerIn: ViewModifier {
    let appeared: Bool
    let delay: Double

    func body(content: Content) -> some View {
        content
            .opacity(appeared ? 1 : 0)
            .offset(y: appeared ? 0 : 12)
            .animation(
                .spring(response: 0.55, dampingFraction: 0.85).delay(delay),
                value: appeared
            )
    }
}

#Preview {
    NavigationStack {
        DealDetailView(dealId: "preview-id")
    }
}
