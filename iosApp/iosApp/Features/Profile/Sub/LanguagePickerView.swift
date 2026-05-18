import SwiftUI

/// Per-app language picker. Apple Settings convention (cf. Settings → General →
/// Language & Region → Preferred Language): a `List` of available languages
/// with a checkmark on the active one, plus a "Sistema" entry that clears the
/// override and falls back to the device locale.
///
/// Picking a row immediately re-bootstraps the i18n catalog and the active
/// localisation re-renders thanks to the `.id(localeManager.version)` higher
/// up the view tree.
struct LanguagePickerView: View {
    @ObservedObject private var locale = LocaleManager.shared
    @State private var switching: String? = nil

    var body: some View {
        List {
            Section {
                row(
                    title: tr("app.language.system", fallback: "Sistema"),
                    subtitle: tr(
                        "app.language.system_subtitle",
                        fallback: "Segui la lingua del dispositivo"
                    ),
                    isSelected: locale.override == nil,
                    isLoading: switching == "" && locale.override != nil,
                    action: { Task { await pick(nil) } }
                )
            }

            Section {
                ForEach(locale.availableLocales, id: \.self) { tag in
                    row(
                        title: locale.displayName(for: tag).capitalized,
                        subtitle: tag != locale.activeTag
                            ? locale.nativeName(for: tag).capitalized
                            : nil,
                        isSelected: locale.override == tag,
                        isLoading: switching == tag,
                        action: { Task { await pick(tag) } }
                    )
                }
            } header: {
                Text(tr("app.language.available", fallback: "Disponibili"))
            } footer: {
                Text(tr(
                    "app.language.footer",
                    fallback: "L'app userà questa lingua indipendentemente dalle impostazioni del dispositivo. Le traduzioni mancanti vengono caricate dal server."
                ))
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("app.profile.language", fallback: "Lingua"))
        .navigationBarTitleDisplayMode(.inline)
    }

    @MainActor
    private func pick(_ tag: String?) async {
        // Skip if already on this choice.
        if (tag == nil && locale.override == nil) || (tag != nil && tag == locale.override) {
            return
        }
        switching = tag ?? ""
        await locale.setOverride(tag)
        switching = nil
    }

    @ViewBuilder
    private func row(
        title: String,
        subtitle: String?,
        isSelected: Bool,
        isLoading: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .foregroundStyle(.primary)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 8)
                if isLoading {
                    ProgressView()
                } else if isSelected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(switching != nil)
    }
}

#Preview {
    NavigationStack { LanguagePickerView() }
}
