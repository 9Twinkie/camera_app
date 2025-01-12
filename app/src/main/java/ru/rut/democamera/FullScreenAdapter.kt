package ru.rut.democamera

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.MediaController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ItemFullscreenMediaBinding
import java.io.File
import java.util.*

class FullScreenAdapter(private var files: List<File>) :
    RecyclerView.Adapter<FullScreenAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFullscreenMediaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFullscreenMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        val extension = file.extension.lowercase(Locale.getDefault())

        if (extension in listOf("mp4")) {
            holder.binding.imageView.visibility = android.view.View.GONE
            holder.binding.videoView.visibility = android.view.View.VISIBLE
            holder.binding.videoView.setVideoURI(Uri.fromFile(file))
            val mediaController = MediaController(holder.binding.root.context)
            mediaController.setAnchorView(holder.binding.videoView)
            holder.binding.videoView.setMediaController(mediaController)
            holder.binding.videoView.start()
        } else {
            holder.binding.videoView.visibility = android.view.View.GONE
            holder.binding.imageView.visibility = android.view.View.VISIBLE
            Glide.with(holder.binding.root)
                .load(file)
                .fitCenter()
                .into(holder.binding.imageView)
        }
    }
}
