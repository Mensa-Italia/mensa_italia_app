package it.mensa.shared.integrity

import io.ktor.http.decodeURLPart
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * La stringa che la prova lega alla richiesta, identica sui due lati:
 *
 *     clientData = challenge + "|" + METHOD + "|" + PATH
 *
 * `PATH` e' il solo path, senza query string ne' host. Lato server e'
 * `r.URL.Path`, cioe' il path *decodificato*: per questo qui si decodifica
 * l'`encodedPath` di Ktor. Per i path di oggi (id alfanumerici) i due coincidono;
 * differirebbero solo con un `%xx` nel path, e in quel caso usare l'encoded
 * romperebbe la verifica.
 */
object IntegrityClientData {

    fun build(challenge: String, method: String, path: String): String =
        challenge + "|" + method.uppercase() + "|" + path

    /** Il path com'e' per il server (`r.URL.Path`) a partire da quello codificato di Ktor. */
    fun serverPath(encodedPath: String): String = encodedPath.decodeURLPart()

    /** Android: `requestHash` di Play Integrity = base64url senza padding di SHA-256(UTF-8(clientData)). */
    @OptIn(ExperimentalEncodingApi::class)
    fun requestHash(clientData: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(clientDataHash(clientData))

    /** iOS: `clientDataHash` per App Attest = SHA-256(UTF-8(clientData)), 32 byte. */
    fun clientDataHash(clientData: String): ByteArray = Sha256.digest(clientData.encodeToByteArray())
}

/**
 * Quali richieste portano la prova.
 *
 * Dalla revisione 2 del contratto vale per *tutte* le richieste autenticate al
 * nostro server sotto `/api/`, non piu' solo per timbri e medaglie: quel che
 * resta qui e' l'elenco delle esenzioni, che sono quelle senza le quali il
 * flusso non funzionerebbe (la sfida e l'attestazione) o che non passano da
 * Ktor (i file, scaricati dai loader nativi) o che non sono autenticate
 * (apertura dell'SSE, health).
 */
object IntegrityScope {

    private const val API_PREFIX = "/api/"

    /** True se [path] (quello decodificato, vedi [IntegrityClientData.serverPath]) e' esente. */
    fun isExempt(method: String, path: String): Boolean {
        if (!path.startsWith(API_PREFIX)) return true
        val m = method.uppercase()
        return when {
            path == "/api/integrity/challenge" -> true
            path == "/api/integrity/ios/attest" -> true
            path.startsWith("/api/files/") -> true
            // Aprire lo stream e' esente; le sottoscrizioni (POST) no.
            path == "/api/realtime" && m == "GET" -> true
            path == "/api/health" -> true
            else -> false
        }
    }
}

/**
 * Lo specchio, lato client, dell'allowlist dei `blocked` (contratto §7): le
 * poche route che il server continua a servire a un account sospeso, perche'
 * restano soci validi — registrazione del dispositivo e push, conferma delle
 * app esterne, lettura delle proprie notifiche.
 *
 * Serve a una cosa sola: una risposta riuscita su una di queste route NON prova
 * che l'account sia stato riattivato, quindi non deve spegnere
 * [it.mensa.shared.auth.AccountStatus]. Senza, la registrazione del device
 * (che riesce sempre) accenderebbe e spegnerebbe la schermata di account
 * sospeso a ogni foreground. Se il server allarga l'allowlist e qui si
 * dimentica, l'effetto e' solo che la schermata si spegne piu' tardi, alla
 * prima risposta riuscita su una route non in elenco.
 */
object BlockedAllowlist {

