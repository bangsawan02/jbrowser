package fulguris.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import fulguris.settings.preferences.UserPreferences
import javax.inject.Inject

/**
 * Lightweight Introduction activity replacing AppIntro.
 * Automatically accepts terms and proceeds to the main activity.
 */
@AndroidEntryPoint
class IntroActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    fun nextSlide() {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userPreferences.acceptTerms = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
