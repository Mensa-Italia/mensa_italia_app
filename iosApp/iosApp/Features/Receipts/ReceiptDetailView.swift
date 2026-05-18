import SwiftUI
import Shared
import SafariServices

@MainActor
@Observable
final class ReceiptDetailViewModel {
    var receipt: ReceiptModel? = nil
    var loading = true
    var error: String? = nil
    var downloadingPDF = false
    var pdfURL: URL? = nil

    func load(id: String) async {
        loading = true
        defer { loading = false }
        do {
            receipt = try await koin.receipts.getById(id: id)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func downloadPDF(id: String) async {
        downloadingPDF = true
        defer { downloadingPDF = false }
        do {
            let urlString = try await koin.receipts.getReceiptUrl(id: id)
            if let url = URL(string: urlString) {
                self.pdfURL = url
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct ReceiptDetailView: View {
    let receiptId: String
    @State private var vm = ReceiptDetailViewModel()
    @State private var showSafari = false

    private var dateString: String {
        guard let r = vm.receipt else { return "-" }
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .full
        f.timeStyle = .short
        return f.string(from: Date(timeIntervalSince1970: Double(r.created.epochSeconds)))
    }

    var body: some View {
        Group {
            if vm.loading && vm.receipt == nil {
                ProgressView()
            } else if let r = vm.receipt {
                content(r)
            } else if let err = vm.error {
                ContentUnavailableView(err, systemImage: "exclamationmark.triangle")
            } else {
                ContentUnavailableView(
                    tr("receipts.not_found", fallback: "Ricevuta non trovata"),
                    systemImage: "doc.text"
                )
            }
        }
        .navigationTitle(tr("receipts.detail.title", fallback: "Ricevuta"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load(id: receiptId) }
        .sheet(isPresented: $showSafari) {
            if let url = vm.pdfURL {
                SafariView(url: url)
            }
        }
        .onChange(of: vm.pdfURL) { _, newValue in
            if newValue != nil { showSafari = true }
        }
    }

    @ViewBuilder
    private func content(_ r: ReceiptModel) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 12) {
                        Image(systemName: r.kind.icon)
                            .font(.title)
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                        Text(tr(r.kind.labelKey, fallback: r.kind.fallback))
                            .font(.title2.bold())
                        Spacer()
                    }
                    Text(r.amountFormatted)
                        .font(.system(size: 44, weight: .heavy, design: .rounded))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    HStack(spacing: 6) {
                        Circle().fill(r.statusColor).frame(width: 8, height: 8)
                        Text(r.status.capitalized)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(r.statusColor)
                    }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
                .glassEffect(.regular, in: .rect(cornerRadius: 20))

                VStack(alignment: .leading, spacing: 14) {
                    if let desc = r.description_, !desc.isEmpty {
                        infoRow(icon: "text.alignleft", label: tr("receipts.description", fallback: "Descrizione"), value: desc)
                    }
                    infoRow(icon: "calendar", label: tr("receipts.date", fallback: "Data"), value: dateString)
                    if !r.stripeCode.isEmpty {
                        infoRow(icon: "number", label: tr("receipts.stripe", fallback: "Codice Stripe"), value: r.stripeCode)
                    }
                }
                .padding(16)
                .glassEffect(.regular, in: .rect(cornerRadius: 16))

                Button {
                    Task { await vm.downloadPDF(id: r.id) }
                } label: {
                    HStack {
                        if vm.downloadingPDF {
                            ProgressView().tint(.white)
                        } else {
                            Image(systemName: "arrow.down.doc.fill")
                        }
                        Text(tr("receipts.download_pdf", fallback: "Scarica PDF"))
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                }
                .buttonStyle(.borderedProminent)
                .tint(AppTheme.Colors.brandPrimary)
                .disabled(vm.downloadingPDF)
            }
            .padding(20)
        }
    }

    private func infoRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .frame(width: 24)
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption).foregroundStyle(.secondary)
                Text(value).font(.subheadline).textSelection(.enabled)
            }
            Spacer()
        }
    }
}

struct SafariView: UIViewControllerRepresentable {
    let url: URL
    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }
    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}
