import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

/// App-wide design tokens. Mensa Italia visual identity per Brandbook 2020:
/// • Blu primario  #184295  C98 M80 Y0 K0   (80% of usage)
/// • Azzurro       #6AC9F0  C56 M0 Y2 K0    (20% of usage)
/// • Nero testi    #575656  (80% black, per brandbook)
/// • Gradiente blu→azzurro consigliato
enum AppTheme {
    enum Colors {
        // Brandbook 2020 — official colors
        static let mensaBlue       = Color(red: 24/255, green: 66/255, blue: 149/255)   // #184295
        static let mensaBlueDeep   = Color(red: 12/255, green: 34/255, blue: 95/255)   // shaded for depth
        static let mensaCyan       = Color(red: 106/255, green: 201/255, blue: 240/255)   // #6AC9F0
        static let mensaInk        = Color(red: 87/255, green: 86/255, blue: 86/255)   // #575656

        // Backdrop neutrals
        static let backdropDark    = Color(red: 6/255, green: 17/255, blue: 46/255)   // night
        static let parchment       = Color(red: 252/255, green: 251/255, blue: 247/255)

        // Semantic aliases
        static let brandPrimary    = mensaBlue
        static let brandSecondary  = mensaCyan
        static let accent          = mensaCyan
        static let background      = backdropDark
        static let surfaceText     = Color.white
        static let subtleText      = Color.white.opacity(0.65)

        /// **Adaptive brand tint** for icons, toolbar action items, links and
        /// any small foreground glyph that must contrast against the system
        /// background in BOTH light and dark mode.
        ///
        /// • Light mode → `mensaBlue` (#184295) — same as `brandPrimary`,
        ///   WCAG AA against white.
        /// • Dark mode → `mensaCyan` (#6AC9F0) — same brand family, WCAG AA
        ///   against black; `mensaBlue` is too dark on dark backgrounds and
        ///   collapses visually into the surface.
        ///
        /// Use this **anywhere `brandPrimary` was being applied as a tint or
        /// foreground style on interactive controls**. For backgrounds
        /// (gradients, filled shapes) keep `mensaBlue` / `brandGradient` — the
        /// brand color is correct as a *background* in both appearances.
        static let brandTintAdaptive: Color = {
            #if canImport(UIKit)
            return Color(UIColor { trait in
                switch trait.userInterfaceStyle {
                case .dark:
                    return UIColor(red: 106/255, green: 201/255, blue: 240/255, alpha: 1) // mensaCyan
                default:
                    return UIColor(red: 24/255, green: 66/255, blue: 149/255, alpha: 1) // mensaBlue
                }
            })
            #else
            return mensaBlue
            #endif
        }()
    }

    enum Spacing {
        static let xs: CGFloat = 4
        static let s: CGFloat = 8
        static let m: CGFloat = 16
        static let l: CGFloat = 24
        static let xl: CGFloat = 32
        static let xxl: CGFloat = 48
    }

    enum Radius {
        static let card: CGFloat = 24
        static let button: CGFloat = 16
        static let field: CGFloat = 18
    }

    /// Official brand gradient (blu → azzurro), per page 19 of the brandbook.
    static let brandGradient = LinearGradient(
        colors: [Colors.mensaBlue, Colors.mensaCyan],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    /// Deeper, evocative variant used for full-bleed surfaces (splash, login).
    static let brandGradientDeep = LinearGradient(
        colors: [Colors.mensaBlueDeep, Colors.mensaBlue, Colors.mensaCyan.opacity(0.85)],
        startPoint: .top, endPoint: .bottom
    )
}
