import SwiftUI
#if os(iOS)
import UIKit
import CoreText

/// Registers the bundled Gotham-Bold.otf (shipped as an NSDataAsset in the
/// asset catalog) with CoreText and exposes its PostScript name. We keep this
/// out of pbxproj surgery by piggy-backing on Assets.xcassets, which is
/// already a folder-reference in the project.
enum GothamFont {
    /// PostScript name resolved after registration. nil until `register()` runs
    /// successfully, in which case callers fall back to the system font.
    static private(set) var boldPostScriptName: String?

    private static var didAttemptRegistration = false

    static func register() {
        guard !didAttemptRegistration else { return }
        didAttemptRegistration = true

        guard let asset = NSDataAsset(name: "GothamBold") else { return }
        guard let provider = CGDataProvider(data: asset.data as CFData),
              let cgFont = CGFont(provider) else { return }

        var error: Unmanaged<CFError>?
        if CTFontManagerRegisterGraphicsFont(cgFont, &error) {
            boldPostScriptName = cgFont.postScriptName as String?
        } else if let cfErr = error?.takeRetainedValue() {
            // "Already registered" is fine — recover the PS name anyway.
            let code = CFErrorGetCode(cfErr)
            if code == CTFontManagerError.alreadyRegistered.rawValue {
                boldPostScriptName = cgFont.postScriptName as String?
            }
        }
    }

    /// SwiftUI Font built on Gotham-Bold if registered, system black weight otherwise.
    static func bold(size: CGFloat) -> Font {
        if let name = boldPostScriptName {
            return .custom(name, size: size)
        }
        return .system(size: size, weight: .black)
    }
}
#else
enum GothamFont {
    static func register() {}
    static func bold(size: CGFloat) -> Font { .system(size: size, weight: .black) }
}
#endif
