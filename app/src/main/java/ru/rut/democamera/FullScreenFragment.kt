package ru.rut.democamera

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.FragmentFullScreenBinding
import java.io.File

class FullScreenFragment : Fragment() {

    private var _binding: FragmentFullScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var files: MutableList<File>
    private var currentIndex = 0
    private lateinit var adapter: FullScreenAdapter

    companion object {
        fun newInstance(index: Int): FullScreenFragment {
            val fragment = FullScreenFragment()
            val args = Bundle()
            args.putInt("current_index", index)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera app"
        )
        files = (directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() }
            ?: emptyList()).toMutableList()

        currentIndex = arguments?.getInt("current_index", 0) ?: 0

        if (currentIndex >= files.size) currentIndex = files.size - 1
        if (currentIndex < 0) currentIndex = 0

        adapter = FullScreenAdapter(files)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentIndex, false)

        binding.deleteBtn.setOnClickListener {
            if (files.isNotEmpty()) {
                val position = binding.viewPager.currentItem
                val fileToDelete = files[position]
                if (fileToDelete.delete()) {
                    Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show()
                    files.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    if (files.isEmpty()) {
                        requireActivity().onBackPressed()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error deleting file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}