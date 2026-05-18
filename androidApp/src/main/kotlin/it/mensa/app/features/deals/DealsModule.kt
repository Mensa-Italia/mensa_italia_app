package it.mensa.app.features.deals

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * dealsModule — Koin module for the Deals feature.
 *
 * Registered alongside [it.mensa.app.di.appModule] in MensaApplication.
 * ViewModels use parametrized injection for deal ID navigation.
 */
val dealsModule = module {
    viewModel { DealListViewModel() }
    viewModel { (id: String) -> DealDetailViewModel(id) }
    viewModel { (id: String?) -> AddDealViewModel(id) }
}
