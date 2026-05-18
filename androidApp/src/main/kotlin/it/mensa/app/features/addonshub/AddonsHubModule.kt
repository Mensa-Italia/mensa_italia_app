package it.mensa.app.features.addonshub

import it.mensa.app.features.addonshub.AddonsHubViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * addonsHubModule — Koin bindings for AddonsHub feature.
 *
 * Register alongside appModule in [it.mensa.app.MensaApplication.onCreate].
 */
val addonsHubModule = module {
    viewModel { AddonsHubViewModel() }
}
