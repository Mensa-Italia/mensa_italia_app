import SwiftUI

/// Branded glass-prominent button supporting label + optional system icon.
struct PrimaryButton: View {
    let title: String
    var icon: String?
    var loading: Bool
    let action: () async -> Void

    init(
        _ title: String,
        icon: String? = nil,
        loading: Bool = false,
        action: @escaping () async -> Void
    ) {
        self.title = title
        self.icon = icon
        self.loading = loading
        self.action = action
    }

    var body: some View {
        Button {
            Task { await action() }
        } label: {
            HStack(spacing: 8) {
                if loading {
                    ProgressView().tint(.white)
                } else {
                    if let icon {
                        Image(systemName: icon)
                            .font(.headline)
                    }
                    Text(title)
                        .font(.headline)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
        }
        .buttonStyle(.glassProminent)
        .disabled(loading)
    }
}
