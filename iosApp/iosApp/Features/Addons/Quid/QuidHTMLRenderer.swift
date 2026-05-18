import Foundation
import UIKit
import Shared

/// A piece of an article body — text, image, heading, or pull-quote.
/// The detail view renders these top-to-bottom, picking native widgets per kind
/// so we get inline images and editorial typography without a WebView.
enum QuidBodyBlock: Identifiable {
    case text(AttributedString)
    case image(url: URL, alt: String?)
    case heading(AttributedString)
    case quote(AttributedString)

    var id: String {
        switch self {
        case .text(let a):     return "t:\(a.characters.count):\(a.description.hashValue)"
        case .image(let u, _): return "i:\(u.absoluteString)"
        case .heading(let a):  return "h:\(a.characters.count):\(a.description.hashValue)"
        case .quote(let a):    return "q:\(a.characters.count):\(a.description.hashValue)"
        }
    }

    /// True for `.text` and `.heading` blocks whose underlying string is empty after trimming.
    var isEffectivelyEmpty: Bool {
        switch self {
        case .text(let a), .heading(let a), .quote(let a):
            return String(a.characters).trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        case .image:
            return false
        }
    }
}

/// Converts a WordPress HTML body string into a sequence of native blocks.
///
/// Why: rendering everything as one `AttributedString` (a) drops `<img>` tags and
/// (b) bakes a foreground colour into the run attributes that breaks dark mode.
/// Splitting on `<img>`, `<h2>`, and `<blockquote>` lets us render each natively
/// with editorial typography (serif body, pull-quotes, etc.) while preserving
/// SwiftUI's `.foregroundStyle(.primary)` colour adaptation.
///
/// Must run on main actor — UIKit font metrics require it.
@MainActor
enum QuidHTMLRenderer {
    static func parse(html: String) -> [QuidBodyBlock] {
        // Block identification lives in shared KMP code (`QuidHtmlParser`):
        // regex-split on <img>, then on <blockquote>/<h2..h4>. This Swift
        // layer owns the UIKit/SwiftUI rendering of each identified block.
        let parsed = Shared.QuidHtmlParser.shared.parse(html: html)
        var blocks: [QuidBodyBlock] = []
        blocks.reserveCapacity(parsed.count)
        for b in parsed {
            if let t = b as? QuidHtmlBlockText {
                if let a = renderText(t.html, kind: .body) { blocks.append(.text(a)) }
            } else if let img = b as? QuidHtmlBlockImage {
                if let url = URL(string: img.url) {
                    blocks.append(.image(url: url, alt: img.alt))
                }
            } else if let q = b as? QuidHtmlBlockBlockquote {
                if let a = renderText(q.html, kind: .quote) { blocks.append(.quote(a)) }
            } else if let h = b as? QuidHtmlBlockHeading {
                if let a = renderText(h.html, kind: .heading) { blocks.append(.heading(a)) }
            }
        }
        return blocks.filter { !$0.isEffectivelyEmpty }
    }

    // MARK: - Text rendering

    enum TextKind { case body, heading, quote }

    private static func renderText(_ html: String, kind: TextKind) -> AttributedString? {
        let trimmed = html.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return nil }

        let bodyFont = UIFont.preferredFont(forTextStyle: .body)
        let baseSize = bodyFont.pointSize
        let sizeStr = String(format: "%.1f", baseSize)
        // NOTE: we intentionally do NOT set a `color:` here. The HTML→NSAttributedString
        // bridge defaults to black, which we strip in the run-attribute pass below so
        // SwiftUI's `.foregroundStyle(.primary)` controls colour (adaptive light/dark).
        // Font family is set to the system default here, then replaced post-parse with
        // a serif-design UIFont so we get New York (Apple's editorial serif).
        let wrapped = """
        <html><head><meta charset="utf-8">
        <style>
          body       { font-family: -apple-system; font-size: \(sizeStr)px; margin: 0; padding: 0; }
          a          { color: #184295; }
          h1, h2, h3 { font-weight: bold; }
          blockquote { padding-left: 0; }
          em, i      { font-style: italic; }
          strong, b  { font-weight: bold; }
          /* WordPress often wraps song lyrics / preformatted text in <pre>, which
             defaults to white-space: pre and refuses to wrap — that single long
             line then drags the whole article column wider than the screen.
             Force soft-wrap and break long tokens. */
          pre, code  { white-space: pre-wrap; word-break: break-word; overflow-wrap: anywhere; font-family: inherit; }
        </style></head><body>\(html)</body></html>
        """

