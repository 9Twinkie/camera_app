package ru.rut.democamera

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ItemMediaGridBinding
import java.io.File
import java.text.SimpleDateFormat
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

        Glide.with(holder.binding.root)
            .load(file)
            .centerCrop()
            .into(holder.binding.mediaThumbnail)

        val extension = file.extension.lowercase(Locale.getDefault())
        val type = when (extension) {
            in listOf("jpg", "jpeg", "png") -> "Photo"
            in listOf("mp4", "mov") -> "Video"
            else -> "Unknown"
        }
        holder.binding.mediaType.text = type

        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(file.lastModified()))
        holder.binding.mediaDate.text = dateStr

        holder.itemView.setOnClickListener {
            listener.onItemClick(position)
        }
    }
}
