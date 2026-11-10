import Shared
import SwiftUI

/// Members who joined or left the office's region in a period, with the
/// .xlsx export of their mails (the period's list, or every active member).
/// Mirrors Android `LocalGroupMembersScreen`.
struct LocalGroupMembersView: View {
    let snapshot: LocalGroupSnapshot

    private enum Direction: Hashable { case arrived, left }

    /// Picker tags for the shared periods (Kotlin enums aren't handy as tags).
    private enum Period: Hashable, CaseIterable {
        case days7, days30, days90, season

        var shared: LocalGroupPeriod {
            switch self {
            case .days7: .days7
            case .days30: .days30
            case .days90: .days90
            case .season: .season
            }
        }

        var label: String {
            switch self {
            case .days7: tr("local_group.period.days_7", fallback: "7 giorni")
            case .days30: tr("local_group.period.days_30", fallback: "30 giorni")
            case .days90: tr("local_group.period.days_90", fallback: "90 giorni")
            case .season: tr("local_group.period.season", fallback: "Stagione")
            }
        }
    }

    @State private var direction = Direction.arrived
    @State private var period = Period.days30
    @State private var changes: [MemberChangeModel] = []
    @State private var loading = false
    /// The latest load: the Kotlin call ignores Swift cancellation, so an
    /// older load finishing late must not overwrite a newer one.
    @State private var loadToken = 0
    @State private var loadFailed = false
    @State private var exporting = false
    @State private var exportURL: URL?
    @State private var exportFailed = false

    /// A secretary sees only the last days of exits (the server's window):
    /// no choice of period for them. Super admins see every exit.
    private var exitsLocked: Bool { direction == .left && !snapshot.canSeeOldExits }

    /// The period loaded and exported: the chosen one, or the exits' window
    /// while `exitsLocked` (the choice is kept for "Entrati").
    private var effectivePeriod: Period { exitsLocked ? .days7 : period }

    private var range: LocalGroupRange { effectivePeriod.shared.range(today: LocalGroupDates.shared.today()) }

    private var shown: [MemberChangeModel] {
        changes.filter { direction == .arrived ? $0.isArrival : !$0.isArrival }
    }

    var body: some View {
        List {
            Section {
                VStack(spacing: 12) {
                    Picker(tr("local_group.admin.members", fallback: "Soci"), selection: $direction) {
                        Text(tr("local_group.members.arrived", fallback: "Entrati")).tag(Direction.arrived)
                        Text(tr("local_group.members.left", fallback: "Usciti")).tag(Direction.left)
                    }
                    if !exitsLocked {
                        Picker(tr("local_group.period.season", fallback: "Stagione"), selection: $period) {
                            ForEach(Period.allCases, id: \.self) { Text($0.label).tag($0) }
                        }
                    }
                }
                .pickerStyle(.segmented)
                .padding(.vertical, 4)
            } footer: {
                if exitsLocked {
                    Text(tr("local_group.members.exits_window",
                            fallback: "Le uscite sono visibili solo per gli ultimi {days} giorni.",
                            ["days": "\(LocalGroupRules.shared.EXITS_WINDOW_DAYS)"]))
                }
            }

            if let exportURL {
                Section {
                    ShareLink(item: exportURL) {
                        Label {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(tr("local_group.export.share", fallback: "Condividi il file"))
                                Text(exportURL.lastPathComponent)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                            }
                        } icon: {
                            Image(systemName: "square.and.arrow.up")
                        }
                    }
                }
            }

            Section {
                if loading && changes.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if loadFailed {
                    Text(tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale."))
                        .foregroundStyle(.secondary)
                } else if shown.isEmpty {
                    Text(tr("local_group.members.empty", fallback: "Nessun movimento nel periodo"))
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(shown, id: \.id) { change in
                        // A row without a registry id (an old snapshot) has no page to open.
                        if change.member.isEmpty {
                            changeRow(change)
                        } else {
                            NavigationLink(value: RegSociRoute(id: change.member)) { changeRow(change) }
                        }
                    }
                }
            } header: {
                if !loading && !loadFailed && !shown.isEmpty {
                    Text("\(shown.count)")
                }
            }
        }
        .navigationTitle(tr("local_group.admin.members", fallback: "Soci"))
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if exporting {
                    ProgressView()
                } else {
                    Menu {
                        Button {
                            let r = range
                            export(kind: direction == .arrived ? .theNew : .left, from: r.from, to: r.to)
                        } label: {
                            Label(tr("local_group.export.changes", fallback: "Esporta l'elenco"), systemImage: "list.bullet")
                        }
                        Button {
                            export(kind: .active, from: nil, to: nil)
                        } label: {
                            Label(tr("local_group.export.active", fallback: "Esporta tutti gli iscritti"), systemImage: "person.3")
                        }
                    } label: {
                        Label(tr("local_group.export", fallback: "Esporta"), systemImage: "square.and.arrow.up")
                    }
                }
            }
        }
        .alert(tr("local_group.export.failed", fallback: "Esportazione non riuscita"), isPresented: $exportFailed) {
            Button("OK", role: .cancel) {}
        }
        // Reloads when the effective range changes: a new period, or "Usciti" locked to its window.
        .task(id: effectivePeriod) { await load() }
        .refreshable { await load() }
        .onChange(of: direction) { exportURL = nil }
    }

    private func load() async {
        loadToken += 1
        let token = loadToken
        loading = true
        let r = range
        let result: [MemberChangeModel]?
        do {
            result = try await koin.localGroup.loadChanges(officeId: snapshot.office.id, from: r.from, to: r.to)
        } catch {
            result = nil
        }
        guard token == loadToken else { return }
        if let result {
            changes = result
            loadFailed = false
        } else {
            loadFailed = true
        }
        loading = false
        exportURL = nil
    }

    private func changeRow(_ change: MemberChangeModel) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(TextFormatters.shared.titleCase(s: change.name))
            Text("\(LocalGroupFormat.changeLabel(change)) · \(LocalGroupFormat.day(change.dayIso))")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private func export(kind: ExportKind, from: Kotlinx_datetimeLocalDate?, to: Kotlinx_datetimeLocalDate?) {
        exporting = true
        exportURL = nil
        Task {
            defer { exporting = false }
            do {
                let bytes = try await koin.localGroup.exportMembers(officeId: snapshot.office.id, kind: kind, from: from, to: to)
                let data = LocalGroupExportAppleKt.localGroupExportData(bytes: bytes)
                let name = LocalGroupExportFile.shared.fileName(office: snapshot.office, kind: kind, from: from, to: to)
                let url = FileManager.default.temporaryDirectory.appendingPathComponent(name)
                try data.write(to: url, options: .atomic)
                exportURL = url
            } catch {
                exportFailed = true
            }
        }
    }
}
