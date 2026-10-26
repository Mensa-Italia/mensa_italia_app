package it.mensa.shared.net

import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkErrorsTest {
    @Test
    fun noConnectionIsUnavailable() {
        assertTrue(NetworkErrors.isUnavailable(UnknownHostException("Unable to resolve host \"svc.mensa.it\"")))
        assertTrue(NetworkErrors.isUnavailable(ConnectException("Failed to connect")))
        assertTrue(NetworkErrors.isUnavailable(SocketTimeoutException("timeout")))
    }

    @Test
    fun wrappedStillCounts() {
        assertTrue(NetworkErrors.isUnavailable(IllegalStateException("refresh failed", UnknownHostException())))
    }

    /** Errori del server o dei dati: restano errori veri, da mostrare. */
    @Test
    fun otherFailuresAreNot() {
        assertFalse(NetworkErrors.isUnavailable(SerializationException("bad json")))
        assertFalse(NetworkErrors.isUnavailable(IllegalStateException("HTTP 500")))
    }
}