    fun allows(method: String, path: String): Boolean {
        val m = method.uppercase()
        return when {
            path.startsWith("/api/collections/users_devices/") -> true
            path.startsWith("/api/collections/ex_granted_permissions/") -> true
            path.startsWith("/api/collections/ex_apps/") && m == "GET" -> true
            path == "/api/cs/exapp/export/decide" -> true
            // Le notifiche: elenco (con filtro) e GET/PATCH/DELETE del singolo
            // record, cosi' un sospeso riapre la conferma che si e' perso. Niente
            // POST, niente realtime.
            path == "/api/collections/user_notifications/records" && m == "GET" -> true
            path.startsWith("/api/collections/user_notifications/records/") &&
                (m == "GET" || m == "PATCH" || m == "DELETE") -> true
            // Refresh dell'auth PocketBase e record dell'utente stesso. `/api/cs/me`
            // e' il modo in cui l'app lo legge: non si sa se il server lo lascia
            // passare, e se lo fa non dice niente sull'account.
            path == "/api/collections/users/auth-refresh" -> true
            path.startsWith("/api/collections/users/records/") && m == "GET" -> true
            path == "/api/cs/me" -> true
            else -> false
        }
    }
}

/** SHA-256 in Kotlin puro: serve a tutte le piattaforme senza una dipendenza in piu'. */
internal object Sha256 {
    private val K = uintArrayOf(
        0x428a2f98u, 0x71374491u, 0xb5c0fbcfu, 0xe9b5dba5u, 0x3956c25bu, 0x59f111f1u, 0x923f82a4u, 0xab1c5ed5u,
        0xd807aa98u, 0x12835b01u, 0x243185beu, 0x550c7dc3u, 0x72be5d74u, 0x80deb1feu, 0x9bdc06a7u, 0xc19bf174u,
        0xe49b69c1u, 0xefbe4786u, 0x0fc19dc6u, 0x240ca1ccu, 0x2de92c6fu, 0x4a7484aau, 0x5cb0a9dcu, 0x76f988dau,
        0x983e5152u, 0xa831c66du, 0xb00327c8u, 0xbf597fc7u, 0xc6e00bf3u, 0xd5a79147u, 0x06ca6351u, 0x14292967u,
        0x27b70a85u, 0x2e1b2138u, 0x4d2c6dfcu, 0x53380d13u, 0x650a7354u, 0x766a0abbu, 0x81c2c92eu, 0x92722c85u,
        0xa2bfe8a1u, 0xa81a664bu, 0xc24b8b70u, 0xc76c51a3u, 0xd192e819u, 0xd6990624u, 0xf40e3585u, 0x106aa070u,
        0x19a4c116u, 0x1e376c08u, 0x2748774cu, 0x34b0bcb5u, 0x391c0cb3u, 0x4ed8aa4au, 0x5b9cca4fu, 0x682e6ff3u,
        0x748f82eeu, 0x78a5636fu, 0x84c87814u, 0x8cc70208u, 0x90befffau, 0xa4506cebu, 0xbef9a3f7u, 0xc67178f2u,
    )

    fun digest(data: ByteArray): ByteArray {
        val bitLength = data.size.toLong() * 8
        // Padding: 0x80, zeri fino a 56 mod 64, poi la lunghezza in bit su 8 byte big-endian.
        val padded = ByteArray(((data.size + 8) / 64 + 1) * 64)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        for (i in 0 until 8) padded[padded.size - 1 - i] = (bitLength ushr (8 * i)).toByte()

        var h0 = 0x6a09e667u
        var h1 = 0xbb67ae85u
        var h2 = 0x3c6ef372u
        var h3 = 0xa54ff53au
        var h4 = 0x510e527fu
        var h5 = 0x9b05688cu
        var h6 = 0x1f83d9abu
        var h7 = 0x5be0cd19u

        val w = UIntArray(64)
        for (block in 0 until padded.size / 64) {
            val base = block * 64
            for (t in 0 until 16) {
                val o = base + t * 4
                w[t] = ((padded[o].toUInt() and 0xffu) shl 24) or
                    ((padded[o + 1].toUInt() and 0xffu) shl 16) or
                    ((padded[o + 2].toUInt() and 0xffu) shl 8) or
                    (padded[o + 3].toUInt() and 0xffu)
            }
            for (t in 16 until 64) {
                val s0 = w[t - 15].rotateRight(7) xor w[t - 15].rotateRight(18) xor (w[t - 15] shr 3)
                val s1 = w[t - 2].rotateRight(17) xor w[t - 2].rotateRight(19) xor (w[t - 2] shr 10)
                w[t] = w[t - 16] + s0 + w[t - 7] + s1
            }

            var a = h0; var b = h1; var c = h2; var d = h3
            var e = h4; var f = h5; var g = h6; var h = h7
            for (t in 0 until 64) {
                val bigS1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + bigS1 + ch + K[t] + w[t]
                val bigS0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = bigS0 + maj
                h = g; g = f; f = e; e = d + temp1
                d = c; c = b; b = a; a = temp1 + temp2
            }
            h0 += a; h1 += b; h2 += c; h3 += d; h4 += e; h5 += f; h6 += g; h7 += h
        }

        val out = ByteArray(32)
        intArrayOf(h0.toInt(), h1.toInt(), h2.toInt(), h3.toInt(), h4.toInt(), h5.toInt(), h6.toInt(), h7.toInt())
            .forEachIndexed { i, v ->
                out[i * 4] = (v ushr 24).toByte()
                out[i * 4 + 1] = (v ushr 16).toByte()
                out[i * 4 + 2] = (v ushr 8).toByte()
                out[i * 4 + 3] = v.toByte()
            }
        return out
    }
}
