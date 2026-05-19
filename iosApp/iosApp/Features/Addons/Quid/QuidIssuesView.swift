import SwiftUI
import Shared

/// Entry point for the Quid magazine addon.
/// Shows a vertical list of magazine issues (Numeri), each as a large cover card.
struct QuidIssuesView: View {
    @State private var issues: [QuidIssue] = []
    @State private var refreshing = false
    @State private var appeared = false
    @State private var sub: Closeable?

    var body: some View {
        ScrollView {
            LazyVStack(spacing: 20) {
                ForEach(Array(issues.enumerated()), id: \.element.id) { idx, issue in
                    // Destination inline: vedi commento in
                    // `PodcastsListView`. Stesso motivo (push ambiguo con
                    // doppia registrazione di `.navigationDestination`).
                    Group {
                        if let pdfUrlString = issue.pdfUrl,
                           let pdfUrl = URL(string: pdfUrlString) {
                            NavigationLink {
                                QuidPDFViewer(url: pdfUrl, title: issue.name)
                            } label: {
                                QuidIssueCard(issue: issue)
                            }
                        } else {
                            NavigationLink {
                                QuidIssueView(issueId: issue.id, issueName: issue.name)
                            } label: {
                                QuidIssueCard(issue: issue)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 14)
                    .animation(
                        .spring(response: 0.5, dampingFraction: 0.86)
                            .delay(Double(min(idx, 12)) * 0.06),
                        value: appeared
                    )
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .refreshable { await refresh() }
        .overlay {
            if issues.isEmpty {
                if refreshing {
                    LoadingDots()
                } else {
                    ContentUnavailableView(
                        tr("addons.quid.issues_empty", fallback: "Nessun numero"),
                        systemImage: "books.vertical.fill",
                        description: Text(tr(
                            "addons.quid.issues_empty_description",
                            fallback: "Non sono ancora disponibili numeri di Quid."
                        ))
                    )
                }
            }
        }
        .navigationTitle(tr("addons.quid.title", fallback: "Quid"))
        .cleanNavBar()
        .task {
            start()
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
        .onDisappear { stop() }
    }

    // MARK: - Data

    private func start() {
        sub?.close()
        sub = FlowBridgeKt.subscribe(
            flow: koin.quid.observeIssues(),
            onEach: { value in
                Task { @MainActor in
                    self.issues = (value as? [QuidIssue]) ?? []
                }
            },
            onError: { _ in }
        )
        Task { await refresh() }
    }

    private func stop() { sub?.close(); sub = nil }

    private func refresh() async {
        refreshing = true
        defer { refreshing = false }
        do { try await koin.quid.refreshIssues() } catch { }
    }
}

// MARK: - Issue Card

struct QuidIssueCard: View {
    let issue: QuidIssue

    private var coverURL: URL? {
        guard let raw = issue.coverImageUrl, !raw.isEmpty else { return nil }
        return URL(string: raw)
    }

    /// Splits "Quid 16 - La Fine" into ("Quid 16", "La Fine").
    /// Returns (nil, fullName) if separator is absent.
    private var splitName: (number: String?, theme: String) {
        let separator = " - "
        if let range = issue.name.range(of: separator) {
            let number = String(issue.name[issue.name.startIndex ..< range.lowerBound])
            let theme  = String(issue.name[range.upperBound...])
            return (number, theme)
        }
        return (nil, issue.name)
    }

    private var articleCountText: String {
        let count = issue.articleCount
        if count == 1 {
            return tr("addons.quid.article_count_one", fallback: "1 articolo")
        } else {
            let template = tr("addons.quid.article_count_other", fallback: "%lld articoli")
            return String(format: template, Int64(count))
        }
    }

    var body: some View {
        GlassCard(padding: 0, cornerRadius: 18) {
            VStack(alignment: .leading, spacing: 0) {
                // Cover image — portrait 3:4, magazine feel.
                Color.clear
                    .aspectRatio(3 / 4, contentMode: .fit)
                    .overlay {
                        CachedAsyncImage(url: coverURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                                .overlay(
                                    Image(systemName: "books.vertical.fill")
                                        .font(.system(size: 40, weight: .semibold))
                                        .foregroundStyle(.white.opacity(0.85))
                                )
                        }
                    }
                    .overlay(alignment: .topTrailing) {
                        if issue.pdfUrl != nil {
                            HStack(spacing: 4) {
                                Image(systemName: "doc.fill")
                                    .font(.caption2.weight(.semibold))
                                Text("PDF")
                                    .font(.caption.weight(.bold))
                            }
                            .foregroundStyle(.primary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 5)
                            .background(.regularMaterial, in: Capsule())
                            .padding(10)
                        }
                    }
                    .clipped()
                    .clipShape(
                        UnevenRoundedRectangle(
                            topLeadingRadius: 18,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 0,
                            topTrailingRadius: 18,
                            style: .continuous
                        )
                    )

                // Text content
                VStack(alignment: .leading, spacing: 6) {
                    let parts = splitName
                    if let number = parts.number {
                        Text(number)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                            .textCase(.uppercase)
                    }

                    Text(parts.theme)
                        .font(.title2.weight(.bold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)

                    if issue.pdfUrl != nil {
                        Text(tr("addons.quid.pdf_issue", fallback: "Numero in PDF"))
                            .font(.subheadline.italic())
                            .foregroundStyle(.secondary)
                    } else {
                        Text(articleCountText)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

// MARK: - Routes

struct QuidIssueRoute: Hashable {
    let issueId: Int64
    let issueName: String
}

/// Tap a PDF issue when the actual PDF URL isn't known up-front (search hits
/// carry only the deep link `mensa://quid-pdf/<n>`). The loader subscribes to
/// `koin.quid.observeIssues()` and resolves to the real `QuidIssue` (id `-n`).
struct QuidPDFDeepLinkRoute: Hashable {
    let recordId: Int64
}

struct QuidPDFRoute: Hashable {
    let url: String
    let title: String
}
