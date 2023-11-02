package dev.arkbuilders.arkshelf.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.arkbuilders.arkshelf.R
import dev.arkbuilders.arkshelf.databinding.ActivityMainBinding
import dev.arkbuilders.arkshelf.presentation.searchedit.SearchEditFragment
import dev.arkbuilders.arkshelf.presentation.settings.SettingsFragment

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private val binding by viewBinding(ActivityMainBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val url = checkShareIntent(intent)
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkShareIntent(intent)?.let { url ->
            supportFragmentManager.setFragmentResult(
                SearchEditFragment.REQUEST_SHARE_URL_KEY,
                Bundle().apply {
                    putString(SearchEditFragment.URL_KEY, url)
                }
            )
        }
    }

    private fun checkShareIntent(shareIntent: Intent?): String? {
        shareIntent ?: return null

        if (shareIntent.action == Intent.ACTION_SEND && shareIntent.type == "text/plain") {
            return shareIntent.getStringExtra(Intent.EXTRA_TEXT)
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