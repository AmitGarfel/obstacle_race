package com.example.obstacle_race.logic

import android.os.Handler
import android.os.HandlerThread
import kotlin.random.Random

class GameManager(
    initialSpeed: Long = 600L,
    private val onEvent: (GameEvent) -> Unit
) {

    sealed class GameEvent {
        data object Tick : GameEvent()
        data object Crash : GameEvent()
        data class Coin(val newScore: Int) : GameEvent()
        data object GameOver : GameEvent()
    }

    companion object {
        const val ROWS = 10
        const val COLS = 5
        const val CAR_ROW = ROWS - 1
        const val OBSTACLE_FREQUENCY = 3

        private const val UI_UPDATE_MIN_GAP_MS = 16L // ~60 FPS
    }

    private var matrix = Array(ROWS) { Array(COLS) { 0 } }

    var currentLane = 2
        private set

    var lives = 3
        private set

    var score = 0
        private set

    var stepCounter = 0
        private set

    var gameSpeed: Long = initialSpeed
        private set

    private var gameRunning = false

    private val gameThread = HandlerThread("GameLoopThread").apply { start() }
    private val handler = Handler(gameThread.looper)

    private var lastUiUpdateTime = 0L

    private fun requestUiUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastUiUpdateTime >= UI_UPDATE_MIN_GAP_MS) {
            lastUiUpdateTime = now
            onEvent(GameEvent.Tick)
        }
    }

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!gameRunning) return
            step()
            handler.postDelayed(this, gameSpeed)
        }
    }

    fun startGame() {
        if (gameRunning) return
        gameRunning = true
        handler.post(gameRunnable)
    }

    fun stopGame() {
        gameRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    fun release() {
        stopGame()
        gameThread.quitSafely()
    }

    fun updateGameSpeed(newSpeedMs: Long) {
        handler.post { gameSpeed = newSpeedMs }
    }

    fun moveCarLeft() {
        handler.post {
            if (!gameRunning) return@post
            if (currentLane > 0) {
                currentLane--
                resolveCell()
                requestUiUpdate()
            }
        }
    }

    fun moveCarRight() {
        handler.post {
            if (!gameRunning) return@post
            if (currentLane < COLS - 1) {
                currentLane++
                resolveCell()
                requestUiUpdate()
            }
        }
    }

    fun getMatrix(): Array<Array<Int>> = matrix

    fun getFinalScore(): Int = stepCounter + (100 * score)

    private fun step() {
        moveMatrixDown()

        if (stepCounter % OBSTACLE_FREQUENCY == 0) {
            addNewObstacleOrCoin()
        }
        stepCounter++

        resolveCell()
        requestUiUpdate()
    }

    private fun addNewObstacleOrCoin() {
        val lane = Random.nextInt(COLS)
        val type = if (Random.nextInt(5) < 3) 1 else 2 // 1=obstacle, 2=coin
        matrix[0][lane] = type
    }

    private fun moveMatrixDown() {
        for (row in ROWS - 1 downTo 1) {
            for (col in 0 until COLS) {
                matrix[row][col] = matrix[row - 1][col]
            }
        }
        for (col in 0 until COLS) {
            matrix[0][col] = 0
        }
    }

    private fun resolveCell() {
        val cell = matrix[CAR_ROW][currentLane]
        when (cell) {
            1 -> {
                matrix[CAR_ROW][currentLane] = 0
                lives--
                onEvent(GameEvent.Crash)

                if (lives <= 0) {
                    stopGame()
                    onEvent(GameEvent.GameOver)
                }
            }
            2 -> {
                matrix[CAR_ROW][currentLane] = 0
                score++
                onEvent(GameEvent.Coin(score))
            }
        }
    }
}
