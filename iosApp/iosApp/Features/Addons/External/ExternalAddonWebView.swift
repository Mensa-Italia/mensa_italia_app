import SwiftUI
import WebKit
import Shared

/// State machine for the external addon launch flow.
///
/// We deliberately keep this very defensive — every step that can fail
/// (network, URL parsing, missing config) is captured as a `.failed` state
/// rather than throwing or producing a `nil` we'd later force-unwrap.
@MainActor @Observable
final class ExternalAddonViewModel {
    enum LoadState: Equatable {
        case idle
        case loading
        case ready(URL)
        case failed(String)
    }

    var state: LoadState = .idle
    var webViewLoading: Bool = false

    func load(addonId: String, baseUrl: String) async {
        // Trim and validate inputs up-front. Anything missing → error view.
        let trimmedBase = baseUrl.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedBase.isEmpty else {
            self.state = .failed(tr("addons.external.invalid_url", fallback: "Indirizzo non valido"))
            return
        }
        guard !addonId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            self.state = .failed(tr("addons.external.invalid_id", fallback: "ID addon non valido"))
            return
        }

        self.state = .loading

        // Fetch access data from KMP. The Kotlin call bridges as `throws`,
        // wrap in do/catch so a server error never crashes the UI.
        let accessData: AddonAccessData
        do {
            accessData = try await koin.addons.getAccessData(addonId: addonId)
        } catch {
            self.state = .failed((error as NSError).localizedDescription)
            return
        }

        // Build the URL safely.
        guard let url = Self.buildURL(base: trimmedBase, accessData: accessData) else {
            self.state = .failed(tr("addons.external.invalid_url", fallback: "Indirizzo non valido"))
            return
        }
        self.state = .ready(url)
    }

    /// Mirror Flutter's `Uri.replace(queryParameters: value)` behaviour:
    /// append every (name, value) pair returned by the server as a query
    /// parameter, preserving any params already present on `base`.
    static func buildURL(base: String, accessData: AddonAccessData) -> URL? {
        guard var comps = URLComponents(string: base) else { return nil }
        var items: [URLQueryItem] = comps.queryItems ?? []
        // `accessData.params` bridges to `NSDictionary` from Kotlin's
        // `Map<String, String>`. Cast defensively — if bridging produces
        // an unexpected shape we simply skip the offending entries.
        let dict = (accessData.params as? [String: String]) ?? [:]
        for (k, v) in dict where !k.isEmpty {
            items.append(URLQueryItem(name: k, value: v))
        }
        comps.queryItems = items.isEmpty ? nil : items
        return comps.url
    }
}

struct ExternalAddonWebView: View {
    let addonId: String
    let baseUrl: String
    var title: String = ""

    @State private var vm = ExternalAddonViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            switch vm.state {
            case .idle, .loading:
                ProgressView()
            case .ready(let url):
                WebViewContainer(url: url, onGoBack: { dismiss() }, loading: $vm.webViewLoading)
                    .ignoresSafeArea(edges: .bottom)
                if vm.webViewLoading {
                    VStack {
                        ProgressView()
                            .padding(8)
                            .background(.thinMaterial, in: Capsule())
                        Spacer()
                    }
                    .padding(.top, 8)
                }
            case .failed(let err):
                ContentUnavailableView(
                    tr("addons.external.error", fallback: "Impossibile aprire l'addon"),
                    systemImage: "exclamationmark.triangle",
                    description: Text(err)
                )
            }
        }
        .navigationTitle(title.isEmpty ? tr("addons.external.title", fallback: "Addon") : title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button { dismiss() } label: {
                    Image(systemName: "xmark")
                }
            }
        }
        .task { await vm.load(addonId: addonId, baseUrl: baseUrl) }
    }
}

private struct WebViewContainer: UIViewRepresentable {
    let url: URL
    let onGoBack: () -> Void
    @Binding var loading: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(onGoBack: onGoBack, loading: $loading)
    }

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        // Ephemeral data store — no shared cookies/cache leak between sessions,
        // and configuration is fully built before the WKWebView is constructed.
        cfg.websiteDataStore = .nonPersistent()
        let webView = WKWebView(frame: .zero, configuration: cfg)
        webView.navigationDelegate = context.coordinator
        webView.allowsBackForwardNavigationGestures = true
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        if let current = uiView.url, current != url {
            uiView.load(URLRequest(url: url))
        }
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        let onGoBack: () -> Void
        @Binding var loading: Bool

        init(onGoBack: @escaping () -> Void, loading: Binding<Bool>) {
            self.onGoBack = onGoBack
            self._loading = loading
        }

        func webView(_ webView: WKWebView,
                     decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            if let target = navigationAction.request.url?.absoluteString,
               target.contains("svc.mensa.it/goback") {
                decisionHandler(.cancel)
                Task { @MainActor in self.onGoBack() }
                return
            }
            decisionHandler(.allow)
        }

        func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
            Task { @MainActor in self.loading = true }
        }
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            Task { @MainActor in self.loading = false }
        }
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            Task { @MainActor in self.loading = false }
        }
        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            Task { @MainActor in self.loading = false }
        }
    }
}
