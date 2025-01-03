package ru.rut.democamera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ItemMediaGridBinding
import java.io.File

class MediaGridAdapter(
    private val files: List<File>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<MediaGridAdapter.ViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    class ViewHolder(private val binding: ItemMediaGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: File, listener: OnItemClickListener) {
            Glide.with(binding.root)
                .load(file)
                .centerCrop()
                .into(binding.mediaThumbnail)


            binding.videoIcon.visibility = if (file.extension == "mp4") View.VISIBLE else View.GONE

            binding.root.setOnClickListener { listener.onItemClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = files.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position], listener)
    }
}
