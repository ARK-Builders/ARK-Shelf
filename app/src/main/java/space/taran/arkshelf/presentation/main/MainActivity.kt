package space.taran.arkshelf.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import space.taran.arkshelf.R
import space.taran.arkshelf.databinding.ActivityMainBinding
import space.taran.arkshelf.presentation.searchedit.SearchEditFragment
import space.taran.arkshelf.presentation.settings.SettingsFragment

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val url = checkShareIntent()
            supportFragmentManager
                .beginTransaction()
                .add(
                    binding.container.id,
                    SearchEditFragment.newInstance(url),
                    SearchEditFragment.TAG
                )
                .commit()
        }
    }

    private fun checkShareIntent(): String? {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            return intent.getStringExtra(Intent.EXTRA_TEXT)
        }
        return null
    }

    fun navigateToSettings() {
        val searchEditFragment =
            supportFragmentManager.findFragmentByTag(SearchEditFragment.TAG)
                ?: error("SearchEditFragment must exist")
        supportFragmentManager
            .beginTransaction()
            .hide(searchEditFragment)
            .add(
                binding.container.id,
                SettingsFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val REQUEST_CODE_ALL_FILES_ACCESS = 1
        const val REQUEST_CODE_PERMISSIONS = 2
    }
}