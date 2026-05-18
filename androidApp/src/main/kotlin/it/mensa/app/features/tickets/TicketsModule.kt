package it.mensa.app.features.tickets

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * ticketsModule — Koin bindings for Tickets feature.
 *
 * Kept separate from [it.mensa.app.di.appModule] per feature-module convention.
 * Register this in [it.mensa.app.MensaApplication.onCreate] alongside appModule.
 */
val ticketsModule = module {
    viewModel { TicketsListViewModel() }
    viewModel { (id: String) -> TicketDetailViewModel(id) }
}
