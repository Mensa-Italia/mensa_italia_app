package it.mensa.app

import android.app.Application
import it.mensa.app.di.appModule
import it.mensa.app.features.addonshub.addonsHubModule
import it.mensa.app.features.boutique.boutiqueModule
import it.mensa.app.features.contacts.contactsModule
import it.mensa.app.features.deals.dealsModule
import it.mensa.app.features.documents.documentsModule
import it.mensa.app.features.events.eventsModule
import it.mensa.app.features.external.externalModule
import it.mensa.app.features.localoffices.localOfficesModule
import it.mensa.app.features.members.membersModule
import it.mensa.app.features.notifications.notificationsModule
import it.mensa.app.features.podcasts.podcastsModule
import it.mensa.app.features.publicarea.publicAreaModule
import it.mensa.app.features.quid.quidModule
import it.mensa.app.features.receipts.receiptsModule
import it.mensa.app.features.sigs.sigsModule
import it.mensa.app.features.tableport.tableportModule
import it.mensa.app.features.testassistant.testAssistantModule
import it.mensa.app.features.tickets.ticketsModule
import it.mensa.app.services.stripe.StripeService
import it.mensa.shared.MensaSdk
import it.mensa.shared.db.DriverFactory
import it.mensa.shared.di.initializeMensaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.mp.KoinPlatform

class MensaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize shared SDK Koin modules + Android-specific appModule
        MensaSdk.initKoin {
            androidContext(this@MensaApplication)
            modules(
                // Core
                appModule,
                // Feature modules (alphabetical)
                addonsHubModule,
                boutiqueModule,
                contactsModule,
                dealsModule,
                documentsModule,
                eventsModule,
                externalModule,
                localOfficesModule,
                membersModule,
                notificationsModule,
                podcastsModule,
                publicAreaModule,
                quidModule,
                receiptsModule,
                sigsModule,
                tableportModule,
                testAssistantModule,
                ticketsModule,
            )
        }

        // Initialize the SQLDelight database synchronously before any UI composable
        // can instantiate a ViewModel that calls koinAccess().
        // AndroidSqliteDriver.createDriver() completes synchronously despite being declared
        // suspend (it only needs the suspend wrapper for wasmJs compatibility).
        runBlocking {
            val factory = KoinPlatform.getKoin().get<DriverFactory>()
            initializeMensaDatabase(factory)
        }

        // Fetch the Stripe publishable key and initialize the SDK in the
        // background. Both addPaymentMethod() and donation flows call
        // bootstrap() again before opening PaymentSheet, so a slow network
        // here is non-fatal — this just front-loads the work so the first
        // payment screen feels instant.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            KoinPlatform.getKoin().get<StripeService>().bootstrap()
        }
    }
}
