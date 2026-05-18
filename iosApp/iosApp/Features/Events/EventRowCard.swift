import SwiftUI
import Shared

/// Vertical event tile: full-width hero image (aspect-ratio preserving)
/// with overlaid tags and meta block below.
struct EventRowCard: View {
    let event: EventModel

    private var imageURL: URL? {
        guard !event.image.isEmpty else { return nil }
        // PocketBase image filename → resolve via Files helper if it's not an absolute URL.
        if event.image.hasPrefix("http") { return URL(string: event.image) }
        return Files.url(
            collection: "events",
            recordId: event.id,
            filename: event.image,
            thumb: "800x0"
        )
    }

    private var startDate: Date { EventDateUtil.date(event.whenStart) }

    /// Evento concluso → trattamento "passato": card desaturata + opacità
    /// ridotta + chip "Concluso" in primo piano. Cerchiamo un colpo d'occhio
    /// netto senza nascondere il contenuto (l'utente può ancora aprirlo).
    private var isPast: Bool { EventDateUtil.isPast(event) }

    private var gradientPlaceholder: some View {
        LinearGradient(
            colors: [
                AppTheme.Colors.brandPrimary.opacity(0.55),
                AppTheme.Colors.brandSecondary.opacity(0.55)
            ],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if imageURL != nil {
                heroImage
            }
            metaBlock
        }
        .background(.thinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .glassEffect(.regular, in: .rect(cornerRadius: 20))
        // Effetto "passato": colori spenti + opacità ridotta. Applicato
        // all'INTERA card, così foto, tag e testo scolano in coro. Le tile
        // restano leggibili e tappabili — l'utente capisce che è passato.
        .saturation(isPast ? 0.35 : 1.0)
        .opacity(isPast ? 0.68 : 1.0)
        .accessibilityLabel(Text(isPast
            ? "\(event.name). \(tr("events.tag.past", fallback: "Evento concluso"))"
            : event.name))
    }

    // MARK: - Hero image with overlaid tag chips

    @ViewBuilder
    private var heroImage: some View {
        ZStack(alignment: .topLeading) {
            Group {
                if let url = imageURL {
                    CachedAsyncImage(url: url) { img in
                        img.resizable().aspectRatio(contentMode: .fit)
                    } placeholder: {
                        gradientPlaceholder
                            .frame(height: 160)
                    }
                } else {
                    ZStack {
                        gradientPlaceholder
                        Image(systemName: "calendar")
                            .font(.system(size: 40, weight: .semibold))
                            .foregroundStyle(.white.opacity(0.9))
                    }
                    .frame(height: 160)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: 220)
            .clipped()

            // Subtle dark scrim on top so chips stay readable on any image.
            LinearGradient(
                colors: [Color.black.opacity(0.45), Color.black.opacity(0.0)],
                startPoint: .top, endPoint: .bottom
            )
            .frame(height: 70)
            .allowsHitTesting(false)

            tagChips
                .padding(.horizontal, 12)
                .padding(.top, 12)
        }
    }

    @ViewBuilder
    private var tagChips: some View {
        HStack(spacing: 6) {
            // "Concluso" prevale visivamente: quando l'evento è passato il
            // resto dei tag (Nazionale/Spot) è informativamente meno
            // rilevante. Lo mostriamo davanti agli altri tag, in stile
            // capsule grigia coerente con le linee guida.
            if isPast {
                chip(
                    text: tr("events.tag.past", fallback: "Concluso"),
                    icon: "checkmark.seal.fill",
                    tint: .gray
                )
            }
            if event.isNational {
                chip(
                    text: tr("events.tag.national", fallback: "Nazionale"),
                    icon: "globe",
                    tint: AppTheme.Colors.brandPrimary
                )
            } else {
                chip(
                    text: tr("events.tag.local", fallback: "Locale"),
                    icon: "mappin",
                    tint: AppTheme.Colors.brandSecondary
                )
            }
            if event.isSpot {
                chip(
                    text: tr("events.tag.spot", fallback: "Spot"),
                    icon: "sparkles",
                    tint: .orange
                )
            }
        }
    }

    private func chip(text: String, icon: String, tint: Color) -> some View {
        Label(text, systemImage: icon)
            .font(.caption2.weight(.semibold))
            .labelStyle(.titleAndIcon)
            .padding(.horizontal, 9).padding(.vertical, 4)
            .foregroundStyle(.white)
            // Solid pill scuro (~78% nero) invece di un materiale traslucido.
            // I material adattivi (`ultraThinMaterial`) anche forzati a dark
            // restano troppo translucidi → su immagini dai medi-toni il
            // testo bianco si perde. Solid = contrasto AA garantito su
            // qualunque background. Pattern Apple Photos / Maps overlay.
            .background(Color.black.opacity(0.78), in: Capsule())
            .overlay(
                Capsule().strokeBorder(tint, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.35), radius: 3, y: 1)
    }

    // MARK: - Meta

    @ViewBuilder
    private var metaBlock: some View {
        VStack(alignment: .leading, spacing: 6) {
            // Quando manca l'hero, i tag chip non hanno una superficie scura
            // su cui galleggiare → li mettiamo inline in cima al meta block.
            if imageURL == nil {
                inlineTagChips
                    .padding(.bottom, 2)
            }

            Text(event.name)
                .font(.headline.bold())
                .lineLimit(2)
                .foregroundStyle(.primary)

            Label(EventDateUtil.mediumFormatter.string(from: startDate),
                  systemImage: "calendar")
                .font(.caption)
                .foregroundStyle(.secondary)

            if let pos = event.position {
                Label(pos.address.isEmpty ? pos.name : pos.address,
                      systemImage: "mappin.and.ellipse")
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
    }

    /// Variante dei tag chip per la modalità "senza hero": versione
    /// minimale tonal (no fondo scuro, no shadow) integrata nel meta block.
    @ViewBuilder
    private var inlineTagChips: some View {
        HStack(spacing: 6) {
            if isPast {
                inlineChip(text: tr("events.tag.past", fallback: "Concluso"),
                           icon: "checkmark.seal.fill",
                           tint: .gray)
            }
            if event.isNational {
                inlineChip(text: tr("events.tag.national", fallback: "Nazionale"),
                           icon: "globe",
                           tint: AppTheme.Colors.brandPrimary)
            } else {
                inlineChip(text: tr("events.tag.local", fallback: "Locale"),
                           icon: "mappin",
                           tint: AppTheme.Colors.brandSecondary)
            }
            if event.isSpot {
                inlineChip(text: tr("events.tag.spot", fallback: "Spot"),
                           icon: "sparkles",
                           tint: .orange)
            }
        }
    }

    private func inlineChip(text: String, icon: String, tint: Color) -> some View {
        Label(text, systemImage: icon)
            .font(.caption2.weight(.semibold))
            .labelStyle(.titleAndIcon)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .foregroundStyle(tint)
            .background(tint.opacity(0.15), in: Capsule())
    }
}
