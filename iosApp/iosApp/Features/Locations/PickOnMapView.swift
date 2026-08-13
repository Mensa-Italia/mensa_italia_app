import SwiftUI
import MapKit
import CoreLocation

/// Pagina 2 del flusso: qui si sceglie soltanto la coppia lat/lon.
///
/// Il pin non si muove mai — si muove la mappa sotto di lui. Perché il punto
/// salvato coincida con la punta disegnata, mappa e mirino stanno nello stesso
/// `ZStack` e condividono lo stesso `ignoresSafeArea`: quando la mappa si
/// estendeva sotto la safe area e il pin no, i due centri geometrici non
/// coincidevano e si salvavano coordinate spostate di una ventina di metri.
struct PickOnMapView: View {
    let draft: NewLocationDraft
    var onNext: () -> Void
    var onCancel: () -> Void

    @State private var camera: MapCameraPosition
    /// Fermate della camera che non sono gesti dell'utente e quindi non devono
    /// marcare il pin come posizionato: la prima arriva al layout iniziale, le
    /// altre dopo ogni animazione che facciamo partire noi. È un contatore e
    /// non un flag perché le due cose possono accavallarsi (il fix GPS può
    /// arrivare prima che la mappa si sia assestata la prima volta).
    @State private var pendingProgrammaticSettles = 1
    @State private var authorization: CLAuthorizationStatus = .notDetermined
    @State private var locating = false
    @State private var showPermissionHint = false
    @FocusState private var searchFocused: Bool

