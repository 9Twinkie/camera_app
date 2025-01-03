package ru.rut.democamera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.FragmentNavBarBinding

class NavBarFragment(
    private val listener: NavBarListener,
    private val selectedButtonId: Int
) : Fragment() {

    interface NavBarListener {
        fun onGallerySelected()
        fun onPhotoModeSelected()
        fun onVideoModeSelected()
    }

    private var _binding: FragmentNavBarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNavBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        highlightSelectedButton()

        binding.galleryBtn.setOnClickListener { listener.onGallerySelected() }
        binding.photoBtn.setOnClickListener { listener.onPhotoModeSelected() }
        binding.videoBtn.setOnClickListener { listener.onVideoModeSelected() }
    }

    private fun highlightSelectedButton() {
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.purple_500)
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.purple_200)

        listOf(binding.galleryBtn, binding.photoBtn, binding.videoBtn).forEach { button ->
            button.setBackgroundColor(defaultColor)
        }

        when (selectedButtonId) {
            R.id.galleryBtn -> binding.galleryBtn.setBackgroundColor(selectedColor)
            R.id.photoBtn -> binding.photoBtn.setBackgroundColor(selectedColor)
            R.id.videoBtn -> binding.videoBtn.setBackgroundColor(selectedColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
