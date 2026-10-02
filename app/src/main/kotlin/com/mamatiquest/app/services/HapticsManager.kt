package com.mamatiquest.app.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Android counterpart to iOS's `Haptics` enum + `HapticsPlayer` (CoreHaptics).
 * There's no bundled-pattern haptics API below Android 8, so every call here
 * degrades gracefully: predefined effects (API 29+) on capable hardware, a
 * short one-shot below that, and a silent no-op with no vibrator at all —
 * mirroring how iOS's HapticsPlayer silently no-ops on hardware without a
 * Taptic Engine.
 */
object HapticsManager {
    var isEnabled: Boolean = true
    private var vibrator: Vibrator? = null

    fun initialize(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.applicationContext
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun predefined(effectId: Int) {
        val v = vibrator ?: return
        if (!isEnabled || !v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            v.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /** Maps iOS's `.light` impact. */
    fun light() = predefined(VibrationEffect.EFFECT_TICK)

    /** Maps iOS's `.medium` impact. */
    fun medium() = predefined(VibrationEffect.EFFECT_CLICK)

    /** Maps iOS's success notification feedback. */
    fun success() = predefined(VibrationEffect.EFFECT_HEAVY_CLICK)

    /** Maps iOS's selection-changed feedback. */
    fun selection() = predefined(VibrationEffect.EFFECT_TICK)

    /** Maps iOS's error notification feedback — a short double-tap. */
    fun error() {
        val v = vibrator ?: return
        if (!isEnabled || !v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 40, 60, 40), -1)
        }
    }

    /** Approximates iOS's custom CoreHaptics "level complete" rising four-tap pattern. */
    fun playLevelComplete() {
        val v = vibrator ?: return
        if (!isEnabled || !v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 20, 40, 20, 40, 20, 60),
                    intArrayOf(0, 140, 0, 180, 0, 220, 255),
                    -1,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 20, 40, 20, 40, 20, 60), -1)
        }
    }

    /** Approximates iOS's custom CoreHaptics single sharp "correct answer" pulse. */
    fun playCorrectPulse() = predefined(VibrationEffect.EFFECT_CLICK)
}
