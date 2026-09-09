package it.mensa.app.features.locations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import it.mensa.shared.model.LocationModel
import org.koin.androidx.compose.koinViewModel

// ─── Route constants ─────────────────────────────────────────────────────────

object LocationRoutes {
    const val GRAPH   = "locations/picker"
    const val SAVED   = "locations/picker/saved"
    const val MAP     = "locations/picker/map"
    const val DETAILS = "locations/picker/details"

    const val RESULT_LOCATION_ID = "picked_location_id"

    /** Posizione gia' scelta dal chiamante, per aprire la mappa sul punto giusto. */
    const val ARG_CURRENT_LOCATION_ID = "current_location_id"
}

// ─── Nav graph builder ────────────────────────────────────────────────────────

/**
 * locationsNavGraph — il flusso "scegli/crea posizione", tre pagine a tutto schermo.
 *
 * Mappa e tastiera non stanno sullo stesso schermo: la mappa si prende
 * l'altezza intera nella pagina [LocationRoutes.MAP], il form la sua in
 * [LocationRoutes.DETAILS]. E' un nested graph e non tre rotte sciolte perche'
 * il draft ([NewLocationViewModel]) e' scoped al graph e deve sopravvivere
 * all'andirivieni fra mappa e dettagli.
 *
 * Il risultato torna al chiamante come id sul SavedStateHandle: si scrive
 * *dopo* il pop, altrimenti finisce sull'entry che sta per sparire.
 */
fun NavGraphBuilder.locationsNavGraph(navController: NavController) {
    navigation(route = LocationRoutes.GRAPH, startDestination = LocationRoutes.SAVED) {

        composable(LocationRoutes.SAVED) {
            SavedLocationsScreen(
                onPicked = { loc -> navController.finishLocationPicker(loc) },
                onAddClick = { navController.navigate(LocationRoutes.MAP) },
                onCancel = { navController.popBackStack(LocationRoutes.GRAPH, inclusive = true) },
            )
        }

        composable(LocationRoutes.MAP) { entry ->
            val graphEntry = entry.rememberGraphEntry(navController)
            val vm = koinViewModel<NewLocationViewModel>(viewModelStoreOwner = graphEntry)
            LaunchedEffect(vm) {
                graphEntry.savedStateHandle
                    .get<String>(LocationRoutes.ARG_CURRENT_LOCATION_ID)
                    ?.let(vm::startFrom)
            }
            PickOnMapScreen(
                vm = vm,
                onNext = { navController.navigate(LocationRoutes.DETAILS) },
                onCancel = { navController.popBackStack(LocationRoutes.GRAPH, inclusive = true) },
            )
        }

        composable(LocationRoutes.DETAILS) { entry ->
            NameLocationScreen(
                vm = koinViewModel(viewModelStoreOwner = entry.rememberGraphEntry(navController)),
                onSaved = { loc -> navController.finishLocationPicker(loc) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Apre il picker. [currentLocationId] e' la posizione gia' selezionata dal
 * chiamante, se ce n'e' una: serve solo a inquadrare la mappa.
 */
fun NavController.navigateToLocationPicker(currentLocationId: String? = null) {
    navigate(LocationRoutes.GRAPH)
    if (currentLocationId != null) {
        getBackStackEntry(LocationRoutes.GRAPH)
            .savedStateHandle[LocationRoutes.ARG_CURRENT_LOCATION_ID] = currentLocationId
    }
}

/**
 * L'entry del graph, che ospita il draft ([NewLocationViewModel]) condiviso fra
 * MAP e DETAILS.
 *
 * Risolta una volta sola e tenuta da [remember], con la pagina corrente come
 * chiave. Chiudere il picker fa il pop del graph, che esce dal back stack
 * all'istante, mentre la pagina resta composta per tutta l'animazione di uscita
 * e in quel frattempo si ricompone almeno una volta: cercare li' di nuovo la
 * rotta del graph e' un [IllegalArgumentException], ed e' la chiusura dell'app
 * che si vedeva toccando "Salva posizione".
 *
 * L'oggetto invece resta valido: finche' la pagina figlia sta uscendo, il graph
 * e' "in transizione" e Navigation non lo distrugge ne' svuota il suo
 * ViewModelStore. E' solo la ricerca per rotta a non essere piu' possibile.
 */
@Composable
private fun NavBackStackEntry.rememberGraphEntry(navController: NavController): NavBackStackEntry =
    remember(this) { navController.getBackStackEntry(LocationRoutes.GRAPH) }

/** Chiude il flusso e consegna la posizione scelta a chi l'ha aperto. */
private fun NavController.finishLocationPicker(location: LocationModel) {
    popBackStack(LocationRoutes.GRAPH, inclusive = true)
    currentBackStackEntry?.savedStateHandle?.set(LocationRoutes.RESULT_LOCATION_ID, location.id)
}
