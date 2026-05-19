import SwiftUI
import Shared

struct NotificationDetailView: View {
    let notification: NotificationModel

    @State private var target: NotificationTarget?

    private var title: String { notificationTitle(notification) }
    private var body_: String { notificationBody(notification) }

    private var formattedDate: String {
        let d = Date(timeIntervalSince1970: Double(notification.created.epochSeconds))
        return d.formatted(
            .dateTime
                .weekday(.wide)
                .day().month(.wide).year()
                .hour().minute()
                .locale(Locale(identifier: "it_IT"))
        )
    }

    /// Human-readable label for the CTA, mirroring Flutter's deep-link types.
    private func ctaLabel(for target: NotificationTarget) -> String {
        switch target {
        case .event:              return tr("notifications.go_event", fallback: "Vai all'evento")
        case .deal:               return tr("notifications.go_deal", fallback: "Vai all'offerta")
        case .singleDocument:     return tr("notifications.go_document", fallback: "Apri documento")
        case .multipleDocuments:  return tr("notifications.go_documents", fallback: "Vai ai documenti")
        case .ticketPurchase:     return tr("notifications.go_tickets", fallback: "Vai ai biglietti")
        case .paymentUpdateStatus: return tr("notifications.go_receipts", fallback: "Vai alle ricevute")
        case .quid:               return tr("notifications.go_quid", fallback: "Apri Quid")
        case .quidArticle:        return tr("notifications.go_quid_article", fallback: "Leggi articolo")
        case .quidPdf:            return tr("notifications.go_quid_pdf", fallback: "Apri PDF Quid")
        case .localOffice:        return tr("notifications.go_local_office", fallback: "Apri gruppo locale")
        case .accountConfirmation: return tr("notifications.go_account_confirmation", fallback: "Conferma identità")
        case .member:              return tr("notifications.go_member", fallback: "Apri socio")
        }
    }

    @ViewBuilder
    private func destinationView(for t: NotificationTarget) -> some View {
        switch t {
        case .event(let id):          EventDetailView(eventId: id)
        case .deal(let id):           DealDetailView(dealId: id)
        case .singleDocument(let id): DocumentDetailView(documentId: id)
        case .multipleDocuments:      AreaDocumentsView()
        case .ticketPurchase:         TicketsListView()
        case .paymentUpdateStatus:    ReceiptsListView()
        case .quid(let id):           QuidIssueView(issueId: Int64(id) ?? 0, issueName: "Quid")
        case .quidArticle(let id):    QuidArticleView(articleId: Int64(id) ?? 0)
        case .quidPdf(let id):        QuidPDFDeepLinkLoader(recordId: Int64(id) ?? 0)
        case .localOffice(let slug):  LocalOfficeBySlugLoader(slug: slug)
        case .accountConfirmation(let appId, let url, let nid):
            AccountConfirmationSheet(
                exAppId: appId,
                callbackUrl: url,
                notificationId: nid,
                onDismiss: { target = nil }
            )
        case .member(let id):         MemberDetailView(memberId: id)
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                HStack(spacing: 14) {
                    ZStack {
                        Circle()
                            .fill(AppTheme.Colors.brandPrimary.opacity(0.15))
                            .frame(width: 56, height: 56)
                        Image(systemName: notificationSystemIcon(for: notification))
                            .font(.title2)
                            .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                    }
                    VStack(alignment: .leading, spacing: 4) {
                        Text(title).font(.title3.bold())
                        Text(formattedDate).font(.caption).foregroundStyle(.secondary)
                    }
                }

                Divider()

                if !body_.isEmpty {
                    Text(body_)
                        .font(.body)
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if let t = notificationTarget(from: notification) {
                    Button {
                        target = t
                    } label: {
                        Label(ctaLabel(for: t), systemImage: "arrow.right.circle.fill")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(AppTheme.Colors.brandPrimary)
                }

                Spacer(minLength: 40)
            }
            .padding(20)
        }
        .navigationTitle(tr("notifications.detail.title", fallback: "Dettaglio"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(item: $target) { t in
            destinationView(for: t)
        }
        .task {
            if notification.seen == nil {
                try? await koin.notifications.markSeen(id: notification.id)
            }
        }
    }
}
