package it.mensa.app.features.events

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * eventsModule — Koin module for the Events feature.
 *
 * NOT registered in AppModule.kt yet.
 * Usage: include in Koin modules list alongside appModule.
 *
 * NavGraph entry point: NavGraphBuilder.eventsNavGraph(navController)
 */
val eventsModule = module {
    viewModel { EventListViewModel() }
    viewModel { (eventId: String) -> EventDetailViewModel(eventId) }
    viewModel { (eventId: String?) -> AddEventViewModel(eventId) }
    viewModel { EventCalendarViewModel() }
    viewModel { EventMapViewModel() }
}
