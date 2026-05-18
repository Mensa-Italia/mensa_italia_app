import SwiftUI
import Shared

/// Single-column list of compact deal tiles with sector chips, search and
/// pull-to-refresh.
/// Cache-first: never blocks on a spinner if we already have cached data.
struct DealListView: View {
    @State private var vm = DealListViewModel()
    @State private var appeared = false
    @State private var showAdd = false

    var body: some View {
        content
            .overlay {
                if vm.allDeals.isEmpty && vm.refreshing {
                    LoadingDots()
                } else if vm.allDeals.isEmpty {
                    ContentUnavailableView(
                        tr("app.deals.empty", fallback: "Nessun deal"),
                        systemImage: "tag.slash",
                        description: Text(tr(
                            "app.deals.empty_description",
                            fallback: "Non ci sono deal disponibili al momento."
                        ))
                    )
                } else if vm.filteredDeals.isEmpty && vm.isFilterActive {
                    ContentUnavailableView(
                        tr("app.deals.no_results", fallback: "Nessun deal trovato"),
                        systemImage: "magnifyingglass",
                        description: Text(tr(
                            "app.deals.no_results_description",
                            fallback: "Prova a cambiare filtro o ricerca."
                        ))
                    )
                }
            }
            .navigationTitle(tr("addons.deals.title", fallback: "Deal & convenzioni"))
            .cleanNavBar()
            .searchable(
                text: $vm.searchText,
                placement: .navigationBarDrawer(displayMode: .automatic),
                prompt: Text(tr("app.deals.search_placeholder", fallback: "Cerca convenzioni"))
            )
            .toolbar {
                sectorFilterToolbarItem
                if vm.canAddDeal {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            showAdd = true
                        } label: {
                            Image(systemName: "plus.circle.fill")
                        }
                        .accessibilityLabel(tr("addons.deals.add", fallback: "Aggiungi deal"))
                    }
                }
            }
        .sheet(isPresented: $showAdd) {
            NavigationStack {
                AddDealView()
            }
        }
        .navigationDestination(for: String.self) { dealId in
            DealDetailView(dealId: dealId)
        }
        .task {
            vm.start()
            // Drive stagger entrance.
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
        }
        .onDisappear { vm.stop() }
        .alert(
            tr("app.error.title", fallback: "Errore"), // i18n
            isPresented: Binding(
                get: { vm.error != nil },
                set: { if !$0 { vm.error = nil } }
            )
        ) {
            Button("OK") { vm.error = nil }
        } message: {
            Text(vm.error ?? "")
        }
    }

    // MARK: - Sub-views

    @ViewBuilder
    private var content: some View {
        ScrollView {
            LazyVStack(spacing: 10) {
                ForEach(Array(vm.filteredDeals.enumerated()), id: \.element.id) { idx, deal in
                    NavigationLink(value: deal.id) {
                        DealCardView(deal: deal)
                    }
                    .buttonStyle(.plain)
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 16)
                    .animation(
                        .spring(response: 0.55, dampingFraction: 0.85)
                            .delay(Double(min(idx, 12)) * 0.05),
                        value: appeared
                    )
                    .contextMenu {
                        if let link = deal.link, !link.isEmpty, let url = URL(string: link) {
                            Link(destination: url) {
                                Label(
                                    tr("app.open_link", fallback: "Apri link"),
                                    systemImage: "safari"
                                )
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 24)
        }
        .refreshable { await vm.refresh() }
    }

    /// Il filtro è SEMPRE in toolbar — i settori specifici si popolano dal
    /// flow ma sappiamo a prescindere che esisteranno. Mostrare il bottone
    /// subito evita lo snap quando arrivano i dati.
    @ToolbarContentBuilder private var sectorFilterToolbarItem: some ToolbarContent {
        ToolbarItem(placement: .topBarTrailing) {
            Menu {
                Picker(selection: $vm.selectedSector, label: EmptyView()) {
                    Text(tr("app.deals.filter.all", fallback: "Tutti"))
                        .tag(String?.none)
                    ForEach(vm.sectors, id: \.self) { sector in
                        Text(sector).tag(String?.some(sector))
                    }
                }
            } label: {
                Image(systemName: vm.selectedSector == nil
                      ? "line.3.horizontal.decrease.circle"
                      : "line.3.horizontal.decrease.circle.fill")
                    .accessibilityLabel(Text(tr(
                        "app.deals.filter.label",
                        fallback: "Filtra per settore"
                    )))
            }
            .tint(AppTheme.Colors.brandTintAdaptive)
        }
    }
}

#Preview {
    NavigationStack {
        DealListView()
    }
}
