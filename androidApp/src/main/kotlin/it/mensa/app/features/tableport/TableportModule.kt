package it.mensa.app.features.tableport

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * tableportModule — Koin bindings for Tableport (passport + QR scanner) feature.
 *
 * Register alongside appModule in [it.mensa.app.MensaApplication.onCreate].
 */
val tableportModule = module {
    viewModel { PassportViewModel() }
    viewModel { QrScannerViewModel() }
}
