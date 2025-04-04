package xo.william.pixeldrain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore.MediaColumns.DISPLAY_NAME
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import xo.william.pixeldrain.databinding.ActivityMainBinding
import xo.william.pixeldrain.fileList.FileAdapter
import xo.william.pixeldrain.model.FileViewModel
import xo.william.pixeldrain.repository.SharedRepository

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewAdapter: FileAdapter
    private lateinit var fileViewModel: FileViewModel
    private lateinit var sharedRepository: SharedRepository
    private lateinit var loginButtonRef: MenuItem
    private lateinit var registerButton: MenuItem
    
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var loginLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setupActivityResultLaunchers()
            
            sharedRepository = SharedRepository(this)

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setSupportActionBar(binding.mainToolbar)
            
            fileViewModel = ViewModelProvider(this)[FileViewModel::class.java]
            fileViewModel.setSharedResponse(sharedRepository)
            
            setRecyclerView()

            fileViewModel.loadedFiles.observe(this) { files ->
                files?.let {
                    if (files.isNotEmpty()) {
                        stopProgress(false)
                    }
                    viewAdapter.setFiles(files)
                }
            }

            fileViewModel.dbFiles.observe(this) { files ->
                files?.let {
                    if (it.isEmpty() && !sharedRepository.isUserLoggedIn()) {
                        stopProgress(true)
                    }
                    fileViewModel.loadFiles(it)
                }
            }

            binding.mainActionButton.setOnClickListener {
                handleActionButton()
            }
            
            try {
                fileViewModel.initializeData()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing data", e)
                Toast.makeText(this, "Error initializing data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during initialization", e)
            Toast.makeText(this, "Error during initialization: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupActivityResultLaunchers() {
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val selectedFile = result.data?.data
                startUpload(selectedFile)
            }
        }
        
        loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == 200) {
                loginButtonRef.title = "Logout"
                registerButton.isVisible = false
                fileViewModel.loadFilesFromApi(loadedFiles = fileViewModel.loadedFiles)
                Toast.makeText(this, "Logged in", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopProgress(empty: Boolean) {
        binding.initProgress.visibility = View.GONE
        binding.initText.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun setRecyclerView() {
        val viewManager = LinearLayoutManager(this)
        viewAdapter = FileAdapter(this, fileViewModel)

        binding.fileRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun handleActionButton() {
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        filePickerLauncher.launch(Intent.createChooser(intent, "Select a file"))
    }

    private fun startUpload(selectedFile: Uri?) {
        if (selectedFile != null) {
            var fileName = selectedFile.path

            binding.mainProgress.visibility = View.VISIBLE

            val filePathColumn = arrayOf(DISPLAY_NAME)
            val cursor = contentResolver.query(selectedFile, filePathColumn, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                fileName = cursor.getString(0)
                cursor.close()
            }
            val stream = contentResolver.openInputStream(selectedFile)

            if (sharedRepository.isUserLoggedIn()) {
                fileViewModel.uploadPost(stream, fileName, sharedRepository.getAuthKey(), this::finishUpload)
            } else {
                fileViewModel.uploadAnonPost(stream, fileName, this::finishUpload)
            }
        }
    }

    private fun finishUpload(message: String?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        binding.mainProgress.visibility = View.INVISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_toolbar, menu)

        loginButtonRef = menu.findItem(R.id.action_login)
        registerButton = menu.findItem(R.id.action_register)
        
        if (sharedRepository.isUserLoggedIn()) {
            loginButtonRef.title = "Logout"
            registerButton.isVisible = false
        } else {
            loginButtonRef.title = "Login"
            registerButton.isVisible = true
        }

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewAdapter.searchFiles(query)
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        searchView.setOnCloseListener {
            viewAdapter.searchFiles(null)
            false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_login -> {
            if (sharedRepository.isUserLoggedIn()) {
                sharedRepository.deleteToken()
                item.title = "Login"
                registerButton.isVisible = true
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            } else {
                openLoginActivity()
            }
            true
        }
        
        R.id.action_register -> {
            openNewTabWindow()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun openLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        loginLauncher.launch(intent)
    }

    private fun openNewTabWindow() {
        val uris = Uri.parse("https://pixeldrain.com/register")
        val intents = Intent(Intent.ACTION_VIEW, uris)
        val b = Bundle()
        b.putBoolean("new_window", true)
        intents.putExtras(b)
        this.startActivity(intents)
    }
}