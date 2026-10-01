package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playScanSuccess(context: Context) {
        // Haptic pulse (tick)
        vibrate(context, 35)
        // Subtle crisp beep
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
        } catch (e: Exception) {
            // Ignore audio failure
        }
    }

    fun playAddToCart(context: Context) {
        // Double-tap haptic
        vibratePattern(context, longArrayOf(0, 25, 45, 30))
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playCrossingBudget(context: Context) {
        // Long warning buzz
        vibratePattern(context, longArrayOf(0, 160, 80, 200))
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 180)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playBudgetWarning(context: Context) {
        playCrossingBudget(context)
    }

    fun playCelebrateUnderBudget(context: Context) {
        // Celebratory festive pattern
        vibratePattern(context, longArrayOf(0, 40, 50, 40, 50, 80))
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun playClick(context: Context) {
        vibrate(context, 15)
    }

    private fun vibrate(context: Context, millis: Long) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(millis)
                }
            }
        } catch (e: Exception) {
            // Ignore permission or hardware exceptions
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
