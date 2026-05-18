import SwiftUI
import Shared

struct EventPreviewCard: View {
    let event: EventModel

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "it_IT")
        f.dateStyle = .medium
        f.timeStyle = .short
        return f
    }()

    private var startDate: Date {
        Date(timeIntervalSince1970: Double(event.whenStart.epochSeconds))
    }

    private var isPast: Bool { EventDateUtil.isPast(event) }

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "calendar")
                        .foregroundStyle(Color.accentColor)
                    Text(event.name)
                        .font(.headline)
                        .lineLimit(2)
                    Spacer()
                    if isPast {
                        // Chip "Concluso" — quando entrambe (Nazionale +
                        // Concluso) sarebbero presenti, "Concluso" vince:
                        // è l'informazione più rilevante per chi scorre.
                        Label(tr("events.tag.past", fallback: "Concluso"),
                              systemImage: "checkmark.seal.fill")
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(.secondary)
                    } else if event.isNational {
                        Label(tr("events.preview.national", fallback: "Nazionale"), systemImage: "globe")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }

                Text(Self.dateFormatter.string(from: startDate))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                if !event.description_.isEmpty {
                    Text(event.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }

                if let pos = event.position {
                    let locationLabel = pos.address.isEmpty ? pos.name : pos.address
                    Label(locationLabel, systemImage: "mappin.and.ellipse")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .lineLimit(1)
                }
            }
            .padding(14)
        }
        // Stesso trattamento "passato" della card hero per coerenza
        // visiva fra varianti compact e full-width.
        .saturation(isPast ? 0.35 : 1.0)
        .opacity(isPast ? 0.68 : 1.0)
    }
}
