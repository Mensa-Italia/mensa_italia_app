package it.mensa.shared.di

import it.mensa.shared.api.endpoints.AddonsApi
import it.mensa.shared.api.endpoints.BoutiqueApi
import it.mensa.shared.api.endpoints.LocalOfficesApi
import it.mensa.shared.api.endpoints.CalendarLinksApi
import it.mensa.shared.api.endpoints.DevicesApi
import it.mensa.shared.api.endpoints.DocumentsApi
import it.mensa.shared.api.endpoints.ExAppsApi
import it.mensa.shared.api.endpoints.LocationsApi
import it.mensa.shared.api.endpoints.MetadataApi
import it.mensa.shared.api.endpoints.OrgChartApi
import it.mensa.shared.api.endpoints.NotificationsApi
import it.mensa.shared.api.endpoints.PodcastsApi
import it.mensa.shared.api.endpoints.QuidApi
import it.mensa.shared.api.endpoints.PaymentMethodsApi
import it.mensa.shared.api.endpoints.ReceiptsApi
import it.mensa.shared.api.endpoints.SearchApi
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.api.endpoints.TicketsApi
import it.mensa.shared.i18n.I18n
import it.mensa.shared.i18n.TranslationLoader
import it.mensa.shared.iqtest.MensaTestClient
import it.mensa.shared.onboarding.OnboardingState
import it.mensa.shared.repository.AddonsRepository
import it.mensa.shared.repository.BoutiqueRepository
import it.mensa.shared.repository.LocalOfficesRepository
import it.mensa.shared.repository.CalendarLinksRepository
import it.mensa.shared.repository.DevicesRepository
import it.mensa.shared.repository.DocumentsRepository
import it.mensa.shared.repository.ExAppsRepository
import it.mensa.shared.repository.LocationsRepository
import it.mensa.shared.repository.MetadataRepository
import it.mensa.shared.repository.OrgChartRepository
import it.mensa.shared.repository.NotificationsRepository
import it.mensa.shared.repository.PodcastsRepository
import it.mensa.shared.repository.QuidRepository
import it.mensa.shared.repository.PaymentMethodsRepository
import it.mensa.shared.repository.ReceiptsRepository
import it.mensa.shared.repository.SearchRepository
import it.mensa.shared.repository.TicketsRepository
import it.mensa.shared.sse.RealtimeClient
import org.koin.dsl.module

val extrasModule = module {
    // Json is provided by contentModule. PocketBaseClient + HttpClient + MensaDatabase too.

    single { NotificationsApi(get(), get()) }
    single { DevicesApi(get()) }
    single { SettingsApi(get(), get()) }
    single { AddonsApi(get(), get()) }
    single { LocationsApi(get(), get()) }
    single { DocumentsApi(get()) }
    single { BoutiqueApi(get()) }
    single { SearchApi(get()) }
    single { TicketsApi(get()) }
    single { ReceiptsApi(get(), get()) }
    single { PaymentMethodsApi(get()) }
    single { CalendarLinksApi(get()) }
    single { MetadataApi(get()) }
    single { OrgChartApi(get()) }
    single { QuidApi(get(), get()) }
    single { PodcastsApi(get()) }
    single { LocalOfficesApi(get()) }
    single { ExAppsApi(get(), get()) }

    // i18n
    single { TranslationLoader(get(), get(), get(), get()) }
    single { I18n(get()) }

    // Realtime SSE client
    single { RealtimeClient(get(), get()) }

    // Offline-first repositories
    single { NotificationsRepository(get(), get(), get(), get()) }
    single { AddonsRepository(get(), get(), get()) }
    single { LocationsRepository(get(), get(), get()) }
    single { SearchRepository(get()) }
    single { BoutiqueRepository(get(), get(), get()) }
    single { DocumentsRepository(get(), get()) }
    single { DevicesRepository(get(), get()) }
    single { TicketsRepository(get(), get(), get(), get()) }
    single { ReceiptsRepository(get(), get(), get(), get()) }
    single { CalendarLinksRepository(get(), get(), get()) }
    single { PaymentMethodsRepository(get(), get()) }
    single { MetadataRepository(get()) }
    single { OrgChartRepository(get()) }
    single { QuidRepository(get()) }
    single { PodcastsRepository(get()) }
    single { LocalOfficesRepository(get()) }
    single { ExAppsRepository(get(), get()) }

    // Onboarding gate
    single { OnboardingState(get()) }

    // Public IQ test scraper (dedicated Ktor client with cookie storage)
    single { MensaTestClient() }
}
