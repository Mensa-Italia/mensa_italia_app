package it.mensa.shared

import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.di.contentModule
import it.mensa.shared.di.databaseModule
import it.mensa.shared.di.extrasModule
import it.mensa.shared.di.platformModule
import it.mensa.shared.di.sharedModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

object MensaSdk {
    fun greet(): String = "Mensa shared SDK ready"
    fun apiBaseUrl(): String = "https://svc.mensa.it"
    fun databaseSchemaVersion(): Int = MensaDatabase.Schema.version.toInt()

    fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
        startKoin {
            appDeclaration()
            modules(sharedModule, platformModule, databaseModule, contentModule, extrasModule)
        }

    fun doInitKoinIos() = initKoin {}
}
