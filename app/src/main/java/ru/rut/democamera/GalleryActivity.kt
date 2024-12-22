package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File
class GalleryActivity : AppCompatActivity(), MediaGridAdapter.OnItemClickListener, NavBarFragment.NavBarListener {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var mediaFiles: List<File>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val directory = File(externalMediaDirs[0].absolutePath)
        val files = directory.listFiles()?.toList()?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        mediaFiles = files

        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        val adapter = MediaGridAdapter(mediaFiles, this)
        binding.recyclerView.adapter = adapter
        supportFragmentManager.beginTransaction()
            .replace(binding.navbarContainer.id, NavBarFragment(this))
            .commit()
    }

    override fun onItemClick(position: Int) {
        val intent = Intent(this, FullScreenActivity::class.java)
        intent.putExtra("current_index", position)
        startActivity(intent)
    }

    override fun onGallerySelected() {

    }

    override fun onPhotoModeSelected() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onVideoModeSelected() {
        val intent = Intent(this, VideoActivity::class.java)
        startActivity(intent)
    }
}
