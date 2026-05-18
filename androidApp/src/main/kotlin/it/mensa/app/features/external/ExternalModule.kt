package it.mensa.app.features.external

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * externalModule — Koin bindings for External addon WebView feature.
 */
val externalModule = module {
    viewModel { (addonId: String) -> ExternalAddonViewModel(addonId) }
}
