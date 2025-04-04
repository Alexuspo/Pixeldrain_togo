package xo.william.pixeldrain.model

import android.app.Application
import androidx.lifecycle.*
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xo.william.pixeldrain.database.AppDatabase
import xo.william.pixeldrain.database.File
import xo.william.pixeldrain.database.FileDao
import xo.william.pixeldrain.fileList.InfoModel
import xo.william.pixeldrain.repository.FileRepository
import xo.william.pixeldrain.repository.SharedRepository
import java.io.InputStream

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val format = Json { ignoreUnknownKeys = true }
    private val repository: FileRepository
    private var sharedRepository: SharedRepository = SharedRepository(application)
    private val fileDao: FileDao = AppDatabase.getDatabase(application).fileDao()

    val loadedFiles: MutableLiveData<MutableList<InfoModel>> =
        MutableLiveData(mutableListOf<InfoModel>())
    val dbFiles: LiveData<List<File>>

    init {
        repository = FileRepository(fileDao)
        dbFiles = repository.getDatabaseFiles()
    }

    fun initializeData() {
        loadFilesFromApi(loadedFiles)
    }

    fun loadFiles(list: List<File>) = viewModelScope.launch(Dispatchers.IO) {
        list.forEach {
            repository.loadFileInfo(it, loadedFiles)
        }
    }

    fun loadFilesFromApi(loadedFiles: MutableLiveData<MutableList<InfoModel>>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (sharedRepository.isUserLoggedIn()) {
                repository.loadApiFiles(loadedFiles, sharedRepository.getAuthKey())
            }
        }

    fun setSharedResponse(sharedRepository: SharedRepository) {
        this.sharedRepository = sharedRepository
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun uploadAnonPost(stream: InputStream?, fileName: String?, callback: ((String) -> Unit)) =
        viewModelScope.launch(Dispatchers.IO) {
            if (stream !== null) {
                repository.uploadAnonPost(stream, fileName)
                    .responseString { _, _, result ->
                        when (result) {
                            is Result.Success -> {
                                val file = format.decodeFromString<InfoModel>(result.get())
                                insert(File(file.id))
                                callback("Succes: " + file.id + " added")
                            }
                            is Result.Failure -> {
                                val ex = result.getException()
                                callback("Something went wrong: " + ex.message)
                            }
                        }
                    }
            }
        }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun uploadPost(
        stream: InputStream?,
        fileName: String?,
        authKey: String,
        callback: ((String) -> Unit),
    ) =
        viewModelScope.launch(Dispatchers.IO) {
            if (stream !== null) {
                repository.uploadPost(stream, fileName, authKey)
                    .responseString { _, _, result ->
                        when (result) {
                            is Result.Success -> {
                                val file = format.decodeFromString<InfoModel>(result.get())
                                //add loaded file to loadedFiles list
                                repository.loadFileInfo(File(file.id), loadedFiles)
                                callback("Succes: " + file.id + " added")
                            }
                            is Result.Failure -> {
                                val ex = result.getException()
                                callback("Something went wrong: " + ex.message)
                            }
                        }
                    }
            }
        }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    private fun insert(file: File) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(file)
    }

    fun deleteFile(infoModel: InfoModel, callback: (String) -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFromDb(infoModel.id)
            if (infoModel.can_edit && sharedRepository.isUserLoggedIn()) {
                repository.deleteFromApi(infoModel.id, sharedRepository.getAuthKey())
                    .response { result ->
                        when (result) {
                            is Result.Success -> {
                                repository.deleteFromLoadedFiles(infoModel.id, loadedFiles)
                                callback("Deleted ${infoModel.name}")
                            }
                            is Result.Failure -> {
                                val ex = result.getException()
                                callback("Error: ${ex.exception.message}")
                            }
                        }
                    }
            } else {
                repository.deleteFromLoadedFiles(infoModel.id, loadedFiles)
                callback("Deleted ${infoModel.name}")
            }
        }
}