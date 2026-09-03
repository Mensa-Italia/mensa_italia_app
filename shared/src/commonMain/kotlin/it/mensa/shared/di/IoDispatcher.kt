package it.mensa.shared.di

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Il dispatcher per il lavoro che tocca il disco.
 *
 * `Dispatchers.IO` esiste su JVM e su Kotlin/Native, ma non nella superficie
 * comune di kotlinx-coroutines: questo modulo ha solo `commonMain`,
 * `androidMain` e `appleMain`, senza un source set intermedio da cui vederlo.
 * Da qui l'expect/actual, che nelle due implementazioni e' letteralmente
 * `Dispatchers.IO`.
 *
 * Serve a [wipeAllUserData]: la catena del logout parte da `viewModelScope`
 * (Android) e da un `Task` di SwiftUI (iOS), cioe' dal thread principale in
 * entrambi i casi, e le DELETE piu' il VACUUM riscrivono l'intero file del
 * database.
 */
internal expect val ioDispatcher: CoroutineDispatcher
