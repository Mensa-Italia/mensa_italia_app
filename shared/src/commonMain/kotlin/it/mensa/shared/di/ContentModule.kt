package it.mensa.shared.di

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.DealsApi
import it.mensa.shared.api.endpoints.EventSchedulesApi
import it.mensa.shared.api.endpoints.EventsApi
import it.mensa.shared.api.endpoints.RegSociApi
import it.mensa.shared.api.endpoints.SigsApi
import it.mensa.shared.api.endpoints.StampsApi
import it.mensa.shared.repository.DealsRepository
import it.mensa.shared.repository.EventSchedulesRepository
import it.mensa.shared.repository.EventsRepository
import it.mensa.shared.repository.RegSociRepository
import it.mensa.shared.repository.SigsRepository
import it.mensa.shared.repository.StampsRepository
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val contentModule = module {
    // JSON instance shared by repositories
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    // PocketBase generic client (uses the HttpClient from sharedModule)
    single { PocketBaseClient(get()) }

    // Endpoint APIs
    single { EventsApi(get()) }
    single { EventSchedulesApi(get()) }
    single { DealsApi(get()) }
    single { SigsApi(get()) }
    single { StampsApi(get()) }
    single { RegSociApi(get()) }

    // Offline-first repositories
    single { EventsRepository(get(), get(), get(), get()) }
    single { EventSchedulesRepository(get(), get()) }
    single { DealsRepository(get(), get(), get()) }
    single { SigsRepository(get(), get(), get()) }
    single { StampsRepository(get(), get(), get(), get()) }
    single { RegSociRepository(get(), get(), get()) }
}
