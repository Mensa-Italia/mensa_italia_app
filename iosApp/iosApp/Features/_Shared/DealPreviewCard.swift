import SwiftUI
import Shared

struct DealPreviewCard: View {
    let deal: DealModel

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "tag.fill")
                        .foregroundStyle(Color.accentColor)
                    Text(deal.name)
                        .font(.headline)
                        .lineLimit(2)
                    Spacer()
                    if deal.isActive {
                        Text(tr("deals.preview.active", fallback: "Attivo"))
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(.green.opacity(0.2), in: Capsule())
                            .foregroundStyle(.green)
                    }
                }

                if !deal.commercialSector.isEmpty {
                    Text(deal.commercialSector)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }

                if let details = deal.details, !details.isEmpty {
                    Text(details)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }

                if let pos = deal.position {
                    let locationLabel = pos.address.isEmpty ? pos.name : pos.address
                    Label(locationLabel, systemImage: "mappin.and.ellipse")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                        .lineLimit(1)
                }
            }
            .padding(14)
        }
    }
}
