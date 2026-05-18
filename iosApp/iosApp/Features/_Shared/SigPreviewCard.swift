import SwiftUI
import Shared

struct SigPreviewCard: View {
    let sig: SigModel

    var body: some View {
        GlassCard {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "star.fill")
                        .foregroundStyle(Color.accentColor)
                    Text(sig.name)
                        .font(.headline)
                        .lineLimit(2)
                    Spacer()
                    if !sig.groupType.isEmpty {
                        Text(sig.groupType)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }

                if !sig.description_.isEmpty {
                    Text(sig.description_)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }
            }
            .padding(14)
        }
    }
}
