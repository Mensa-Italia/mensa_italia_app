import Shared
import SwiftUI

/// The members who joined the office's region in the last week (the
/// server's `members_new_7d`), open to every member of the office. Each row
/// opens the member's card through the stack's `RegSociRoute` destination.
struct LocalGroupNewMembersView: View {
    let officeId: String

    @State private var members: [RegSociModel] = []
    @State private var loading = false
    /// The latest load: the Kotlin call ignores Swift cancellation, so an
    /// older load finishing late must not overwrite a newer one.
    @State private var loadToken = 0
    @State private var loadFailed = false

    var body: some View {
        List {
            Section {
                if loading && members.isEmpty {
                    HStack { Spacer(); ProgressView(); Spacer() }
                } else if loadFailed {
                    Text(tr("local_group.state.failed", fallback: "Non siamo riusciti a caricare il gruppo locale."))
                        .foregroundStyle(.secondary)
                } else if members.isEmpty {
                    Text(tr("local_group.new_members.empty", fallback: "Nessun nuovo socio nell'ultima settimana"))
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(members, id: \.id) { member in
                        // By view, like the screen that opened this one: a value push from
                        // a view-pushed screen makes the stack pop it (see LocalGroupMembersView).
                        NavigationLink { MemberDetailView(memberId: member.id) } label: {
                            MemberCellCompact(member: member)
                        }
                    }
                }
            } header: {
                if !loading && !loadFailed && !members.isEmpty {
                    Text("\(members.count)")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(tr("local_group.new_members.title", fallback: "Nuovi soci"))
        .task { await load() }
        .refreshable { await load() }
    }

    private func load() async {
        loadToken += 1
        let token = loadToken
        loading = true
        let result: [RegSociModel]?
        do {
            result = try await koin.localGroup.loadNewMembers(officeId: officeId)
        } catch {
            if error is CancellationError { return }
            result = nil
        }
        guard token == loadToken else { return }
        if let result {
            members = result
            loadFailed = false
        } else {
            loadFailed = true
        }
        loading = false
    }
}
