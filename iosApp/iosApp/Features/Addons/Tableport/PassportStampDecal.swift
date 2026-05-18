import SwiftUI

/// Single stamp decal — postal-style perforated edge, ivory backing, optional cancel mark.
/// Wraps the (often black-on-transparent) PNG with an ivory tile so it's visible in dark mode.
struct PassportStampDecal: View {
    let url: URL?
    let size: CGFloat
    let rotation: Double
    var showsCancel: Bool = false

    var body: some View {
        ZStack {
            // Perforated (toothed) backing — drawn via Canvas as a circle of arcs cut out.
            PerforatedTile(toothCount: 22, toothRadius: 3)
                .fill(PassportPalette.parchment)
                .shadow(color: .black.opacity(0.25), radius: 3, x: 1, y: 2)

            // Inner ivory plate
            PerforatedTile(toothCount: 22, toothRadius: 3)
                .fill(PassportPalette.parchment)
                .padding(2)
                .overlay(
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .stroke(PassportPalette.gold.opacity(0.5), lineWidth: 0.6)
                        .padding(8)
                )

            // The stamp PNG (black on transparent — looks great on ivory).
            Group {
                if let url {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().aspectRatio(contentMode: .fit)
                    } placeholder: {
                        Image(systemName: "seal")
                            .font(.system(size: size * 0.35, weight: .regular))
                            .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.5))
                    }
                } else {
                    Image(systemName: "seal")
                        .font(.system(size: size * 0.35, weight: .regular))
                        .foregroundStyle(AppTheme.Colors.mensaBlue.opacity(0.5))
                }
            }
            .padding(12)

            // "Cancel" wave — postal ink lines crossing the stamp.
            if showsCancel {
                CancelMark()
                    .stroke(PassportPalette.stampRed.opacity(0.55), lineWidth: 1.2)
                    .padding(6)
                    .allowsHitTesting(false)
            }
        }
        .frame(width: size, height: size)
        .rotationEffect(.degrees(rotation))
    }
}

// MARK: - Perforated edge shape (postal stamp tooth border)

private struct PerforatedTile: Shape {
    let toothCount: Int
    let toothRadius: CGFloat

    func path(in rect: CGRect) -> Path {
        // Start with a rounded rect, then subtract semicircles along each edge.
        var path = Path(roundedRect: rect, cornerRadius: 4, style: .continuous)
        let teethPerSide = toothCount
        let stepX = rect.width / CGFloat(teethPerSide)
        let stepY = rect.height / CGFloat(teethPerSide)
        var cuts = Path()
        for i in 0..<teethPerSide {
            // Top
            let cx = rect.minX + stepX * (CGFloat(i) + 0.5)
            cuts.addEllipse(in: CGRect(
                x: cx - toothRadius,
                y: rect.minY - toothRadius,
                width: toothRadius * 2,
                height: toothRadius * 2
            ))
            // Bottom
            cuts.addEllipse(in: CGRect(
                x: cx - toothRadius,
                y: rect.maxY - toothRadius,
                width: toothRadius * 2,
                height: toothRadius * 2
            ))
            let cy = rect.minY + stepY * (CGFloat(i) + 0.5)
            // Left
            cuts.addEllipse(in: CGRect(
                x: rect.minX - toothRadius,
                y: cy - toothRadius,
                width: toothRadius * 2,
                height: toothRadius * 2
            ))
            // Right
            cuts.addEllipse(in: CGRect(
                x: rect.maxX - toothRadius,
                y: cy - toothRadius,
                width: toothRadius * 2,
                height: toothRadius * 2
            ))
        }
        // Boolean subtract via even-odd fill
        path.addPath(cuts)
        return path
    }
}

// MARK: - Cancel mark

private struct CancelMark: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        let lines = 5
        let spacing = rect.height / CGFloat(lines + 1)
        for i in 1...lines {
            let y = CGFloat(i) * spacing
            p.move(to: CGPoint(x: rect.minX, y: y - 4))
            p.addQuadCurve(
                to: CGPoint(x: rect.maxX, y: y + 4),
                control: CGPoint(x: rect.midX, y: y - 10)
            )
        }
        return p
    }
}
