package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import io.ktor.client.request.headers

/**
 * A multipart file part. Used by [PocketBaseClient.createMultipart] / [PocketBaseClient.updateMultipart]
 * to send file uploads (e.g. event cover image) alongside text fields.
 */
data class FilePart(
    val name: String,           // form field name (e.g. "image")
    val filename: String,
    val contentType: String,    // e.g. "image/jpeg"
    val bytes: ByteArray,
)

@Serializable
data class PbListResponse<T>(
    val page: Int = 1,
    val perPage: Int = 30,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val items: List<T> = emptyList()
)

class PocketBaseClient(@PublishedApi internal val client: HttpClient) {

    suspend inline fun <reified T> list(
        collection: String,
        page: Int = 1,
        perPage: Int = 50,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null
    ): PbListResponse<T> = client.get("/api/collections/$collection/records") {
        parameter("page", page)
        parameter("perPage", perPage)
        filter?.let { parameter("filter", it) }
        sort?.let { parameter("sort", it) }
        expand?.let { parameter("expand", it) }
    }.body()

    suspend inline fun <reified T> getOne(
        collection: String,
        id: String,
        expand: String? = null
    ): T = client.get("/api/collections/$collection/records/$id") {
        expand?.let { parameter("expand", it) }
    }.body()

    suspend inline fun <reified T, reified B> create(collection: String, body: B): T =
        client.post("/api/collections/$collection/records") {
            setBody(body)
        }.body()

    suspend inline fun <reified T, reified B> update(collection: String, id: String, body: B): T =
        client.patch("/api/collections/$collection/records/$id") {
            setBody(body)
        }.body()

    suspend fun delete(collection: String, id: String) {
        client.delete("/api/collections/$collection/records/$id")
    }

    @PublishedApi
    internal fun buildMultipartBody(
        fields: Map<String, Any?>,
        files: List<FilePart>,
    ): MultiPartFormDataContent = MultiPartFormDataContent(
        formData {
            fields.forEach { (k, v) ->
                if (v != null) append(k, v.toString())
            }
            files.forEach { f ->
                append(
                    key = f.name,
                    value = f.bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, f.contentType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${f.filename}\"")
                    }
                )
            }
        }
    )

    suspend inline fun <reified T> createMultipart(
        collection: String,
        fields: Map<String, Any?>,
        files: List<FilePart> = emptyList(),
    ): T = client.post("/api/collections/$collection/records") {
        setBody(buildMultipartBody(fields, files))
    }.body()

    suspend inline fun <reified T> updateMultipart(
        collection: String,
        id: String,
        fields: Map<String, Any?>,
        files: List<FilePart> = emptyList(),
    ): T = client.patch("/api/collections/$collection/records/$id") {
        setBody(buildMultipartBody(fields, files))
    }.body()

    suspend inline fun <reified T> fullList(
        collection: String,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null,
        batch: Int = 200
    ): List<T> {
        val out = mutableListOf<T>()
        var page = 1
        while (true) {
            val resp = list<T>(collection, page, batch, filter, sort, expand)
            out += resp.items
            if (page >= resp.totalPages || resp.items.isEmpty()) break
            page++
        }
        return out
    }

    suspend inline fun <reified T> listUnauthenticated(
        collection: String,
        page: Int = 1,
        perPage: Int = 50,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null
    ): PbListResponse<T> = client.get("/api/collections/$collection/records") {
        headers { remove(HttpHeaders.Authorization) }
        parameter("page", page)
        parameter("perPage", perPage)
        filter?.let { parameter("filter", it) }
        sort?.let { parameter("sort", it) }
        expand?.let { parameter("expand", it) }
    }.body()

    suspend inline fun <reified T> fullListUnauthenticated(
        collection: String,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null,
        batch: Int = 200
    ): List<T> {
        val out = mutableListOf<T>()
        var page = 1
        while (true) {
            val resp = listUnauthenticated<T>(collection, page, batch, filter, sort, expand)
            out += resp.items
            if (page >= resp.totalPages || resp.items.isEmpty()) break
            page++
        }
        return out
    }
}
