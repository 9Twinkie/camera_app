package ru.rut.democamera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ItemMediaGridBinding
import java.io.File
import java.util.*

class MediaGridAdapter(
    private val files: List<File>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MediaGridAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    class ViewHolder(val binding: ItemMediaGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        bindMediaData(holder, file)
        holder.itemView.setOnClickListener { listener.onItemClick(position) }
    }

    private fun bindMediaData(holder: ViewHolder, file: File) {
        Glide.with(holder.binding.root)
            .load(file)
            .centerCrop()
            .into(holder.binding.mediaThumbnail)

        when (file.extension.lowercase(Locale.getDefault())) {
            in listOf("jpg", "jpeg", "png") -> {
                holder.binding.mediaType.visibility = View.GONE
                holder.binding.videoIcon.visibility = View.GONE
            }
            in listOf("mp4", "mov") -> {
                holder.binding.mediaType.visibility = View.GONE
                holder.binding.videoIcon.visibility = View.VISIBLE
            }
        }
    }
}
