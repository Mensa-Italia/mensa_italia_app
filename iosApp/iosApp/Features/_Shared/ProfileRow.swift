import SwiftUI

/// Reusable settings row used in Profile screen.
struct ProfileRow: View {
    let icon: String
    let title: String
    var value: String?
    var action: (() -> Void)?
    var disabled: Bool = false

    var body: some View {
        Button(action: { action?() }) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .frame(width: 28, height: 28)
                    .foregroundStyle(disabled ? .secondary : .primary)

                Text(title)
                    .foregroundStyle(disabled ? .secondary : .primary)

                Spacer()

                if let value {
                    Text(value)
                        .foregroundStyle(.secondary)
                        .font(.subheadline)
                }

                if action != nil && !disabled {
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
        }
        .disabled(disabled || action == nil)
    }
}

#Preview {
    List {
        ProfileRow(icon: "creditcard", title: "Membership", action: {})
        ProfileRow(icon: "creditcard.fill", title: "Pagamenti", value: "Coming soon", disabled: true)
        ProfileRow(icon: "info.circle", title: "Versione", value: "1.0.0")
    }
}
