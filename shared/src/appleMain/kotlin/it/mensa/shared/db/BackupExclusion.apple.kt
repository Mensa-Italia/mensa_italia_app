package it.mensa.shared.db

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.numberWithBool

/**
 * Tiene il database locale fuori dai backup di iOS.
 *
 * Il file `mensa.db` e' una copia di cio' che sta sul server — anagrafica dei
 * soci, documenti, ricevute, timbri — e su un dispositivo nuovo l'app lo
 * riscarica da sola dopo il login. In un backup non serve a niente e allarga
 * soltanto la superficie: chi mette le mani su un backup di iTunes/Finder,
 * anche cifrato, si porta via l'elenco dei soci.
 *
 * `NSURLIsExcludedFromBackupKey` e' l'unico interruttore che iOS offre, e vale
 * sia per iCloud sia per i backup locali. Lo mettiamo:
 *
 *  - sulla **cartella** `<ApplicationSupport>/databases`, perche' e' l'unica
 *    esclusione che resiste nel tempo: e' ereditata da tutto cio' che ci sta
 *    dentro, quindi copre anche i `-wal`/`-shm` che SQLite cancella e ricrea
 *    da capo — e con essi si porterebbe via l'attributo esteso;
 *  - su ogni **file** noto, perche' un attributo esplicito non costa niente e
 *    non dipende da come iOS decide di propagare quello della cartella.
 *
 * Idempotente: si puo' chiamare a ogni avvio. I file che non esistono ancora
 * (il `-wal` prima della prima scrittura) falliscono in silenzio, ed e'
 * corretto cosi' — la cartella li copre comunque.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun excludeDatabaseFromBackup(dbName: String) {
    val support = applicationSupportPath() ?: return
    val databasesDir = "$support/databases"

    excludePathFromBackup(databasesDir)
    listOf(
        "$databasesDir/$dbName",
        "$databasesDir/$dbName-journal",
        "$databasesDir/$dbName-wal",
        "$databasesDir/$dbName-shm",
    ).forEach { excludePathFromBackup(it) }
}

/**
 * Marca un singolo path come escluso dal backup.
 *
 * Torna `false` quando il path non esiste o iOS rifiuta di scrivere
 * l'attributo. Nessuno dei due casi e' un errore da propagare: un DB che
 * finisce nel backup e' un peggioramento della privacy, non un guasto, e far
 * fallire l'avvio dell'app per questo sarebbe sproporzionato.
 */
@OptIn(ExperimentalForeignApi::class)
private fun excludePathFromBackup(path: String): Boolean {
    if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return false
    val url = NSURL.fileURLWithPath(path)
    return runCatching {
        url.setResourceValue(NSNumber.numberWithBool(true), NSURLIsExcludedFromBackupKey, null)
    }.getOrDefault(false)
}

/**
 * `NSApplicationSupportDirectory` = 14, `NSUserDomainMask` = 1.
 *
 * `.convert()` perche' su watchosArm64 (32 bit) i due parametri sono `UInt`
 * mentre su iosArm64 sono `ULong`: il bridge K/N sceglie il tipo nativo della
 * piattaforma. Stessa ragione per cui lo fa [DriverFactory.deleteDbFile].
 */
@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportPath(): String? =
    NSSearchPathForDirectoriesInDomains(14u.convert(), 1u.convert(), true).firstOrNull() as? String