    init(draft: NewLocationDraft, onNext: @escaping () -> Void, onCancel: @escaping () -> Void) {
        self.draft = draft
        self.onNext = onNext
        self.onCancel = onCancel
        // La camera parte dalla bozza e non da una costante: tornando qui dalla
        // pagina dei dettagli la mappa deve ritrovare il punto già scelto.
        _camera = State(initialValue: .region(draft.initialRegion))
    }

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .top) {
                mapLayer
                searchPanel(maxResultsHeight: proxy.size.height * 0.4)
            }
            .overlay(alignment: .bottomTrailing) { locationControls }
        }
        .ignoresSafeArea(edges: .bottom)
        .navigationTitle(tr("app.location.map.title", fallback: "Scegli posizione"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(tr("app.location.cancel", fallback: "Annulla")) { onCancel() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(tr("app.location.map.next", fallback: "Avanti")) {
                    searchFocused = false
                    onNext()
                }
                .fontWeight(.semibold)
                .disabled(!draft.pinPositioned)
            }
        }
        .task { await centerOnStartIfAllowed() }
    }

    // MARK: - Mappa e mirino

    private var mapLayer: some View {
        ZStack {
            Map(position: $camera) {
                if authorization == .authorizedWhenInUse || authorization == .authorizedAlways {
                    UserAnnotation()
                }
            }
            .mapStyle(.standard)
            .onMapCameraChange(frequency: .onEnd) { context in
                cameraSettled(context)
            }

            // Il punto esatto che verrà salvato. Il glifo `mappin` è alto e
            // opaco: senza questo puntino l'utente non sa a quale pixel della
            // punta si riferisce la coordinata.
            Circle()
                .fill(AppTheme.Colors.brandTintAdaptive)
                .frame(width: 6, height: 6)
                .allowsHitTesting(false)

            // `mappin` ha la punta sul bordo inferiore del suo box: allineando
            // quel bordo alla linea centrale dello ZStack la punta cade esatta
            // sul centro, senza offset magici da ritarare a ogni cambio di size.
            Image(systemName: "mappin")
                .font(.system(size: 32, weight: .bold))
                .foregroundStyle(AppTheme.Colors.brandTintAdaptive)
                .shadow(radius: 3, y: 2)
                .alignmentGuide(VerticalAlignment.center) { d in d[.bottom] }
                .allowsHitTesting(false)
        }
    }

    private func cameraSettled(_ context: MapCameraUpdateContext) {
        draft.search.setRegion(context.region)
        let center = context.camera.centerCoordinate
        guard pendingProgrammaticSettles == 0 else {
            pendingProgrammaticSettles -= 1
            draft.center = center
            return
        }
        draft.centerMoved(to: center)
    }

    // MARK: - Ricerca

    @ViewBuilder
    private func searchPanel(maxResultsHeight: CGFloat) -> some View {
        VStack(spacing: 6) {
            searchField
            if draft.searchState == .results, !draft.searchResults.isEmpty {
                resultsList(maxHeight: maxResultsHeight)
            } else if draft.searchState == .noResults {
                infoCard(tr("app.location.search.noResults", fallback: "Nessun risultato"))
            } else {
                infoCard(tr("app.location.map.hint",
                            fallback: "Muovi la mappa per posizionare il punto"))
            }
            Spacer(minLength: 0)
        }
        .padding(.horizontal)
        .padding(.top, 8)
    }

    private var searchField: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .foregroundStyle(.secondary)

            TextField(
                tr("app.location.search.placeholder", fallback: "Cerca un indirizzo…"),
                text: Binding(
                    get: { draft.searchQuery },
                    set: { draft.updateQuery($0) }
                )
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .submitLabel(.search)
            .focused($searchFocused)
            .onSubmit {
                Task {
                    if await draft.submitSearch() { focusCameraOnDraftCenter() }
                    searchFocused = false
                }
            }

            if draft.searchState == .loading {
                ProgressView().controlSize(.small)
            } else if !draft.searchQuery.isEmpty {
                Button {
                    draft.clearQuery()
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(tr("app.location.search.clear", fallback: "Cancella"))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func resultsList(maxHeight: CGFloat) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                ForEach(draft.searchResults) { item in
                    Button {
                        Task {
                            if await draft.apply(item) { focusCameraOnDraftCenter() }
                            searchFocused = false
                        }
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(.subheadline)
                                .foregroundStyle(.primary)
                            if !item.subtitle.isEmpty {
                                Text(item.subtitle)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 14)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    Divider().opacity(0.5)
                }
            }
        }
        .scrollBounceBehavior(.basedOnSize)
        .frame(maxHeight: maxHeight)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func infoCard(_ text: String) -> some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - "La mia posizione"

    private var locationControls: some View {
        VStack(alignment: .trailing, spacing: 8) {
            if showPermissionHint { permissionCard }

            Button {
                Task { await centerOnUser() }
            } label: {
                Group {
                    if locating {
                        ProgressView()
                    } else {
                        Image(systemName: "location.fill")
                            .font(.system(size: 17, weight: .semibold))
                    }
                }
                .frame(width: 44, height: 44)
                .background(.regularMaterial, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(tr("app.location.map.myLocation", fallback: "La mia posizione"))
        }
        .padding(.trailing, 16)
        .padding(.bottom, 44)
    }

    private var permissionCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(tr("app.location.map.permission.body",
                    fallback: "Consenti l'accesso alla posizione per centrare la mappa dove ti trovi"))
                .font(.footnote)
                .foregroundStyle(.secondary)
            Button {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text(tr("app.location.map.permission.action", fallback: "Consenti"))
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
        .frame(maxWidth: 260, alignment: .leading)
        .padding(12)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    /// All'apertura ci centriamo sull'utente solo se il permesso c'è già:
    /// far comparire il prompt di sistema per il solo fatto di aver aperto una
    /// pagina è il modo più veloce per farselo negare per sempre.
    private func centerOnStartIfAllowed() async {
        authorization = LocationProvider.shared.authorizationStatus
        guard !draft.pinPositioned else { return }
        guard authorization == .authorizedWhenInUse || authorization == .authorizedAlways else { return }
        guard let location = await LocationProvider.shared.requestOnce() else { return }
        // La camera si sposta ma il pin resta "non posizionato": centrare in
        // automatico non è una scelta dell'utente.
        move(to: location.coordinate, meters: 1000)
    }

    private func centerOnUser() async {
        authorization = LocationProvider.shared.authorizationStatus
        if authorization == .denied || authorization == .restricted {
            // Già negato una volta: il dialog di sistema non ricompare più,
            // riproporlo servirebbe solo a far sembrare il tasto rotto.
            showPermissionHint = true
            return
        }
        locating = true
        let location = await LocationProvider.shared.requestOnce()
        locating = false
        authorization = LocationProvider.shared.authorizationStatus
        guard let location else {
            // Permesso appena negato o fix non arrivato: niente alert, il
            // flusso resta completabile posizionando la mappa a mano.
            showPermissionHint = (authorization == .denied || authorization == .restricted)
            return
        }
        showPermissionHint = false
        move(to: location.coordinate, meters: 1000)
        draft.centerMoved(to: location.coordinate)
    }

    private func focusCameraOnDraftCenter() {
        move(to: draft.center, meters: 600)
    }

    private func move(to coordinate: CLLocationCoordinate2D, meters: CLLocationDistance) {
        pendingProgrammaticSettles += 1
        withAnimation(.easeInOut(duration: 0.4)) {
            camera = .region(
                MKCoordinateRegion(center: coordinate,
                                   latitudinalMeters: meters,
                                   longitudinalMeters: meters)
            )
        }
    }
}

#Preview {
    NavigationStack {
        PickOnMapView(draft: NewLocationDraft(initial: nil), onNext: {}, onCancel: {})
    }
}
