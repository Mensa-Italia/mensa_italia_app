package it.mensa.app.features.receipts

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * receiptsModule — Koin bindings for Receipts feature.
 *
 * Register alongside appModule in [it.mensa.app.MensaApplication.onCreate].
 */
val receiptsModule = module {
    viewModel { ReceiptsListViewModel() }
    viewModel { (id: String) -> ReceiptDetailViewModel(id) }
}
