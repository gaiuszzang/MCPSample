package io.groovin.mcpsample.util

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import io.groovin.mcpsample.R
import java.util.Locale

class MicInputController(
    private val context: Context,
    private val useEffectSound: Boolean = false
) {
    companion object {
        private const val TAG = "MicInputController"
    }

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent. EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    var listener: MicInputListener? = null

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                logd(TAG, "onBeginningOfSpeech")
                listener?.onStart()
            }
            override fun onEndOfSpeech() {
                logd(TAG, "onEndOfSpeech")
                listener?.onStop()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // -2 ~ 10 -> -2~3은 무시하고 3~10을 0 ~ 100으로 변환
                val convertedRmsDb = ((rmsdB.coerceIn(3f, 10f) - 3f) * 100f / 7f).toInt().coerceIn(0, 100)
                listener?.onRmsDb(convertedRmsDb)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onError(error: Int) {
                if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                    listener?.onError(error)
                    stopRecording()
                } else {
                    if (isRecording) {
                        startListening()
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                logd(TAG, "onResults : $results")
                if (results == null) return
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                listener?.onResult(matches.joinToString())
                startListening()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                logd(TAG, "onPartialResults : $partialResults")
                if (partialResults == null) return
                val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                listener?.onPartialResults(matches.joinToString())
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    private var isRecording = false

    fun startRecording() {
        if (!isRecording) {
            isRecording = true
            playStartSoundEffect()
            startListening()
        }
    }

    fun stopRecording() {
        if (isRecording) {
            isRecording = false
            stopListening()
            playStopSoundEffect()
        }
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun startListening() {
        speechRecognizer.startListening(recognizerIntent)
    }

    private fun stopListening() {
        logd(TAG, "stopListening")
        speechRecognizer.stopListening()
    }

    private fun playStartSoundEffect() {
        if (!useEffectSound) return
        val mediaPlayer = MediaPlayer.create(context, R.raw.mic_start)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            mp.release()
            true
        }
        mediaPlayer.start()
    }

    private fun playStopSoundEffect() {
        if (!useEffectSound) return
        val mediaPlayer = MediaPlayer.create(context, R.raw.mic_end)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            mp.release()
            true
        }
        mediaPlayer.start()
    }
}

interface MicInputListener {
    fun onStart()
    fun onStop()
    fun onError(error: Int)
    fun onPartialResults(partialResults: String)
    fun onResult(result: String)
    fun onRmsDb(rmsDB: Int)
}
