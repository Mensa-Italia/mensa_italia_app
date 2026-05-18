package it.mensa.app.features.boutique

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * boutiqueModule — Koin bindings for Boutique feature.
 *
 * Register alongside appModule in [it.mensa.app.MensaApplication.onCreate].
 */
val boutiqueModule = module {
    viewModel { BoutiqueListViewModel() }
    viewModel { (id: String) -> BoutiqueProductViewModel(id) }
}
