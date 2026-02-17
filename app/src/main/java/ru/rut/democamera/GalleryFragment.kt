package ru.rut.democamera

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.FragmentGalleryBinding
import ru.rut.democamera.utils.GridSpacingItemDecoration
import java.io.File

class GalleryFragment : Fragment(), MediaGridAdapter.OnItemClickListener {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var mediaFiles: List<File>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMediaFiles()
        setupRecyclerView()
        displayItemCount()
    }

    private fun setupMediaFiles() {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera app"
        )
        mediaFiles = directory.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.toList()
            ?: emptyList()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            addItemDecoration(GridSpacingItemDecoration(3, 16))
            adapter = MediaGridAdapter(mediaFiles, this@GalleryFragment)
        }
    }

    private fun displayItemCount() {
        binding.fileCountText.text = when (val count = mediaFiles.size) {
            0 -> "No files yet"
            1 -> "1 file"
            else -> "$count files"
        }
    }

    override fun onItemClick(position: Int) {
        (activity as? MainActivity)?.showFullScreenFragment(position)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}