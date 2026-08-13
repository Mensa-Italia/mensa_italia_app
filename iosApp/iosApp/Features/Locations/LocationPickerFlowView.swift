import SwiftUI
import Shared

enum LocationPickerRoute: Hashable {
    case map, details
}

/// Contenitore del flusso "scegli o crea una posizione": lista salvate →
/// mappa → nome e indirizzo.
///
/// Va presentato con `fullScreenCover`, non con `.sheet`: il chiamante
/// (`AddEventView`, `AddDealView`) è già dentro una sheet, e impilarne una
/// terza faceva partire la mappa a metà schermo. A tutto schermo la mappa ha
/// la stessa altezza che ha su Android.
struct LocationPickerFlowView: View {
    /// Posizione già selezionata dal chiamante: apre la mappa dove si trova.
    var initial: LocationModel?
    /// Chiamata con la posizione scelta o appena creata. Il flusso si chiude
    /// da solo: al chiamante basta assegnarla.
    var onPicked: (LocationModel) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var path: [LocationPickerRoute] = []
    @State private var draft: NewLocationDraft

    init(initial: LocationModel? = nil, onPicked: @escaping (LocationModel) -> Void) {
        self.initial = initial
        self.onPicked = onPicked
        _draft = State(initialValue: NewLocationDraft(initial: initial))
    }

    var body: some View {
        NavigationStack(path: $path) {
            SavedLocationsView(
                onPicked: { loc in
                    onPicked(loc)
                    dismiss()
                },
                onAddClick: { path.append(.map) },
                onCancel: { dismiss() }
            )
            .navigationDestination(for: LocationPickerRoute.self) { route in
                switch route {
                case .map:
                    PickOnMapView(
                        draft: draft,
                        onNext: { path.append(.details) },
                        onCancel: { dismiss() }
                    )
                case .details:
                    NameLocationView(
                        draft: draft,
                        onSaved: { created in
                            // La posizione appena creata torna subito al
                            // chiamante: prima finiva nella lista e l'utente
                            // doveva ritrovarla e toccarla per selezionarla.
                            onPicked(created)
                            dismiss()
                        },
                        onBack: { path.removeLast() },
                        onCancel: { dismiss() }
                    )
                }
            }
        }
    }
}

#Preview {
    LocationPickerFlowView(onPicked: { _ in })
}
