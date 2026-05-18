package it.mensa.app.support

import it.mensa.shared.KoinAccess
import it.mensa.shared.koinAccess as sharedKoinAccess

/**
 * Android-side KoinAccess accessor.
 *
 * Delegates to the shared module's [it.mensa.shared.koinAccess] which
 * resolves all repositories + auth + i18n from the Koin container.
 * Do NOT duplicate repository bindings here — they live in the shared module.
 */
fun koinAccess(): KoinAccess = sharedKoinAccess()
