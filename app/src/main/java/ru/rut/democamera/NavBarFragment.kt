// NavBarFragment.kt
package ru.rut.democamera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.FragmentNavBarBinding

class NavBarFragment(private val listener: NavBarListener) : Fragment() {

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

        binding.galleryBtn.setOnClickListener {
            listener.onGallerySelected()
        }

        binding.photoBtn.setOnClickListener {
            listener.onPhotoModeSelected()
        }

        binding.videoBtn.setOnClickListener {
            listener.onVideoModeSelected()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
