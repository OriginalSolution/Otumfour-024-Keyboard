package gh.otumfour.o24.ime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent

/** Collects one spoken phrase and hands it back to the keyboard. */
class VoiceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listen = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        val opened = runCatching { startActivityForResult(listen, 1) }.isSuccess
        if (!opened) finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (resultCode == RESULT_OK && !spoken.isNullOrBlank()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, spoken).commit()
        }
        finish()
    }

    companion object {
        const val PREFS = "o24_voice"
        const val KEY = "pending"
    }
}
