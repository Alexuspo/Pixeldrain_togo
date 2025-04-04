package xo.william.pixeldrain

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.core.requests.tryCancel
import com.github.kittinunf.result.Result
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xo.william.pixeldrain.api.FuelService
import xo.william.pixeldrain.databinding.ActivityFileViewBinding
import xo.william.pixeldrain.fileList.InfoModel

class FileViewActivity : AppCompatActivity() {

    private val format = Json { ignoreUnknownKeys = true }
    private lateinit var infoModel: InfoModel
    private lateinit var binding: ActivityFileViewBinding
    private lateinit var request: CancellableRequest

    private var textLiveData = MutableLiveData<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityFileViewBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setSupportActionBar(binding.subToolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
            }

            val infoModelString: String? = intent.getStringExtra("infoModel")
            if (infoModelString != null) {
                infoModel = format.decodeFromString(infoModelString)
            } else {
                infoModel = InfoModel("")
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            }
            
            // Registrujeme vlastní callback pro tlačítko zpět
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    cleanup()
                    finish()
                }
            })

            loadFile()
        } catch (e: Exception) {
            Toast.makeText(this, "Error during initialization: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("FileViewActivity", "Error during initialization", e)
            finish()
        }
    }

    private fun loadFile() {
        try {
            val type = infoModel.mime_type
            when {
                type.contains("image") -> loadImage()
                type.contains("video") || type.contains("audio") -> {
                    // Dočasně zobrazíme pouze informaci o videu místo přehrávání
                    binding.fileProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Video/audio playback temporarily disabled", Toast.LENGTH_LONG).show()
                }
                type.contains("text") -> loadText()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading file: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("FileViewActivity", "Error loading file", e)
        }
    }

    private fun loadImage() {
        try {
            val urlString = infoModel.getFileUrl()
            binding.imageFile.visibility = View.VISIBLE
            binding.imageFile.contentDescription = infoModel.name
            Glide.with(this).load(urlString).fitCenter().into(binding.imageFile)
            binding.fileProgressBar.visibility = View.GONE
        } catch (e: Exception) {
            binding.fileProgressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("FileViewActivity", "Error loading image", e)
        }
    }

    private fun loadText() {
        try {
            request = FuelService().getFileText(infoModel.getFileUrl())
                .responseString { _, _, result -> 
                    when (result) {
                        is Result.Success -> {
                            textLiveData.postValue(result.get())
                        }
                        is Result.Failure -> {
                            textLiveData.postValue(result.error.exception.message)
                        }
                    }
                }

            textLiveData.observe(this) {
                binding.fileProgressBar.visibility = View.GONE
                binding.textFile.text = it
                binding.textFile.visibility = View.VISIBLE
                binding.textScrollView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            binding.fileProgressBar.visibility = View.GONE
            Toast.makeText(this, "Error loading text: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("FileViewActivity", "Error loading text", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            cleanup()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun cleanup() {
        if (::request.isInitialized) {
            request.tryCancel()
        }
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }
}