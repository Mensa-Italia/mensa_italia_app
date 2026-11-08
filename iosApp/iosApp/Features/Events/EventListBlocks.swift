import SwiftUI

/// A section title of the events list ("Imminenti", "Passati") with its
/// count in a capsule. Shared by `EventListView` and the local office's
/// events in the "Gruppo locale" admin area.
struct EventListSectionHeader: View {
    let title: String
    let count: Int

    var body: some View {
        HStack {
            Text(title).font(.title3.bold())
            Text("\(count)")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
                .padding(.horizontal, 8).padding(.vertical, 2)
                .background(.thinMaterial, in: Capsule())
            Spacer()
        }
        .padding(.horizontal)
        .padding(.top, 6)
    }
}
