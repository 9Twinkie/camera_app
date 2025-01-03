package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import ru.rut.democamera.utils.GridSpacingItemDecoration
import java.io.File

class GalleryActivity : AppCompatActivity(), MediaGridAdapter.OnItemClickListener,
    NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mediaFiles: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMediaFiles()
        setupRecyclerView()
        setupNavBar()
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
            layoutManager = GridLayoutManager(this@GalleryActivity, 3)
            addItemDecoration(GridSpacingItemDecoration(3, 16))
            adapter = MediaGridAdapter(mediaFiles, this@GalleryActivity)
        }
    }

    private fun setupNavBar() {
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.galleryBtn))
            .commit()
    }

    private fun displayItemCount() {
        binding.fileCountText.text = when (val count = mediaFiles.size) {
            0 -> "No files yet"
            1 -> "1 file"
            else -> "$count files"
        }
    }

    override fun onItemClick(position: Int) {
        startActivity(Intent(this, FullScreenActivity::class.java).apply {
            putExtra("current_index", position)
        })
    }

    override fun onGallerySelected() {
    }

    override fun onPhotoModeSelected() {
        navigateTo(MainActivity::class.java)
    }

    override fun onVideoModeSelected() {
        navigateTo(VideoActivity::class.java)
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }
}
