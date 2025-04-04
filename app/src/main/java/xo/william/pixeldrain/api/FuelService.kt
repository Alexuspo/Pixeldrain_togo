package xo.william.pixeldrain.api

import android.util.Base64
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.core.requests.UploadRequest
import com.github.kittinunf.fuel.core.requests.upload
import java.io.InputStream

class FuelService() {
    private val baseUri = "https://pixeldrain.com/api/"

    fun getFileInfoById(id: String): Request {
        val url = "${baseUri}file/${id}/info"
        return Fuel.get(url)
    }

    fun uploadAnonFile(selectedFile: InputStream, fileName: String?): UploadRequest {
        val url = baseUri + "file"
        val setFileName = if (fileName !== null) fileName else "file"

        // Použijeme základní upload bez použití lambda funkcí
        val request = Fuel.upload(url, method = Method.POST, parameters = listOf("name" to setFileName))
        request.dataParts = listOf(
            DataPart.from("file", selectedFile, setFileName, ContentType.OCTET)
        )
        return request
    }

    fun uploadFile(selectedFile: InputStream, fileName: String?, apiKey: String): UploadRequest {
        val url = baseUri + "file"
        val setFileName = if (fileName !== null) fileName else "file"
        
        // Používáme Basic Authentication podle dokumentace API
        val encodedAuth = Base64.encodeToString(":$apiKey".toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encodedAuth"

        // Vytvoříme request s autorizační hlavičkou
        val request = Fuel.upload(url, method = Method.POST, parameters = listOf("name" to setFileName))
            .header(Headers.AUTHORIZATION to authHeader)
        
        // Přidáme soubor jako DataPart
        request.dataParts = listOf(
            DataPart.from("file", selectedFile, setFileName, ContentType.OCTET)
        )
        
        return request
    }

    fun getFiles(apiKey: String): Request {
        val url = "${baseUri}user/files"
        
        // Používáme Basic Authentication podle dokumentace API
        val encodedAuth = Base64.encodeToString(":$apiKey".toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encodedAuth"

        return Fuel.get(url, parameters = listOf("page" to 0, "limit" to 1000))
            .header(Headers.AUTHORIZATION to authHeader)
    }

    fun getUserInfo(apiKey: String): Request {
        val url = "${baseUri}user"
        
        // Používáme Basic Authentication podle dokumentace API
        val encodedAuth = Base64.encodeToString(":$apiKey".toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encodedAuth"

        return Fuel.get(url)
            .header(Headers.AUTHORIZATION to authHeader)
    }

    // Přihlášení nyní vrací informace o uživateli, které obsahují API klíč
    fun loginUser(username: String, password: String): Request {
        val url ="${baseUri}user"
        
        // Používáme Basic Authentication s uživatelským jménem a heslem
        val encodedAuth = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encodedAuth"

        return Fuel.get(url)
            .header(Headers.AUTHORIZATION to authHeader)
    }

    fun deleteFile(id: String, apiKey: String): Request {
        val url ="${baseUri}file/${id}"
        
        // Používáme Basic Authentication podle dokumentace API
        val encodedAuth = Base64.encodeToString(":$apiKey".toByteArray(), Base64.NO_WRAP)
        val authHeader = "Basic $encodedAuth"

        return Fuel.delete(url)
            .header(Headers.AUTHORIZATION to authHeader)
    }

    fun getFileText(fileUrl: String): Request {
        return Fuel.get(fileUrl)
    }
}