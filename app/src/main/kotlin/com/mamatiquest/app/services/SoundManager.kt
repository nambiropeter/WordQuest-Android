package com.mamatiquest.app.services

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Android counterpart to iOS's SoundManager, which plays bundled iOS system
 * sound IDs (no assets needed). Android has no equivalent system-sound bank,
 * so this uses [ToneGenerator] to give real, asset-free audio feedback for
 * the same events. These are generic tones, not a sonic match for the iOS
 * sound IDs — dropping real SFX files under `assets/sounds/` and switching
 * this object to `SoundPool` is a self-contained follow-up whenever those
 * assets are sourced; every call site elsewhere only depends on these
 * function names, not the implementation.
 */
object SoundManager {
    var isEnabled: Boolean = true

    private val toneGenerator: ToneGenerator? =
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()

    private fun play(tone: Int, durationMs: Int = 120) {
        if (!isEnabled) return
        toneGenerator?.startTone(tone, durationMs)
    }

    fun wordFound() = play(ToneGenerator.TONE_PROP_BEEP)
    fun levelComplete() = play(ToneGenerator.TONE_PROP_ACK, 200)
    fun correctAnswer() = play(ToneGenerator.TONE_PROP_ACK)
    fun wrongAnswer() = play(ToneGenerator.TONE_PROP_NACK)
    fun tick() = play(ToneGenerator.TONE_PROP_BEEP, 60)
    fun tap() = play(ToneGenerator.TONE_PROP_BEEP, 60)
}
