package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import ru.rut.democamera.utils.GridSpacingItemDecoration
import java.io.File

class GalleryActivity : AppCompatActivity(), MediaGridAdapter.OnItemClickListener, NavBarFragment.NavBarListener {

    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mediaFiles: List<File>
    private lateinit var mediaAdapter: MediaGridAdapter

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
        val directory = File(externalMediaDirs.firstOrNull()?.absolutePath.orEmpty())
        mediaFiles = directory.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.toList()
            ?: emptyList()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = GridLayoutManager(this,3)
        binding.recyclerView.addItemDecoration(GridSpacingItemDecoration(3,16))
        mediaAdapter = MediaGridAdapter(mediaFiles, this)
        binding.recyclerView.adapter = mediaAdapter
    }


    private fun setupNavBar() {
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this, R.id.galleryBtn))
            .commit()
    }

    private fun displayItemCount() {
        val count = mediaAdapter.itemCount
        binding.fileCountText.text = if (count > 0) {
            "$count files"
        } else {
            "No files yet"
        }
    }

    override fun onItemClick(position: Int) {
        Intent(this, FullScreenActivity::class.java).apply {
            putExtra("current_index", position)
            startActivity(this)
        }
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
        Intent(this, activityClass).apply {
            startActivity(this)
        }
    }
}
