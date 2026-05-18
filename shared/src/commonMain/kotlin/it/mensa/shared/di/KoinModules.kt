package it.mensa.shared.di

import it.mensa.shared.api.AuthRefresherHolder
import it.mensa.shared.api.HttpClientFactory
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.spotlight.SpotlightSyncEngine
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single { HttpClientFactory(get<ITokenStore>()).create() }
    single { AuthApi(get()) }
    single { OidcDiscoveryCache(get()) }
    single {
        TokenRefresher(get(), get(), get()).also {
            // Late-bind the refresher into the HTTP client's AuthPlugin.
            // The plugin is installed eagerly when the HttpClient is built but
            // can't take TokenRefresher as a constructor arg without creating
            // an HttpClient → TokenRefresher → HttpClient cycle. The holder
            // breaks the cycle.
            AuthRefresherHolder.refresher = it
        }
    }
    single { AuthRepository(get(), get(), get(), get(), get(), get()) }
    single { SpotlightSyncEngine(regSoci = get(), httpClient = get(), db = get()) }
}

expect val platformModule: Module
