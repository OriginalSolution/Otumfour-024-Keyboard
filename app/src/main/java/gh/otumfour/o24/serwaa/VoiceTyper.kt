package gh.otumfour.o24.serwaa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceTyper(
    private val context: Context,
    private val onState: (String) -> Unit,
    private val onText: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String) {
        stop()
        if (!available()) {
            onState("Speech recognition is not available on this phone.")
            return
        }
        val speech = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speech
        speech.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onState("Serwaa is listening…")
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                onState("Serwaa is working out the words…")
            }

            override fun onError(error: Int) {
                onState(errorText(error))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isBlank()) onState("Serwaa didn't catch that. Try again.")
                else onText(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (text.isNotBlank()) onState(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Serwaa")
        }
        onState("Serwaa is listening…")
        speech.startListening(intent)
    }

    fun stop() {
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun errorText(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Serwaa could not open the microphone."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow the microphone in O24 Keyboard settings."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Serwaa needs a network connection for voice typing on this phone."
        SpeechRecognizer.ERROR_NO_MATCH -> "Serwaa didn't catch that. Try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Serwaa didn't hear anything."
        else -> "Voice typing stopped. Try again."
    }
}
