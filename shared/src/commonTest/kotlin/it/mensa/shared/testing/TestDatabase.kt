package it.mensa.shared.testing

import it.mensa.shared.db.MensaDatabase

/**
 * Un [MensaDatabase] nuovo, in memoria e con lo schema gia' creato, per i test
 * comuni: JDBC su Android/JVM, il driver nativo su iOS.
 */
expect suspend fun newTestDatabase(): MensaDatabase
