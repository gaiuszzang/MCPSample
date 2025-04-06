package io.groovin.mcpsample.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTS(context: Context) {
    companion object {
        private const val TAG = "TTS"
    }
    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            logd(TAG, "Initialization done")
            initTTS()
        } else {
            logd(TAG, "Initialization failed")
        }
    }

    private fun initTTS() {
        tts.language = Locale.getDefault()
        tts.setSpeechRate(1.0f)
        tts.setPitch(1.0f)
    }

    fun speak(text: String) {
        logd(TAG, "speak: $text")
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
