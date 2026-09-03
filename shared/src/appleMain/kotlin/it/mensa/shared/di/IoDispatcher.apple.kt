package it.mensa.shared.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext

/**
 * Su Kotlin/Native `Dispatchers.IO` non e' utilizzabile: in
 * kotlinx-coroutines 1.10.2 e' dichiarato `internal` e resta pubblico solo
 * sul target JVM. Al suo posto un singolo thread dedicato al disco.
 *
 * Uno solo perche' il lavoro che ci passa e' seriale per natura — le DELETE,
 * il VACUUM e il checkpoint del wipe girano in sequenza sullo stesso file —
 * e perche' cosi' non si sottrae mai un worker al pool di `Dispatchers.Default`,
 * che e' dimensionato sui core e non va bloccato su un'operazione di I/O.
 *
 * Il dispatcher vive quanto il processo e non viene chiuso: e' un singleton di
 * modulo, esattamente come sarebbe `Dispatchers.IO`. Da qui l'opt-in.
 */
@OptIn(DelicateCoroutinesApi::class)
internal actual val ioDispatcher: CoroutineDispatcher =
    newFixedThreadPoolContext(nThreads = 1, name = "mensa-db-io")
