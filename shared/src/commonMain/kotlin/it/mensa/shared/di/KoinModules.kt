package it.mensa.shared.di

import it.mensa.shared.api.AuthRefresherHolder
import it.mensa.shared.api.HttpClientFactory
import it.mensa.shared.net.Connectivity
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.api.endpoints.FileLinkApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.integrity.IntegrityGuard
import it.mensa.shared.integrity.IntegrityProvider
import it.mensa.shared.repository.DevicesRepository
import it.mensa.shared.spotlight.SpotlightSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    single {
        val scope = this
        HttpClientFactory(get<ITokenStore>()).create().also { client ->
            // Account sospeso e prova di integrita' (Play Integrity / App Attest).
            // L'utente si legge a ogni richiesta e non qui: AuthRepository nasce
            // dopo il client, e dipende da lui.
            IntegrityGuard(
                client = client,
                provider = get<IntegrityProvider>(),
                userId = { scope.getOrNull<AuthRepository>()?.currentUser?.value?.id?.takeIf { it.isNotBlank() } },
                // Dove girano i report di un fallimento della prova: fuori dal
                // percorso della richiesta, e un report che fallisce non ferma gli altri.
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            ).install()
        }
    }
    single { Connectivity(get(), get()) }
    single { AuthApi(get()) }
    single { FileLinkApi(get()) }
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
    single {
        val scope = this
        AuthRepository(get(), get(), get(), get(), get(), get(), get(),
            unregisterDevice = { userId -> scope.getOrNull<DevicesRepository>()?.unregisterCurrent(userId) },
        )
    }
    single { SpotlightSyncEngine(regSoci = get(), httpClient = get(), db = get()) }
}

expect val platformModule: Module
