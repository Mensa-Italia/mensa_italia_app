package it.mensa.shared.auth

interface ITokenStore {
    suspend fun save(token: String)
    suspend fun read(): String?
    suspend fun clear()
}
