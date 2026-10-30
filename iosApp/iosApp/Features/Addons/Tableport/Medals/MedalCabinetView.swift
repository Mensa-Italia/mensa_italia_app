import SwiftUI

/// The medal collection as a gallery, always dark: how many are earned, then
/// every medal in its glass case (`MedalCase`). Tapping one brings it out in
/// a sheet (`MedalShowcaseView`). System bars, gauge and sheet over the
/// stage. Mirrors Android `MedalCabinetScreen`.
struct MedalCabinetView: View {
    /// What the top bar's filter can show: everything, one metal (earned),
    /// or the ones still to earn. The summary keeps counting them all.
    private enum Filter: Hashable {
        case all, tier(Medal.Tier), locked

        func matches(_ medal: Medal) -> Bool {
            switch self {
            case .all: true
            case .locked: !medal.isEarned
            case .tier(let tier): medal.isEarned && medal.tier == tier
            }
        }
    }

    @State private var store = MedalStore.shared
    @State private var selected: Medal?
    @State private var filter = Filter.all

    private var medals: [Medal] { store.medals }
    private var shown: [Medal] { medals.filter(filter.matches) }

    private var earnedCount: Int { medals.filter(\.isEarned).count }

    private let columns = [GridItem(.adaptive(minimum: 150, maximum: 210), spacing: 16)]

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                if let hinge = geo.verticalHinge {
                    foldedOpen(width: geo.size.width, hinge: hinge)
                } else {
                    VStack(spacing: 24) {
                        summary
                        grid
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                    .padding(.bottom, 32)
                }
            }
        }
        .navigationTitle(tr("medals.title", fallback: "Medagliere"))
        .navigationBarTitleDisplayMode(.large)
        .toolbar { filterToolbarItem }
        .darkStage()
        .sheet(item: $selected) { medal in
            MedalShowcaseView(medal: medal)
        }
        .overlay {
            if medals.isEmpty {
                if store.failed {
                    ContentUnavailableView(
                        tr("medals.title", fallback: "Medagliere"),
                        systemImage: "medal",
                        description: Text(tr("medals.load_error", fallback: "Non riesco a leggere le medaglie. Riprova più tardi."))
                    )
                } else {
                    ProgressView()
                }
            }
        }
        .refreshable { await store.refresh() }
        .task { await store.refresh() }
    }

    /// iPhone Duo unfolded: the summary in the left half and the cases in
    /// the right one, so none is creased by the hinge.
    private func foldedOpen(width: CGFloat, hinge: CGRect) -> some View {
        HStack(alignment: .top, spacing: 0) {
            summary
                .padding(20)
                .frame(width: hinge.minX)
            Color.clear.frame(width: hinge.width)
            grid
                .padding(20)
                .frame(width: width - hinge.maxX)
        }
    }

    /// The cases that pass the filter; a line when none does.
    @ViewBuilder private var grid: some View {
        if shown.isEmpty, !medals.isEmpty {
            Text(tr("medals.filter.empty", fallback: "Nessuna medaglia qui, per ora."))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 32)
        } else {
            LazyVGrid(columns: columns, spacing: 24) {
                ForEach(shown) { caseButton($0) }
            }
        }
    }

    /// The filter, as on the other lists: a menu in the top bar, filled when active.
    @ToolbarContentBuilder private var filterToolbarItem: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker(selection: $filter, label: EmptyView()) {
                    Text(tr("medals.filter.all", fallback: "Tutte")).tag(Filter.all)
                    ForEach(Medal.Tier.allCases, id: \.self) { tier in
                        Text(tier.name).tag(Filter.tier(tier))
                    }
                    Text(tr("medals.filter.locked", fallback: "Da ottenere")).tag(Filter.locked)
                }
            } label: {
                Label(
                    tr("medals.filter.label", fallback: "Filtra per metallo"),
                    systemImage: filter == .all ? "line.3.horizontal.decrease.circle" : "line.3.horizontal.decrease.circle.fill"
                )
            }
        }
    }

    private func caseButton(_ medal: Medal) -> some View {
        Button {
            selected = medal
        } label: {
            MedalCase(medal: medal)
        }
        .buttonStyle(CasePressStyle())
    }

    private var summary: some View {
        let gold = Medal.Tier.gold.metal
        return HStack(spacing: 18) {
            Gauge(value: Double(earnedCount), in: 0...Double(max(medals.count, 1))) {
                Image(systemName: "medal.fill")
            } currentValueLabel: {
                Text("\(earnedCount)")
                    .foregroundStyle(LinearGradient(colors: [gold.hi, gold.mid], startPoint: .top, endPoint: .bottom))
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .tint(gold.mid)
            .scaleEffect(1.25)
            .frame(width: 80, height: 80)

            VStack(alignment: .leading, spacing: 4) {
                Text(tr("medals.count_of", fallback: "{earned} di {total}",
                        ["earned": "\(earnedCount)", "total": "\(medals.count)"]))
                    .font(.title2.weight(.bold).monospacedDigit())
                Text(tr("medals.count_caption_case", fallback: "Medaglie in teca").uppercased())
                    .font(.caption2.weight(.semibold))
                    .tracking(2)
                    .foregroundStyle(.white.opacity(0.5))
                HStack(spacing: 14) {
                    ForEach(Medal.Tier.allCases, id: \.self) { tier in
                        HStack(spacing: 5) {
                            MedalArtwork(tier: tier, symbol: "star.fill", size: 18)
                            Text("\(medals.filter { $0.tier == tier && $0.isEarned }.count)")
                                .font(.footnote.weight(.semibold).monospacedDigit())
                                .foregroundStyle(.white.opacity(0.8))
                        }
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel(Text(tier.name))
                    }
                }
                .padding(.top, 6)
            }
            Spacer(minLength: 0)
        }
    }
}

/// A case gives a little under the finger.
private struct CasePressStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.6), value: configuration.isPressed)
    }
}
