package it.mensa.shared.integrity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalStdlibApi::class)
class IntegrityProofTest {

    // ── SHA-256 ──────────────────────────────────────────────────────────────

    @Test
    fun sha256MatchesStandardVectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.digest(ByteArray(0)).toHexString(),
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.digest("abc".encodeToByteArray()).toHexString(),
        )
        // 56 byte: costringe il padding a spillare in un secondo blocco.
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.digest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).toHexString(),
        )
        // 1000 'a': piu' blocchi.
        assertEquals(
            "41edece42d63e8d9bf515a9ba6932e1c20cbc9f5a5d134645adb5db1b9737ea3",
            Sha256.digest("a".repeat(1000).encodeToByteArray()).toHexString(),
        )
    }

    // ── clientData / requestHash ─────────────────────────────────────────────

    @Test
    fun clientDataIsChallengeMethodPathJoinedByPipes() {
        assertEquals(
            "abc|GET|/api/collections/stamp/records",
            IntegrityClientData.build("abc", "GET", "/api/collections/stamp/records"),
        )
        // Il metodo e' sempre maiuscolo.
        assertEquals("abc|POST|/x", IntegrityClientData.build("abc", "post", "/x"))
    }

    /** Vettore noto: `abc|GET|/api/collections/stamp/records` (calcolato con openssl). */
    @Test
    fun requestHashKnownVector() {
        val clientData = IntegrityClientData.build("abc", "GET", "/api/collections/stamp/records")
        assertEquals(
            "d748e0ff60778e77b80135c74c565d4bfa3f8c6dc7ad94ba98ec0826524c2f9a",
            IntegrityClientData.clientDataHash(clientData).toHexString(),
        )
        assertEquals("10jg_2B3jne4ATXHTFZdS_o_jG3HrZS6mOwIJlJML5o", IntegrityClientData.requestHash(clientData))
    }

    @Test
    fun requestHashIsBase64UrlWithoutPadding() {
        // SHA-256("abc") in base64 standard e' "ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=":
        // ha '+', '/' e il padding, che la forma url non deve avere.
        val hash = IntegrityClientData.requestHash("abc")
        assertEquals("ungWv48Bz-pBQUDeXa4iI7ADYaOWF3qctBD_YfIAFa0", hash)
        assertFalse(hash.contains('='))
        assertFalse(hash.contains('+'))
        assertFalse(hash.contains('/'))
    }

    @Test
    fun serverPathIsDecodedLikeGoUrlPath() {
        assertEquals("/api/collections/stamp/records", IntegrityClientData.serverPath("/api/collections/stamp/records"))
        assertEquals("/api/a b/x", IntegrityClientData.serverPath("/api/a%20b/x"))
        // Il '+' nel path non e' uno spazio.
        assertEquals("/api/a+b", IntegrityClientData.serverPath("/api/a+b"))
    }

    // ── Esenzioni ────────────────────────────────────────────────────────────

    @Test
    fun everyApiRouteIsCoveredByDefault() {
        assertFalse(IntegrityScope.isExempt("GET", "/api/collections/stamp/records"))
        assertFalse(IntegrityScope.isExempt("POST", "/api/collections/stamp_users/records"))
        assertFalse(IntegrityScope.isExempt("GET", "/api/collections/view_medals/records/abc123"))
        // Non piu' solo timbri e medaglie: tutte le collection e le route custom.
        assertFalse(IntegrityScope.isExempt("GET", "/api/collections/events/records"))
        assertFalse(IntegrityScope.isExempt("GET", "/api/cs/me"))
        assertFalse(IntegrityScope.isExempt("POST", "/api/batch"))
        assertFalse(IntegrityScope.isExempt("POST", "/api/realtime"))
    }

    @Test
    fun exemptionsAreExactlyTheContractList() {
        assertTrue(IntegrityScope.isExempt("POST", "/api/integrity/challenge"))
        assertTrue(IntegrityScope.isExempt("POST", "/api/integrity/ios/attest"))
        assertTrue(IntegrityScope.isExempt("GET", "/api/files/stamp/abc/img.png"))
        assertTrue(IntegrityScope.isExempt("GET", "/api/realtime"))
        assertTrue(IntegrityScope.isExempt("GET", "/api/health"))
    }

    @Test
    fun exemptionsDoNotLeak() {
        // Solo l'apertura dell'SSE e' esente, non le sottoscrizioni.
        assertFalse(IntegrityScope.isExempt("POST", "/api/realtime"))
        // Prefissi simili ma diversi.
        assertFalse(IntegrityScope.isExempt("POST", "/api/integrity/other"))
        assertFalse(IntegrityScope.isExempt("GET", "/api/filesystem"))
        assertFalse(IntegrityScope.isExempt("GET", "/api/collections/files/records"))
    }

    @Test
    fun nonApiRoutesAreOutOfScope() {
        assertTrue(IntegrityScope.isExempt("GET", "/ical/abc"))
        assertTrue(IntegrityScope.isExempt("GET", "/links/stamp/x"))
        assertTrue(IntegrityScope.isExempt("GET", "/.well-known/apple-app-site-association"))
    }

    // ── Allowlist dei bloccati ───────────────────────────────────────────────

    @Test
    fun blockedAllowlistCoversExactlyWhatTheServerKeepsServing() {
        assertTrue(BlockedAllowlist.allows("POST", "/api/collections/users_devices/records"))
        assertTrue(BlockedAllowlist.allows("PATCH", "/api/collections/users_devices/records/abc"))
        assertTrue(BlockedAllowlist.allows("GET", "/api/collections/ex_apps/records/abc"))
        assertTrue(BlockedAllowlist.allows("POST", "/api/collections/ex_granted_permissions/records"))
        assertTrue(BlockedAllowlist.allows("DELETE", "/api/collections/ex_granted_permissions/records/abc"))
        assertTrue(BlockedAllowlist.allows("POST", "/api/cs/exapp/export/decide"))
        assertTrue(BlockedAllowlist.allows("GET", "/api/collections/user_notifications/records"))
        assertTrue(BlockedAllowlist.allows("GET", "/api/collections/user_notifications/records/abc"))
        assertTrue(BlockedAllowlist.allows("PATCH", "/api/collections/user_notifications/records/abc"))
        assertTrue(BlockedAllowlist.allows("DELETE", "/api/collections/user_notifications/records/abc"))
        assertTrue(BlockedAllowlist.allows("GET", "/api/collections/users/records/abc"))
        assertTrue(BlockedAllowlist.allows("POST", "/api/collections/users/auth-refresh"))
    }

    @Test
    fun blockedAllowlistDoesNotCoverTheRest() {
        assertFalse(BlockedAllowlist.allows("GET", "/api/collections/events/records"))
        assertFalse(BlockedAllowlist.allows("GET", "/api/collections/stamp/records"))
        assertFalse(BlockedAllowlist.allows("POST", "/api/collections/ex_apps/records"), "ex_apps e' solo GET")
        assertFalse(BlockedAllowlist.allows("PATCH", "/api/collections/users/records/abc"))
        assertFalse(BlockedAllowlist.allows("GET", "/api/collections/users_metadata/records"))
        assertFalse(BlockedAllowlist.allows("POST", "/api/cs/exapp/export/other"))
        assertFalse(BlockedAllowlist.allows("POST", "/api/collections/user_notifications/records"))
        assertFalse(BlockedAllowlist.allows("PATCH", "/api/collections/user_notifications/records"))
        assertFalse(BlockedAllowlist.allows("GET", "/api/collections/user_notifications_settings/records"))
    }
}
