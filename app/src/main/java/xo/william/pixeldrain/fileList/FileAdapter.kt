package xo.william.pixeldrain.fileList

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xo.william.pixeldrain.FileViewActivity
import xo.william.pixeldrain.databinding.FileItemViewBinding
import xo.william.pixeldrain.model.FileViewModel
import xo.william.pixeldrain.repository.ClipBoard

class FileAdapter(private var context: Context, private var fileViewModel: FileViewModel) :
    RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var loadedFiles = listOf<InfoModel>()
    private var loadedFilesHolder = listOf<InfoModel>()
    private var expandedPosition = -1
    private val clipBoard: ClipBoard = ClipBoard(context)
    private val format = Json { ignoreUnknownKeys = true }

    class FileViewHolder(val binding: FileItemViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = FileItemViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val infoModel: InfoModel = loadedFiles[position]
        
        with(holder.binding) {
            loadImage(infoModel, holder)
            nameTextView.text = infoModel.name
            fileTypeTextView.text = infoModel.mime_type
            viewsTextview.text = "${infoModel.views} views"

            // Basic date formatting
            val formattedDate = infoModel.date_upload.substring(0, 16).replace("T", " ")
            uploadDateTextView.text = formattedDate

            setDetailVisibility(holder, position)
            handleExpand(holder, position)
            setOnClickListeners(holder, infoModel)
        }
    }

    private fun loadImage(infoModel: InfoModel, holder: FileViewHolder) {
        with(holder.binding) {
            try {
                val urlString = infoModel.getThumbnailUrl()
                Glide.with(holder.itemView.context)
                    .load(urlString)
                    .fitCenter()
                    .into(fileThumbnail)
                fileThumbnailLoader.visibility = View.GONE
            } catch (e: Exception) {
                fileThumbnailLoader.visibility = View.GONE
            }
        }
    }

    private fun setDetailVisibility(holder: FileViewHolder, position: Int) {
        val isExpanded = expandedPosition == position
        holder.binding.detailItemLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
    }

    private fun handleExpand(holder: FileViewHolder, position: Int) {
        val isExpanded = position == expandedPosition
        
        holder.binding.mainItemLayout.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(position)
        }
    }

    private fun setOnClickListeners(holder: FileViewHolder, infoModel: InfoModel) {
        with(holder.binding) {
            fileThumbnail.setOnClickListener {
                val mimeType = infoModel.mime_type
                if (mimeType.contains("image") ||
                    mimeType.contains("text") ||
                    mimeType.contains("video") ||
                    mimeType.contains("audio")
                ) {
                    val intent = Intent(context, FileViewActivity::class.java)
                    intent.putExtra("infoModel", format.encodeToString(infoModel))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(holder.itemView.context, "This file type is not supported", Toast.LENGTH_SHORT).show()
                }
            }

            downloadButton.setOnClickListener {
                downloadFile(infoModel)
            }

            copyButton.setOnClickListener {
                clipBoard.copyToClipBoard(infoModel.getShareUrl())
            }

            shareButton.setOnClickListener {
                shareUrl(infoModel)
            }
            
            deleteButton.setOnClickListener {
                openDeleteFileAlert(infoModel)
            }
        }
    }

    private fun downloadFile(infoModel: InfoModel) {
        val url = "${infoModel.getFileUrl()}?download"
        val uris = Uri.parse(url)
        val intents = Intent(Intent.ACTION_VIEW, uris)
        val b = Bundle()
        b.putBoolean("new_window", true)
        intents.putExtras(b)
        context.startActivity(intents)
    }

    private fun shareUrl(infoModel: InfoModel) {
        val text = "Check this file out on PixelDrain: ${infoModel.getShareUrl()}"
        val intent = Intent()
            .setType("text/plain")
            .setAction(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, text)

        val shareIntent = Intent.createChooser(intent, "Share ${infoModel.name}")
        context.startActivity(shareIntent)
    }

    private fun openDeleteFileAlert(infoModel: InfoModel) {
        val builder = AlertDialog.Builder(context)
        val message: String = if (infoModel.can_edit) {
            "After deleting this file no one will be able to see it."
        } else {
            "Warning this file was uploaded anonymous. " +
            "This will only remove this file from the overview. " +
            "People with the link can still view the file."
        }

        builder.setTitle("Are you sure you want to delete ${infoModel.name}")
        builder.setMessage(message)
        builder.setPositiveButton("Confirm") { _, _ ->
            fileViewModel.deleteFile(infoModel) { msg ->
                showToast(msg)
            }
        }
        builder.setNeutralButton("Cancel") { _, _ -> }
        builder.create().show()
    }

    internal fun setFiles(files: List<InfoModel>) {
        val oldList = loadedFiles
        val newList = files.sortedByDescending { it.date_upload }
        
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        })
        
        loadedFiles = newList
        loadedFilesHolder = newList
        diffResult.dispatchUpdatesTo(this)
    }

    fun searchFiles(query: String?) {
        val oldList = loadedFiles
        val newList = if (query != null) {
            loadedFilesHolder.filter { it.name.contains(query, ignoreCase = true) }
        } else {
            loadedFilesHolder
        }
        
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldList.size
            override fun getNewListSize(): Int = newList.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].id == newList[newItemPosition].id
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        })
        
        loadedFiles = newList
        diffResult.dispatchUpdatesTo(this)
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = loadedFiles.size
}