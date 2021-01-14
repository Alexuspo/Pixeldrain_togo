package xo.william.pixeldrain.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xo.william.pixeldrain.database.AppDatabase
import xo.william.pixeldrain.database.File
import xo.william.pixeldrain.database.FileDao
import xo.william.pixeldrain.fileList.FileModel
import xo.william.pixeldrain.fileList.InfoModel
import xo.william.pixeldrain.repository.FileRepository
import java.io.InputStream

class FileViewModel(application: Application) : AndroidViewModel(application) {

    private val format = Json { ignoreUnknownKeys = true }
    private val repository: FileRepository

    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allFiles: LiveData<List<FileModel>>
    private val fileDao: FileDao = AppDatabase.getDatabase(application, viewModelScope).fileDao()

    private var _infoFiles: MutableLiveData<MutableList<InfoModel>> = MutableLiveData();
    val infoFiles: LiveData<MutableList<InfoModel>>
    init {
        repository = FileRepository(fileDao)
        allFiles = repository.allFiles
        infoFiles = this.setInfoFiles();
    }

    private fun setInfoFiles(): LiveData<MutableList<InfoModel>> {
        return Transformations.switchMap(allFiles) { files ->
            repository.setInfoFiles(_infoFiles, files);
        }
    }


    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun uploadAnonPost(stream: InputStream?, fileName: String?, callback: ((String) -> Unit)) =
        viewModelScope.launch(Dispatchers.IO) {
            if (stream !== null) {
                repository.uploadAnonPost(stream, fileName)
                    .responseString { request, response, result ->
                        when (result) {
                            is Result.Success -> {
                                val data = result.get()
                                Log.d("response", "data: " + data);
                                val file = format.decodeFromString<InfoModel>(data);
                                val dbFile = File(file.id, fileName + "", "mime", "asd", 666)
                                insert(dbFile);
                                callback("Succes: " + file.id + " added");
                            }

                            is Result.Failure -> {
                                val ex = result.getException()
                                callback("Something went wrong: " + ex.message);
                            }
                        }
                    }
            }

        }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun uploadPost(stream: InputStream?, fileName: String?, authKey: String, callback: ((String) -> Unit)) =
        viewModelScope.launch(Dispatchers.IO) {
            if (stream !== null) {
                repository.uploadPost(stream, fileName, authKey)
                    .interrupt { Log.d("response", "interupted")  }
                    .responseString() { request, response, result ->
                        when (result) {
                            is Result.Success -> {
                                val data = result.get()
                                Log.d("response", "data: " + data);
                                Log.d("response", "code: ${response.statusCode}")
                                val file = format.decodeFromString<InfoModel>(data);
                                callback("Succes: " + file.id + " added");
                            }

                            is Result.Failure -> {
                                val ex = result.getException()
                                Log.d("response", "error code: ${response.statusCode}")
                                Log.d("response", "error: ${ ex.message}")
                                if (response.statusCode == 401){
                                    // TODO Handle unauthorized
                                }

                                callback("Something went wrong: " + ex.message);
                            }
                        }
                    }
            }

        }
    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(file: File) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(file)
    }
}