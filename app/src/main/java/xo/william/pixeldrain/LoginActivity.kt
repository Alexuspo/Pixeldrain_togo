package xo.william.pixeldrain

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import xo.william.pixeldrain.databinding.ActivityLoginBinding
import xo.william.pixeldrain.model.LoginResponse
import xo.william.pixeldrain.model.LoginViewModel
import xo.william.pixeldrain.repository.SharedRepository

class LoginActivity : AppCompatActivity() {

    private var username = ""
    private var password = ""
    private lateinit var binding: ActivityLoginBinding
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var sharedRepository: SharedRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedRepository = SharedRepository(this)

        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.subToolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        binding.userInput.doOnTextChanged { text, _, _, _ ->
            username = text.toString()
            binding.loginButton.isEnabled = (username.isNotEmpty() && password.isNotEmpty())
        }

        binding.passwordInput.doOnTextChanged { text, _, _, _ ->
            password = text.toString()
            binding.loginButton.isEnabled = (username.isNotEmpty() && password.isNotEmpty())
        }

        binding.loginButton.setOnClickListener {
            binding.loginButton.isEnabled = false
            binding.loginProgress.visibility = View.VISIBLE

            loginViewModel.loginUser(username, password)
        }

        loginViewModel.loginResponse.observe(this) { response -> 
            handleLoginResponse(response) 
        }
    }

    private fun handleLoginResponse(response: LoginResponse) {
        binding.loginProgress.visibility = View.GONE
        if (response.auth_key.isNotEmpty()) {
            sharedRepository.saveToken(response.auth_key)
            setResult(200)
            finish()
        } else {
            binding.loginButton.isEnabled = true
            Toast.makeText(this, "Error: ${response.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}