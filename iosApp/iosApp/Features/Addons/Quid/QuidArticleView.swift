import SwiftUI
import Shared

/// Detail view for a single QUID magazine article — styled as an Italian
/// longform newspaper article ("articolo di giornale"): serif body, drop cap,
/// pull-quotes, hairline rules around the byline.
struct QuidArticleView: View {
    let articleId: Int64

    @State private var article: QuidArticle?
    @State private var loadError: String?
    @State private var blocks: [QuidBodyBlock] = []
    @State private var audio: QuidArticleAudio?

    /// Shared player singleton — drives the toolbar play/pause glyph state.
    @ObservedObject private var audioService = AudioPlayerService.shared

    private let bodyHPadding: CGFloat = 22

    /// Synchronous screen-width read. We resolve this once at body evaluation
    /// time and pass it as an explicit `.frame(width:)` to inline images.
    /// Previous attempts using `.containerRelativeFrame` or a `GeometryReader`
    /// in `.background` both failed: the geometry-derived width wasn't known on
    /// the first render pass, so the resizable image briefly took its intrinsic
    /// (huge) width and dragged the parent VStack(.leading) wider than the
    /// screen — and SwiftUI didn't always relayout it back when the width
    /// arrived a tick later.
    private var deviceWidth: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first?
            .screen.bounds.width ?? UIScreen.main.bounds.width
    }

    private var screenContentWidth: CGFloat {
        max(0, deviceWidth - 2 * bodyHPadding)
    }

    var body: some View {
        // Capture the available width once, store in @State, and feed it to
        // anything that needs an explicit width cap (inline images).
        // The GeometryReader itself doesn't render — it's a zero-frame overlay.
        Group {
            if let loadError {
                ContentUnavailableView(
                    tr("addons.quid.error", fallback: "Errore"),
                    systemImage: "exclamationmark.triangle",
                    description: Text(loadError)
                )
            } else if let article {
                content(article: article)
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        // No title text — the hero image is the visual anchor and the article
        // title is rendered inline as the headline below it. Transparent toolbar
        // so the cover image reads edge-to-edge to the top of the device; the
        // system back button keeps its Liquid Glass capsule and stays legible.
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.hidden, for: .navigationBar)
        .toolbar {
            // Audio play/pause — visible only when an audio narration record
            // exists. The glyph reflects whether THIS article is the one
            // currently playing in the shared player; tapping toggles or
            // starts playback (handing off the singleton to this article).
            if let article, let audio {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        toggleAudio(article: article, audio: audio)
                    } label: {
                        Image(systemName: toolbarAudioGlyph)
                            .accessibilityLabel(Text(
                                isThisArticlePlaying
                                    ? tr("addons.quid.audio.pause", fallback: "Pausa")
                                    : tr("addons.quid.audio.play", fallback: "Riproduci")
                            ))
                    }
                }
            }
            // Share the original WordPress URL out. The Liquid Glass capsule keeps
            // it readable against any hero background; we only show it once the
            // article is loaded so the share sheet never opens on an empty target.
            if let article, let url = URL(string: article.link) {
                ToolbarItem(placement: .topBarTrailing) {
                    ShareLink(item: url) {
                        Image(systemName: "square.and.arrow.up")
                            .accessibilityLabel(Text(tr("app.share", fallback: "Condividi")))
                    }
                }
            }
        }
        .task { await load() }
    }

    // MARK: - Content

    @ViewBuilder
    private func content(article: QuidArticle) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Hero cover image — bleeds to the very top edge of the device.
                // The `.ignoresSafeArea` on the ScrollView below pushes this whole
                // column up under the (transparent) navigation bar.
                heroImage(article: article)
                    .padding(.bottom, 20)

                VStack(alignment: .leading, spacing: 14) {
                    // Occhiello / kicker — single uppercase letter-spaced line.
                    if !article.categoryNames.isEmpty {
                        Text(article.categoryNames.joined(separator: " · ").uppercased())
                            .font(.caption2.weight(.semibold))
                            .tracking(1.6)
                            .foregroundStyle(AppTheme.Colors.mensaBlue)
                            .lineLimit(2)
                    }

                    // Title — big serif, tight leading, like a newspaper headline.
                    Text(article.titlePlain)
                        .font(.system(.largeTitle, design: .serif).weight(.bold))
                        .lineSpacing(-2)
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, 2)

                    // Byline / date row framed top + bottom by hairline rules.
                    bylineRow(article: article)
                        .padding(.top, 4)

                    // Audio narration banner — visible only when an audio
                    // record exists for this article. The banner subscribes to
                    // the shared `AudioPlayerService` so its state stays in sync.
                    if let audio {
                        QuidNarrationBanner(
                            audio: audio,
                            articleId: articleId,
                            articleTitle: article.titlePlain,
                            artworkURL: article.coverImageUrl.flatMap { URL(string: $0) }
                        )
                        .padding(.top, 4)
                    }

                    // Body
                    if !blocks.isEmpty {
                        bodyContent
                            .padding(.top, 4)
                    } else {
                        ProgressView(tr("addons.quid.loading", fallback: "Caricamento…"))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 20)
                    }

                    // Escape-hatch — quieter bordered style fits the editorial palette.
                    if let url = URL(string: article.link) {
                        Button {
                            UIApplication.shared.open(url)
                        } label: {
                            Label(
                                tr("addons.quid.open_on_site", fallback: "Apri sul sito"),
                                systemImage: "safari"
                            )
                            .font(.system(.subheadline, design: .serif).weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 4)
                        }
                        .buttonStyle(.bordered)
                        .tint(AppTheme.Colors.mensaBlue)
                        .padding(.top, 20)
                    }
                }
                .frame(width: screenContentWidth, alignment: .leading)
                .padding(.horizontal, bodyHPadding)
                .padding(.bottom, 32)
            }
            .frame(width: deviceWidth, alignment: .leading)
        }
        // Push the entire scroll content up under the (transparent) navigation bar
        // and the device top safe area so the hero image bleeds to the very top.
        .ignoresSafeArea(.container, edges: .top)
    }

    // MARK: - Byline row with newsprint rules

    @ViewBuilder
    private func bylineRow(article: QuidArticle) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            hairline
            Text(longDate(from: article.date))
                .font(.system(.footnote, design: .serif).italic())
                .foregroundStyle(.secondary)
                .padding(.vertical, 2)
            hairline
        }
    }

    private var hairline: some View {
        Rectangle()
            .fill(Color.primary.opacity(0.35))
            .frame(height: 0.5)
    }

    // MARK: - Body content (with drop cap + pull-quotes)

    @ViewBuilder
    private var bodyContent: some View {
        // Determine whether the first text block is eligible for a drop cap.
        let dropCapIndex: Int? = firstDropCapEligibleIndex()

        VStack(alignment: .leading, spacing: 16) {
            ForEach(Array(blocks.enumerated()), id: \.element.id) { idx, block in
                switch block {
                case .text(let attr):
                    if idx == dropCapIndex {
                        dropCapParagraph(attr)
                    } else {
                        Text(attr)
                            .lineSpacing(6)
                            .foregroundStyle(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .textSelection(.enabled)
                    }

                case .heading(let attr):
                    Text(attr)
                        .foregroundStyle(.primary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.top, 6)
                        .textSelection(.enabled)

                case .quote(let attr):
                    pullQuote(attr)

                case .image(let url, let alt):
                    inlineImage(url: url, alt: alt)
                }
            }
        }
    }

    /// Drop-cap rendering: enlarge the first character INSIDE the AttributedString
    /// so the whole paragraph remains a single `Text` and wraps correctly. A
    /// previous attempt put the initial in a separate Text inside an HStack — the
    /// HStack proposed infinite width to its children, so the remainder never
    /// wrapped and dragged the whole column wider than the screen.
    private func dropCapParagraph(_ attr: AttributedString) -> some View {
        Text(styledWithDropCap(attr))
            .lineSpacing(6)
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .textSelection(.enabled)
    }

    /// Enlarge the first letter of an AttributedString in-place; return the
    /// original if the first character isn't a letter.
    private func styledWithDropCap(_ attr: AttributedString) -> AttributedString {
        guard let first = String(attr.characters).first, first.isLetter else { return attr }
        var styled = attr
        if let r = styled.range(of: String(first)) {
            let bodyPt = UIFont.preferredFont(forTextStyle: .body).pointSize
            styled[r].font = .system(size: bodyPt * 2.6, weight: .bold, design: .serif)
        }
        return styled
    }

    /// Centred italic serif callout with thin rules — classic pull-quote.
    @ViewBuilder
    private func pullQuote(_ attr: AttributedString) -> some View {
        VStack(spacing: 10) {
            Rectangle()
                .fill(Color.primary.opacity(0.4))
                .frame(width: 60, height: 0.75)
            Text(attr)
                .multilineTextAlignment(.center)
                .lineSpacing(5)
                .foregroundStyle(.primary.opacity(0.85))
                .textSelection(.enabled)
            Rectangle()
                .fill(Color.primary.opacity(0.4))
                .frame(width: 60, height: 0.75)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .padding(.horizontal, 4)
    }

    /// Inline image with optional italic caption (newspaper-style).
    ///
    /// Width is bound to the scroll container's width minus body padding via
    /// `containerRelativeFrame(.horizontal)`. The simpler `.frame(maxWidth: .infinity)`
    /// did NOT work because the SwiftUI propagation chain
    ///   ScrollView → VStack(.leading) → VStack(.leading) → resizable image
    /// has no enforced upper bound: a `.leading` VStack adopts its widest child's
    /// intrinsic width, so a big image dragged the whole column wider than the screen.
    @ViewBuilder
    private func inlineImage(url: URL, alt: String?) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            CachedAsyncImage(url: url) { img in
                img.resizable()
                    .aspectRatio(contentMode: .fit)
            } placeholder: {
                Rectangle()
                    .fill(Color.secondary.opacity(0.12))
                    .aspectRatio(16/9, contentMode: .fit)
            }
            .frame(width: screenContentWidth)
            .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
            .accessibilityLabel(Text(alt ?? ""))

            if let alt, !alt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text(alt)
                    .font(.system(.caption, design: .serif).italic())
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    // MARK: - Drop-cap eligibility

    /// Returns the index of the first text block eligible for drop-cap treatment,
    /// or `nil` if the article opens with something else (image, heading, quote).
    private func firstDropCapEligibleIndex() -> Int? {
        for (i, block) in blocks.enumerated() {
            switch block {
            case .text:
                return i
            case .image, .heading, .quote:
                return nil
            }
        }
        return nil
    }

    // MARK: - Hero (parallax stretchy, mirrors EventDetailView)

    @ViewBuilder
    private func heroImage(article: QuidArticle) -> some View {
        let coverURL: URL? = {
            guard let raw = article.coverImageUrl, !raw.isEmpty else { return nil }
            return URL(string: raw)
        }()

        GeometryReader { geo in
            let minY = geo.frame(in: .global).minY
            let stretch = max(0, minY) // grows when user pulls the scroll down
            ZStack(alignment: .bottom) {
                Group {
                    if let coverURL {
                        CachedAsyncImage(url: coverURL) { img in
                            img.resizable().aspectRatio(contentMode: .fill)
                        } placeholder: {
                            AppTheme.brandGradient
                        }
                    } else {
                        AppTheme.brandGradient
                            .overlay(
                                Image(systemName: "magazine.fill")
                                    .font(.system(size: 48, weight: .semibold))
                                    .foregroundStyle(.white.opacity(0.85))
                            )
                    }
                }
                .frame(width: geo.size.width, height: geo.size.height + stretch)
                .clipped()

                // Subtle bottom-fade so the page title below sits on a darker
                // base — matches the EventDetailView treatment.
                LinearGradient(
                    colors: [.black.opacity(0), .black.opacity(0.45)],
                    startPoint: .top, endPoint: .bottom
                )
                .frame(width: geo.size.width, height: geo.size.height + stretch)
            }
            .frame(width: geo.size.width, height: geo.size.height + stretch)
            .clipped()
            .offset(y: -stretch)
        }
        .frame(height: 360)
    }

    // MARK: - Helpers

    private func longDate(from iso: String) -> String {
        guard let date = QuidDateParser.parse(iso) else { return iso }
        let fmt = DateFormatter()
        fmt.locale = Locale(identifier: "it_IT")
        fmt.dateStyle = .long
        fmt.timeStyle = .none
        return fmt.string(from: date)
    }

    // MARK: - Audio integration

    private var isThisArticleLoaded: Bool {
        audioService.currentTrack?.id == QuidAudioFactory.trackId(articleId: articleId)
    }

    private var isThisArticlePlaying: Bool {
        isThisArticleLoaded && audioService.isPlaying
    }

    private var toolbarAudioGlyph: String {
        isThisArticlePlaying ? "pause.fill" : "play.fill"
    }

    private func toggleAudio(article: QuidArticle, audio: QuidArticleAudio) {
        if isThisArticleLoaded {
            audioService.toggle()
            return
        }
        // Different article (or first-time playback) — hand the singleton
        // off to this article. `play(_:)` no-ops on identical ids, so we
        // never re-fetch on a redundant tap.
        if let track = QuidAudioFactory.makeTrack(
            audio: audio,
            articleId: articleId,
            articleTitle: article.titlePlain,
            artworkURL: article.coverImageUrl.flatMap { URL(string: $0) }
        ) {
            audioService.play(track)
        }
    }

    // MARK: - Loading

    private func load() async {
        do {
            let fetched = try await koin.quid.getArticle(id: articleId)
            article = fetched
            await MainActor.run {
                self.blocks = QuidHTMLRenderer.parse(html: fetched.contentHtml)
            }
        } catch {
            loadError = error.localizedDescription
        }
        // Audio narration is best-effort: a fetch failure must NOT block the
        // article view. The banner simply doesn't appear if `audio` stays nil.
        self.audio = try? await koin.quid.getAudioForArticle(wpId: articleId)
    }
}
