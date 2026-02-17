package ru.rut.democamera

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavBarFragment.NavBarListener {
    private lateinit var binding: ActivityMainBinding
    private var currentFragmentTag: String = TAG_PHOTO

    companion object {
        const val TAG_PHOTO = "PHOTO"
        const val TAG_VIDEO = "VIDEO"
        const val TAG_GALLERY = "GALLERY"
        const val TAG_FULL_SCREEN = "FULL_SCREEN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ EDGE-TO-EDGE
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Отступ только для navbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.navbarContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.navbarContainer, NavBarFragment(this, R.id.photoBtn))
            .commit()

        if (savedInstanceState == null) {
            loadFragment(PhotoFragment(), TAG_PHOTO)
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        if (currentFragmentTag == tag && supportFragmentManager.findFragmentByTag(tag) != null) {
            return
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
        currentFragmentTag = tag
    }

    override fun onGallerySelected() {
        loadFragment(GalleryFragment(), TAG_GALLERY)
    }

    override fun onPhotoModeSelected() {
        loadFragment(PhotoFragment(), TAG_PHOTO)
    }

    override fun onVideoModeSelected() {
        loadFragment(VideoFragment(), TAG_VIDEO)
    }

    fun showFullScreenFragment(index: Int) {
        val fragment = FullScreenFragment.newInstance(index)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, TAG_FULL_SCREEN)
            .addToBackStack(null)
            .commit()
        currentFragmentTag = TAG_FULL_SCREEN
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            currentFragmentTag = TAG_GALLERY

            // ✅ Восстанавливаем Edge-to-Edge при возврате из FullScreen
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        } else {
            super.onBackPressed()
        }
    }
}