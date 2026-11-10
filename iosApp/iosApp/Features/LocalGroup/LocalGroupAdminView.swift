import Shared
import SwiftUI

/// The admin area of the local office (secretary, co-secretaries, super):
/// members' movements, the office's figures and its events. Mirrors
/// Android `LocalGroupAdminScreen`.
struct LocalGroupAdminView: View {
    let snapshot: LocalGroupSnapshot

    var body: some View {
        List {
            Section {
                NavigationLink(value: LocalGroupRoute.adminMembers) {
                    Label(tr("local_group.admin.members", fallback: "Soci"), systemImage: "person.2")
                }
                NavigationLink(value: LocalGroupRoute.adminStats) {
                    Label(tr("local_group.admin.stats", fallback: "Statistiche"), systemImage: "chart.bar")
                }
                NavigationLink(value: LocalGroupRoute.adminEvents) {
                    Label(tr("local_group.admin.events", fallback: "Eventi della sede"), systemImage: "calendar")
                }
            } header: {
                Text(snapshot.office.name)
            } footer: {
                Text(tr("local_group.admin.subtitle", fallback: "Soci, statistiche ed eventi della sede"))
            }
        }
        .navigationTitle(tr("local_group.admin.title", fallback: "Area segreteria"))
    }
}
