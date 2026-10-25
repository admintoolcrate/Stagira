package com.stagirite.ledger.sync

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DriveException(val code: Int, message: String) : IOException(message)

/**
 * Minimal Google Drive v3 client for the app data folder: a hidden, per-app
 * folder in the user's own Drive. With the drive.appdata scope the app can see
 * nothing else in their Drive, and nothing else can see this folder.
 * Blocking calls; run them off the main thread.
 */
class DriveClient(private val accessToken: String) {

    /** The id of our ledger file, or null if there isn't one yet. */
    fun findFile(): String? {
        val q = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val body = request("GET", "$API/files?spaces=appDataFolder&fields=files(id)&q=$q")
        val files = JSONObject(body).getJSONArray("files")
        return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    fun download(id: String): String = request("GET", "$API/files/$id?alt=media")

    fun create(content: String) {
        val boundary = "ledger-boundary"
        val metadata = JSONObject().put("name", FILE_NAME).put("parents", JSONArray().put("appDataFolder"))
        val body = "--$boundary\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n\r\n$metadata\r\n" +
            "--$boundary\r\n" +
            "Content-Type: application/json; charset=UTF-8\r\n\r\n$content\r\n" +
            "--$boundary--"
        request("POST", "$UPLOAD/files?uploadType=multipart", body, "multipart/related; boundary=$boundary")
    }

    fun update(id: String, content: String) {
        // HttpURLConnection can't send PATCH; Google APIs accept this override instead.
        request(
            "POST", "$UPLOAD/files/$id?uploadType=media", content, "application/json; charset=UTF-8",
            mapOf("X-HTTP-Method-Override" to "PATCH"),
        )
    }

    fun delete(id: String) {
        request("DELETE", "$API/files/$id")
    }

    /** The signed-in account's email, shown so the user knows where their data is. */
    fun email(): String? = runCatching {
        JSONObject(request("GET", "$API/about?fields=user(emailAddress)"))
            .getJSONObject("user").optString("emailAddress").ifEmpty { null }
    }.getOrNull()

    private fun request(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw DriveException(code, "Google Drive returned $code")
            return text
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val FILE_NAME = "ledger.json"

        /** Tells Google to drop this app's access to the account. Best effort. */
        fun revoke(token: String) {
            runCatching {
                val conn = URL("https://oauth2.googleapis.com/revoke").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use { it.write("token=${URLEncoder.encode(token, "UTF-8")}".toByteArray()) }
                conn.responseCode
                conn.disconnect()
            }
        }
    }
}
