import SwiftUI
import Shared
import UIKit

/// Calendar link manager. Mirrors the Flutter `CalendarLinkerView` behavior:
/// - Subscribes to a per-user iCal feed at `webcal://svc.mensa.it/ical/{hash}`.
/// - "National events" are always included on the backend (not editable here).
/// - The user toggles per-region inclusion; the list lives in `link.state`.
struct CalendarLinkerView: View {
    @State private var vm = CalendarLinkerViewModel()
    @State private var appeared = false
    @State private var copied = false

    /// Italian regions, alphabetically sorted — mirrors `ListOfStates`
    /// in `mensa_italia_app/lib/model/location.dart`.
    private let availableStates: [String] = [
        "Abruzzo",
        "Basilicata",
        "Calabria",
        "Campania",
        "Emilia-Romagna",
        "Friuli-Venezia Giulia",
        "Lazio",
        "Liguria",
        "Lombardia",
        "Marche",
        "Molise",
        "Piemonte",
        "Puglia",
        "Sardegna",
        "Sicilia",
        "Toscana",
        "Trentino-Alto Adige",
        "Umbria",
        "Valle d'Aosta",
        "Veneto"
    ]

    var body: some View {
        ScrollView {
            VStack(spacing: 22) {
                hero
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)
                    .animation(.spring(response: 0.6, dampingFraction: 0.8), value: appeared)

                if let link = vm.link {
                    addToCalendarButton(link)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 12)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(0.08),
                            value: appeared
                        )
                    currentLinkCard(link)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 12)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(0.12),
                            value: appeared
                        )
                    regionsSection(link: link)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 12)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(0.18),
                            value: appeared
                        )
                    Text(tr("app.calendar_link.sync_notice",
                            fallback: "Gli aggiornamenti verranno applicati al tuo calendario ma potrebbero richiedere del tempo. Controlla l'orario di aggiornamento nell'app Calendario."))
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 8)
                } else if !vm.loading {
                    emptyCard
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 12)
                        .animation(
                            .spring(response: 0.6, dampingFraction: 0.8).delay(0.08),
                            value: appeared
                        )
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 24)
        }
        .navigationTitle(tr("app.calendar_link.title", fallback: "Calendario"))
        .navigationBarTitleDisplayMode(.large)
        .overlay {
            if vm.loading && vm.link == nil {
                ProgressView().controlSize(.large)
            }
        }
        .refreshable { await vm.refresh() }
        .task {
            await vm.load()
            withAnimation { appeared = true }
        }
    }

    private var hero: some View {
        VStack(spacing: 10) {
            Image(systemName: "calendar.badge.plus")
                .font(.system(size: 44))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
            Text(tr("app.calendar_link.headline",
                    fallback: "Sincronizza con il tuo calendario"))
                .font(.title3.weight(.semibold))
                .multilineTextAlignment(.center)
            Text(tr("app.calendar_link.subhead",
                    fallback: "Aggiungi automaticamente eventi Mensa al tuo calendario preferito tramite un link iCal."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private func addToCalendarButton(_ link: CalendarLinkModel) -> some View {
        Button {
            if let url = URL(string: webcalURL(for: link)) {
                UIApplication.shared.open(url)
            }
        } label: {
            HStack {
                Image(systemName: "calendar.badge.plus")
                Text(tr("app.calendar_link.add_to_calendar",
                        fallback: "Aggiungi al calendario"))
                    .fontWeight(.semibold)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(AppTheme.brandGradient,
                        in: RoundedRectangle(cornerRadius: AppTheme.Radius.button))
            .foregroundStyle(.white)
        }
    }

    private func currentLinkCard(_ link: CalendarLinkModel) -> some View {
        GlassCard(tint: AppTheme.Colors.brandSecondary.opacity(0.18)) {
            VStack(alignment: .leading, spacing: 10) {
                Text(tr("app.calendar_link.your_link", fallback: "Il tuo link iCal"))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
                Text(httpsURL(for: link))
                    .font(.system(.footnote, design: .monospaced))
                    .lineLimit(2)
                    .truncationMode(.middle)
                HStack(spacing: 10) {
                    Button {
                        UIPasteboard.general.string = httpsURL(for: link)
                        withAnimation { copied = true }
                        Task {
                            try? await Task.sleep(nanoseconds: 1_500_000_000)
                            withAnimation { copied = false }
                        }
                    } label: {
                        Label(copied
                              ? tr("app.calendar_link.copied", fallback: "Copiato!")
                              : tr("app.calendar_link.copy", fallback: "Copia"),
                              systemImage: copied ? "checkmark" : "doc.on.doc")
                            .font(.subheadline.weight(.medium))
                    }
                    .buttonStyle(.bordered)
                    .tint(AppTheme.Colors.brandTintAdaptive)

                    if let url = URL(string: httpsURL(for: link)) {
                        ShareLink(item: url) {
                            Label(tr("app.calendar_link.share", fallback: "Condividi"),
                                  systemImage: "square.and.arrow.up")
                                .font(.subheadline.weight(.medium))
                        }
                        .buttonStyle(.bordered)
                        .tint(AppTheme.Colors.brandTintAdaptive)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func regionsSection(link: CalendarLinkModel) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(tr("app.calendar_link.regions_section", fallback: "Regioni"))
                .font(.headline)
                .padding(.bottom, 10)

            // Non-editable: National events are always included server-side.
            Toggle(isOn: .constant(true)) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(tr("app.calendar_link.national_events",
                            fallback: "Eventi nazionali"))
                    Text(tr("app.calendar_link.not_editable",
                            fallback: "Non modificabile"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .disabled(true)
            .tint(AppTheme.Colors.brandTintAdaptive)
            .padding(.vertical, 6)

            Divider()

            ForEach(availableStates, id: \.self) { region in
                Toggle(isOn: Binding(
                    get: { link.state.contains(region) },
                    set: { _ in
                        Task { await vm.toggleRegion(region) }
                    }
                )) {
                    Text(region)
                }
                .tint(AppTheme.Colors.brandTintAdaptive)
                .padding(.vertical, 4)
                if region != availableStates.last {
                    Divider()
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: 16))
    }

    private var emptyCard: some View {
        VStack(spacing: 10) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.system(size: 36))
                .foregroundStyle(.secondary)
            Text(tr("app.calendar_link.empty",
                    fallback: "Nessun calendario configurato"))
                .font(.headline)
            Text(tr("app.calendar_link.empty_message",
                    fallback: "Il link al calendario verrà generato automaticamente."))
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(Color(.secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: 16))
    }

    private func webcalURL(for link: CalendarLinkModel) -> String {
        "webcal://svc.mensa.it/ical/\(link.hash)"
    }

    private func httpsURL(for link: CalendarLinkModel) -> String {
        "https://svc.mensa.it/ical/\(link.hash)"
    }
}

@MainActor
@Observable
final class CalendarLinkerViewModel {
    var link: CalendarLinkModel?
    var loading = true
    var errorMessage: String?

    private var sub: Closeable?

    func load() async {
        sub?.close()
        let flow = koin.calendarLinks.observeCurrent() as Kotlinx_coroutines_coreFlow
        sub = subscribeOptionalFlow(flow) { [weak self] (model: CalendarLinkModel?) in
            Task { @MainActor [weak self] in
                self?.link = model
                self?.loading = false
            }
        }
        await refresh()
    }

    func refresh() async {
        do {
            try await koin.calendarLinks.refresh()
        } catch {
            errorMessage = error.localizedDescription
        }
        loading = false
    }

    func toggleRegion(_ region: String) async {
        guard let current = link else { return }
        var newState = current.state
        if let idx = newState.firstIndex(where: { $0.lowercased() == region.lowercased() }) {
            newState.remove(at: idx)
        } else {
            newState.append(region)
        }
        // Optimistic update — the DB flow will reconcile.
        link = CalendarLinkModel(
            id: current.id,
            user: current.user,
            hash: current.hash,
            state: newState
        )
        do {
            _ = try await koin.calendarLinks.changeState(id: current.id, state: newState)
        } catch {
            errorMessage = error.localizedDescription
            // Revert by re-fetching.
            await refresh()
        }
    }
}

#Preview {
    NavigationStack { CalendarLinkerView() }
}
