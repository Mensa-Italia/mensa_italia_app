import SwiftUI

extension GeometryProxy {
    /// The vertical hinge of an unfolded foldable (iPhone Duo) crossing this
    /// view, in its own space and margins included. Nil on single-screen
    /// devices, while folded, and where the hinge falls outside the view
    /// (e.g. under a split view's sidebar).
    var verticalHinge: CGRect? {
        guard #available(iOS 27.1, *) else { return nil }
        // Fully flat, iOS 27.1 reports the hinge as an *inactive* division
        // region (one continuous panel), so inactive regions count too.
        return reservedRegions(kind: .division, options: .includeInactive)
            .map(\.frame)
            .first { $0.height > $0.width && $0.minX > 0 && $0.maxX < size.width }
    }

    /// The hinge running across the view: iPhone Duo unfolded and held
    /// upright, the inner screen turned a quarter. Nil otherwise.
    var horizontalHinge: CGRect? {
        guard #available(iOS 27.1, *) else { return nil }
        return reservedRegions(kind: .division, options: .includeInactive)
            .map(\.frame)
            .first { $0.width > $0.height && $0.minY > 0 && $0.maxY < size.height }
    }
}
