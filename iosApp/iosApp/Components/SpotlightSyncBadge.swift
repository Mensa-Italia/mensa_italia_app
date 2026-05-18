import SwiftUI

/// Indicatore non bloccante della sincronizzazione Spotlight in corso.
struct SpotlightSyncBadge: View {
    var coordinator = SpotlightRefreshCoordinator.shared

    var body: some View {
        Group {
            if coordinator.phase.isActive {
                HStack(spacing: 6) {
                    ProgressView().controlSize(.mini)
                    Text(coordinator.phase.localizedShort)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                        .lineLimit(1)
                }
            }
        }
    }
}

#if DEBUG
/// Pannello diagnostico DEBUG: mostra l'ultimo stato persisto del coordinator
/// + bottone per forzare un rebuild on-demand. Da rimuovere prima dello ship.
struct SpotlightDebugPanel: View {
    var coordinator = SpotlightRefreshCoordinator.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Image(systemName: "ladybug.fill")
                    .foregroundStyle(.orange)
                Text("Spotlight debug")
                    .font(.caption.weight(.semibold))
                Spacer()
                Button {
                    coordinator.manualRefresh(reason: "manual")
                } label: {
                    Label("Refresh", systemImage: "arrow.clockwise")
                        .font(.caption2)
                        .labelStyle(.titleAndIcon)
                }
                .buttonStyle(.bordered)
                .controlSize(.mini)
            }
            Text(coordinator.debugStatus)
                .font(.caption2.monospaced())
                .foregroundStyle(.secondary)
                .lineLimit(3)
                .multilineTextAlignment(.leading)
            if coordinator.phase.isActive {
                HStack(spacing: 6) {
                    ProgressView().controlSize(.mini)
                    Text(coordinator.phase.localizedShort)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(10)
        .background(.orange.opacity(0.08), in: RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .strokeBorder(.orange.opacity(0.3), lineWidth: 1)
        )
    }
}
#endif
