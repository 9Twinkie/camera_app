package ru.rut.democamera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ru.rut.democamera.databinding.ActivityMainBinding
import ru.rut.democamera.utils.PermissionsUtil

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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Добавляем NavBarFragment в контейнер навигации
        supportFragmentManager.beginTransaction()
            .replace(R.id.navbarContainer, NavBarFragment(this, R.id.photoBtn))
            .commit()

        // Загружаем первый фрагмент (Фото)
        if (savedInstanceState == null) {
            loadFragment(PhotoFragment(), TAG_PHOTO)
        }
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        // Не перезагружаем если тот же фрагмент уже активен
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
        } else {
            super.onBackPressed()
        }
    }
}