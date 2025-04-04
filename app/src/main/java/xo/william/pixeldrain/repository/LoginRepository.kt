package xo.william.pixeldrain.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.github.kittinunf.result.Result
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xo.william.pixeldrain.api.FuelService
import xo.william.pixeldrain.model.LoginResponse

class LoginRepository() {
    private val fuelService = FuelService()
    private val format = Json { ignoreUnknownKeys = true }

    fun loginUser(
        username: String,
        password: String,
        loginResponse: MutableLiveData<LoginResponse>
    ) {
        fuelService.loginUser(username, password).responseString { _, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                    val loginResponseData = LoginResponse(
                        success = false,
                        message = "Error: ${ex.message}"
                    )
                    // Pro zpětnou kompatibilitu
                    loginResponseData.succes = false
                    
                    Log.e("LoginRepository", "Login failed: ${ex.message}")
                    loginResponse.postValue(loginResponseData)
                }
                is Result.Success -> {
                    try {
                        val data = result.get()
                        Log.d("LoginRepository", "Response received: $data")
                        
                        // Extrahujeme API klíč z odpovědi
                        val loginResponseData = format.decodeFromString<LoginResponse>(data)
                        
                        // Nastavíme hodnoty pro zpětnou kompatibilitu
                        loginResponseData.success = true
                        loginResponseData.succes = true
                        loginResponseData.authKey = loginResponseData.api_key
                        
                        Log.d("LoginRepository", "Login successful, API key: ${loginResponseData.api_key}")
                        loginResponse.postValue(loginResponseData)
                    } catch (e: Exception) {
                        Log.e("LoginRepository", "Error parsing response: ${e.message}")
                        val loginResponseData = LoginResponse(
                            success = false,
                            message = "Error parsing response: ${e.message}"
                        )
                        loginResponseData.succes = false
                        loginResponse.postValue(loginResponseData)
                    }
                }
            }
        }
    }
}