import SwiftUI

/// Thin wrapper around the existing `MembersDirectoryView` so the addon list
/// can route to the directory under the "Contacts" addon brand.
struct ContactsAddonView: View {
    var body: some View {
        MembersDirectoryView()
            .navigationTitle(tr("addons.contacts.title", fallback: "Rubrica Soci"))
    }
}

#Preview {
    NavigationStack { ContactsAddonView() }
}
