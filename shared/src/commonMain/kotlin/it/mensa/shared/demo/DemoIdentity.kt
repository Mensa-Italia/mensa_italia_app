package it.mensa.shared.demo

import it.mensa.shared.model.UserModel

/**
 * Sostituisce i dati identificativi del socio loggato con un segnaposto.
 *
 * Serve alla cattura degli screenshot per gli store (`tools/storekit`): le
 * immagini finiscono pubbliche e indicizzate, e senza questo filtro
 * pubblicherebbero nome, foto e numero tessera di chi ha fatto il login.
 *
 * Il filtro si applica in [it.mensa.shared.auth.AuthRepository] sul valore
 * esposto da `currentUser`, quindi copre in un colpo solo ogni schermata che
 * mostra il socio: saluto di Today, tessera, profilo, su iOS e Android.
 * Il record vero resta intatto nel DB locale e sul backend: qui si tocca solo
 * cio' che viene mostrato.
 *
 * Va attivato dalle harness di automazione, che girano solo su build di debug
 * (`MENSA_DEMO_IDENTITY=1` su iOS, extra `mensa_demo_identity` su Android).
 * Di default e' spento e `redact` restituisce l'utente invariato.
 */
object DemoIdentity {

    /** True solo quando una harness di debug lo accende. */
    var enabled: Boolean = false

    /**
     * Campi sostituiti. Restano fuori `expireMembership`, `powers`, `addons`,
     * `isMembershipActive` e `created`: non identificano nessuno e pilotano
     * quali sezioni la UI mostra, quindi azzerarli darebbe screenshot di
     * un'app piu' vuota di quella vera.
     *
     * Anche `id` e `username` vanno sostituiti, non solo il nome: la tessera
     * ci costruisce sopra il QR (`MENSA-IT|id:…|user:…`), che altrimenti
     * finirebbe sugli store in forma scansionabile.
     */
    fun redact(user: UserModel?): UserModel? {
        if (!enabled || user == null) return user
        return user.copy(
            id = PLACEHOLDER_ID,
            username = PLACEHOLDER_USERNAME,
            name = PLACEHOLDER_NAME,
            // Vuoto: la UI ricade sull'avatar generico invece di scaricare
            // la foto vera. Vedi `CardViewModel.avatarURL`.
            avatar = "",
            email = PLACEHOLDER_EMAIL,
        )
    }

    private const val PLACEHOLDER_ID = "demo000000000000"
    private const val PLACEHOLDER_USERNAME = "socio.demo"
    private const val PLACEHOLDER_NAME = "Giulia Bianchi"
    private const val PLACEHOLDER_EMAIL = "socio.demo@mensa.it"
}
