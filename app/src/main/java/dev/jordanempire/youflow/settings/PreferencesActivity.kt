package dev.jordanempire.youflow.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dev.jordanempire.youflow.R
import dev.jordanempire.youflow.databinding.SettingsLayoutBinding
import dev.jordanempire.youflow.util.ThemeHelper

/** Hosts one of the remaining preference screens (downloads, content, notifications). */
class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(ThemeHelper.getSettingsThemeStyle(this))
        super.onCreate(savedInstanceState)
        val binding = SettingsLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment: Fragment = when (intent.getStringExtra(EXTRA_SCREEN)) {
                SCREEN_DOWNLOADS -> DownloadSettingsFragment()
                SCREEN_NOTIFICATIONS -> NotificationsSettingsFragment()
                else -> ContentSettingsFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.settings_fragment_holder, fragment).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val EXTRA_SCREEN = "screen"
        const val SCREEN_DOWNLOADS = "downloads"
        const val SCREEN_CONTENT = "content"
        const val SCREEN_NOTIFICATIONS = "notifications"

        fun intent(context: Context, screen: String): Intent =
            Intent(context, PreferencesActivity::class.java).putExtra(EXTRA_SCREEN, screen)
    }
}
