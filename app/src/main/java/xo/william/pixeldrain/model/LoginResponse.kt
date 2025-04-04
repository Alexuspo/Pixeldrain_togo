package xo.william.pixeldrain.model

import kotlinx.serialization.*

@Serializable
data class LoginResponse(
    var success: Boolean = false,
    var message: String = "",
    var api_key: String = ""
) {
    // Zpětná kompatibilita se starým kódem
    var authKey: String = ""
    var succes: Boolean = false
}