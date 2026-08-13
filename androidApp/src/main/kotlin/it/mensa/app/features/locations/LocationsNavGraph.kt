package it.mensa.app.features.locations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

        composable(LocationRoutes.MAP) {
            val vm = navController.newLocationViewModel()
            LaunchedEffect(vm) {
                navController.getBackStackEntry(LocationRoutes.GRAPH)
                    .savedStateHandle
                    .get<String>(LocationRoutes.ARG_CURRENT_LOCATION_ID)
                    ?.let(vm::startFrom)
            }
            PickOnMapScreen(
                vm = vm,
                onNext = { navController.navigate(LocationRoutes.DETAILS) },
                onCancel = { navController.popBackStack(LocationRoutes.GRAPH, inclusive = true) },
            )
        }

        composable(LocationRoutes.DETAILS) {
            NameLocationScreen(
                vm = navController.newLocationViewModel(),
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

/** Il draft, preso dall'entry del graph cosi' e' lo stesso su MAP e DETAILS. */
@Composable
private fun NavController.newLocationViewModel(): NewLocationViewModel =
    koinViewModel(viewModelStoreOwner = getBackStackEntry(LocationRoutes.GRAPH))

/** Chiude il flusso e consegna la posizione scelta a chi l'ha aperto. */
private fun NavController.finishLocationPicker(location: LocationModel) {
    popBackStack(LocationRoutes.GRAPH, inclusive = true)
    currentBackStackEntry?.savedStateHandle?.set(LocationRoutes.RESULT_LOCATION_ID, location.id)
}
