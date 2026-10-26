import SwiftUI
import Shared

/// I soci che compiono gli anni oggi, come nella Home della vecchia app:
/// gli avatar sovrapposti e, toccandoli, l'elenco con gli anni e il profilo
/// di ciascuno. Chi festeggia lo decide `Birthdays` (:shared). Mirrors
/// Android `BirthdaysCard`.
struct BirthdaysCard: View {
    let birthdays: [Birthday]
    @State private var showList = false

    private static let maxFaces = 5

    var body: some View {
        Button {
            showList = true
        } label: {
            HStack(spacing: 14) {
                faces
                VStack(alignment: .leading, spacing: 2) {
                    Text(headline)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        // Open on the Duo the card has half the width: wrap,
                        // never truncate.
                        .fixedSize(horizontal: false, vertical: true)
                    Text(tr("today.birthdays.cta", fallback: "Fagli gli auguri"))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .compatGlass(cornerRadius: 18)
        .sheet(isPresented: $showList) {
            BirthdaysListSheet(birthdays: birthdays)
        }
    }

    /// Fino a cinque volti, ognuno sopra il precedente per metà; oltre,
    /// un "+N".
    private var faces: some View {
        let shown = Array(birthdays.prefix(Self.maxFaces))
        let extra = birthdays.count - shown.count
        return HStack(spacing: -14) {
            ForEach(Array(shown.enumerated()), id: \.offset) { _, b in
                MemberAvatar(member: b.member, size: 36)
                    .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2))
            }
            if extra > 0 {
                Text("+\(extra)")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(.secondary)
                    .frame(width: 36, height: 36)
                    .background(Circle().fill(Color(.secondarySystemFill)))
                    .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2))
            }
        }
        .accessibilityHidden(true)
    }

    private var headline: String {
        if birthdays.count == 1, let only = birthdays.first {
            return tr("today.birthdays.one", fallback: "{name} compie gli anni oggi",
                      ["name": memberDisplayName(only.member.name)])
        }
        return tr("today.birthdays.many", fallback: "{count} soci compiono gli anni oggi",
                  ["count": "\(birthdays.count)"])
    }
}

/// L'elenco dei festeggiati, con gli anni; ognuno porta al suo profilo.
private struct BirthdaysListSheet: View {
    let birthdays: [Birthday]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List(birthdays, id: \.member.id) { b in
                NavigationLink {
                    MemberDetailView(memberId: b.member.id)
                } label: {
                    HStack(spacing: 12) {
                        MemberAvatar(member: b.member, size: 40)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(memberDisplayName(b.member.name))
                                .font(.body)
                                .lineLimit(1)
                            if let age = b.age?.intValue {
                                Text(tr("today.birthdays.age", fallback: "Compie {age} anni", ["age": "\(age)"]))
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            } else if !b.member.state.isEmpty {
                                Text(b.member.state)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
            .navigationTitle(tr("today.birthdays.title", fallback: "Compleanni di oggi"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(tr("app.close", fallback: "Chiudi")) { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

/// Il registro scrive i nomi in maiuscolo: "MARIO ROSSI" → "Mario Rossi".
func memberDisplayName(_ raw: String) -> String {
    raw.split(separator: " ")
        .map { word in word.prefix(1).uppercased() + word.dropFirst().lowercased() }
        .joined(separator: " ")
}