        guard let data = wrapped.data(using: .utf8),
              let nsAttr = try? NSMutableAttributedString(
                data: data,
                options: [
                    .documentType: NSAttributedString.DocumentType.html,
                    .characterEncoding: String.Encoding.utf8.rawValue
                ],
                documentAttributes: nil
              )
        else { return nil }

        let full = NSRange(location: 0, length: nsAttr.length)

        // Strip baked-in foreground colours so SwiftUI's foregroundStyle(.primary)
        // wins (otherwise text stays black on dark backgrounds).
        nsAttr.enumerateAttribute(.foregroundColor, in: full) { value, range, _ in
            guard let color = value as? UIColor else { return }
            if isLinkBlue(color) { return }
            nsAttr.removeAttribute(.foregroundColor, range: range)
        }

        // Replace every font with its serif-design counterpart at an editorial size,
        // preserving the bold/italic traits the HTML bridge already set.
        nsAttr.enumerateAttribute(.font, in: full) { value, range, _ in
            guard let f = value as? UIFont else { return }
            let traits = f.fontDescriptor.symbolicTraits
            let isBold = traits.contains(.traitBold)
            let isItalic = traits.contains(.traitItalic)

            let targetSize: CGFloat
            switch kind {
            case .body:    targetSize = baseSize
            case .heading: targetSize = baseSize * 1.35
            case .quote:   targetSize = baseSize * 1.15
            }

            nsAttr.addAttribute(
                .font,
                value: serifFont(size: targetSize, bold: isBold || kind == .heading, italic: isItalic || kind == .quote),
                range: range
            )
        }

        // Paragraph style pass: force word-wrap on every paragraph (some HTML
        // sources — e.g. song lyrics in <pre> — come in with .byClipping or
        // similar, which would let a single long line drag the column wider
        // than the screen), and inject paragraph spacing for body kind so
        // multiple <p> don't read as one wall of text.
        let paragraphGap: CGFloat = kind == .body ? 14 : 0
        nsAttr.enumerateAttribute(.paragraphStyle, in: full) { value, range, _ in
            let mutable: NSMutableParagraphStyle
            if let existing = value as? NSParagraphStyle {
                // swiftlint:disable:next force_cast
                mutable = existing.mutableCopy() as! NSMutableParagraphStyle
            } else {
                mutable = NSMutableParagraphStyle()
            }
            mutable.lineBreakMode = .byWordWrapping
            if paragraphGap > 0 {
                mutable.paragraphSpacing = paragraphGap
            }
            nsAttr.addAttribute(.paragraphStyle, value: mutable, range: range)
        }

        // Trim leading/trailing newlines that the HTML parser tends to add.
        var attr = AttributedString(nsAttr)
        let chars = attr.characters
        var startIdx = chars.startIndex
        while startIdx < chars.endIndex, chars[startIdx].isNewline || chars[startIdx].isWhitespace {
            startIdx = chars.index(after: startIdx)
        }
        var endIdx = chars.endIndex
        while endIdx > startIdx {
            let prev = chars.index(before: endIdx)
            if chars[prev].isNewline || chars[prev].isWhitespace {
                endIdx = prev
            } else { break }
        }
        if startIdx != chars.startIndex || endIdx != chars.endIndex {
            attr = AttributedString(attr[startIdx..<endIdx])
        }
        return attr
    }

    /// Build a UIFont in serif design (New York on iOS) with traits.
    private static func serifFont(size: CGFloat, bold: Bool, italic: Bool) -> UIFont {
        var desc = UIFont.systemFont(ofSize: size).fontDescriptor.withDesign(.serif)
            ?? UIFont.systemFont(ofSize: size).fontDescriptor
        var traits: UIFontDescriptor.SymbolicTraits = []
        if bold { traits.insert(.traitBold) }
        if italic { traits.insert(.traitItalic) }
        if !traits.isEmpty, let withTraits = desc.withSymbolicTraits(traits) {
            desc = withTraits
        }
        return UIFont(descriptor: desc, size: size)
    }

    private static func isLinkBlue(_ color: UIColor) -> Bool {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &b, alpha: &a)
        return b > 0.45 && b > r && b > g * 0.9
    }

}
