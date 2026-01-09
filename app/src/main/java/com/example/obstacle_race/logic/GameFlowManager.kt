package com.example.obstacle_race.logic

import com.example.obstacle_race.utilities.SensorManagerHelper

class GameFlowManager(
    private val game: GameManager,
    private val sensorHelper: SensorManagerHelper?,
    private val isSensorMode: Boolean
) {
    var isActive: Boolean = false
        private set

    var isPausedByDialog: Boolean = false
        private set

    fun start() {
        isActive = true
        isPausedByDialog = false
        game.startGame()
        if (isSensorMode) sensorHelper?.start()
    }

    fun pauseByDialog() {
        if (!isActive || isPausedByDialog) return
        isPausedByDialog = true
        game.stopGame()
        if (isSensorMode) sensorHelper?.stop()
    }


    fun resume() {
        if (!isActive) return
        if (!isPausedByDialog) return
        isPausedByDialog = false
        game.startGame()
        if (isSensorMode) sensorHelper?.start()
    }

    fun stop() {
        isActive = false
        isPausedByDialog = true
        game.stopGame()
        if (isSensorMode) sensorHelper?.stop()
    }
}
