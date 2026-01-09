package com.example.obstacle_race.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class GameDialogs(private val context: Context) {

    // Shown when the game is paused
    fun showPauseDialog(
        onContinue: () -> Unit,
        onHighScores: () -> Unit,
        onBackToMenu: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Pause")
            .setMessage("Choose an option:")
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ -> onContinue() }
            .setNeutralButton("High Scores") { _, _ -> onHighScores() }
            .setNegativeButton("Back to Menu") { _, _ -> onBackToMenu() }
            .show()
    }

    // Shown when the game ends
    fun showGameOverDialog(
        coins: Int,
        distance: Int,
        finalScore: Int,
        onPlayAgain: () -> Unit,
        onHighScores: () -> Unit,
        onBackToMenu: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Game Over")
            .setMessage(
                "Coins: $coins\n" +
                        "Distance: $distance\n" +
                        "Score: $finalScore"
            )
            .setCancelable(false)
            .setPositiveButton("Play Again") { _, _ -> onPlayAgain() }
            .setNeutralButton("High Scores") { _, _ -> onHighScores() }
            .setNegativeButton("Back to Menu") { _, _ -> onBackToMenu() }
            .show()
    }
}
