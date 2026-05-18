import SwiftUI
import Shared

struct TestAssistantView: View {
    @State private var openURL = false

    private let platformURL = URL(string: "https://www.cloud32.it/Associazioni/utenti/testelab")!

    /// Powers session-stable: lettura sincrona dall'auth — cambia solo a
    /// login/logout, in entrambi i casi la view viene smontata da RootView.
    private var currentUser: UserModel? {
        koin.auth.currentUser.value as? UserModel
    }

    var body: some View {
        Group {
            if hasPower("testmakers", user: currentUser) {
                authorizedContent
            } else {
                unauthorizedState
            }
        }
        .navigationTitle(tr("addons.test_assistant.title", fallback: "Test Assistant"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var unauthorizedState: some View {
        VStack(spacing: 18) {
            Image(systemName: "lock.shield")
                .font(.system(size: 56, weight: .light))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .padding(.bottom, 4)
            Text(tr("addons.test_assistant.locked_title", fallback: "Riservato ai testmakers"))
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(tr("addons.test_assistant.locked_description",
                    fallback: "Questa area è disponibile solo per i soci con permessi testmakers."))
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var authorizedContent: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(spacing: 12) {
                        Image(systemName: "flask.fill")
                            .font(.title)
                            .foregroundStyle(AppTheme.Colors.mensaCyan)
                        Text(tr("addons.test_assistant.card_title", fallback: "Sistema testelab Mensa"))
                            .font(.title3.weight(.bold))
                    }
                    Text(tr("addons.test_assistant.card_description",
                            fallback: "Gestisci e somministra i test ufficiali Mensa Italia tramite la piattaforma cloud32 testelab."))
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(20)
                .background(
                    RoundedRectangle(cornerRadius: 22, style: .continuous)
                        .fill(.ultraThinMaterial)
                )

                Button {
                    openURL = true
                } label: {
                    Label(
                        tr("addons.test_assistant.open_platform", fallback: "Apri piattaforma"),
                        systemImage: "arrow.up.right.square"
                    )
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 4)
                }
                .buttonStyle(.glassProminent)
                .tint(AppTheme.Colors.mensaBlue)
            }
            .padding(20)
        }
        .environment(\.openURL, OpenURLAction { url in
            UIApplication.shared.open(url)
            return .handled
        })
        .onChange(of: openURL) { _, newValue in
            if newValue {
                UIApplication.shared.open(platformURL)
                openURL = false
            }
        }
    }
}

#Preview {
    NavigationStack { TestAssistantView() }
}
