package com.example.obstacle_race.utilities

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.obstacle_race.R

class SignalManager(context: Context) {

    private val appContext = context.applicationContext
    private val vibrator =
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    private var crashSound: MediaPlayer? = null
    private var coinSound: MediaPlayer? = null

    fun init() {
        if (crashSound == null) {
            crashSound = MediaPlayer.create(appContext, R.raw.crash)
        }
        if (coinSound == null) {
            coinSound = MediaPlayer.create(appContext, R.raw.coin)
        }
    }

    fun crashFeedback() {
        crashSound?.let {
            try {
                it.seekTo(0)
                it.start()
            } catch (_: IllegalStateException) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(300)
        }
    }

    fun coinFeedback() {
        coinSound?.let {
            try {
                it.seekTo(0)
                it.start()
            } catch (_: IllegalStateException) {}
        }
    }

    fun release() {
        crashSound?.release()
        crashSound = null

        coinSound?.release()
        coinSound = null
    }
}
