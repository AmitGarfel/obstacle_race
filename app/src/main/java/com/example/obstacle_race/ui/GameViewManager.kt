package com.example.obstacle_race.ui

import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.example.obstacle_race.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView

class GameViewManager(
    private val matrixViews: Array<Array<AppCompatImageView>>,
    private val hearts: Array<AppCompatImageView>,
    private val txtScore: MaterialTextView,
    private val txtDistance: MaterialTextView,
    private val fabLeft: FloatingActionButton,
    private val fabRight: FloatingActionButton
) {

    // Cached drawables
    private val carDrawable: Drawable? =
        matrixViews[0][0].context.getDrawable(R.drawable.car)
    private val obstacleDrawable: Drawable? =
        matrixViews[0][0].context.getDrawable(R.drawable.obstacle)
    private val coinDrawable: Drawable? =
        matrixViews[0][0].context.getDrawable(R.drawable.coiin)

    fun updateUI(
        matrix: Array<Array<Int>>,
        currentLane: Int,
        lives: Int,
        score: Int,
        distance: Int,
    ) {
        val rows = matrix.size
        val cols = matrix[0].size

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val img = matrixViews[row][col]
                val cellType = matrix[row][col]
                val isCar = (row == rows - 1 && col == currentLane)

                val desiredType = when {
                    isCar -> 99
                    cellType == 1 -> 1
                    cellType == 2 -> 2
                    else -> 0
                }

                val currentType = (img.tag as? Int) ?: -1
                if (currentType == desiredType) continue

                img.tag = desiredType

                when (desiredType) {
                    99 -> {
                        img.setImageDrawable(carDrawable)
                        img.alpha = 1f
                    }
                    1 -> {
                        img.setImageDrawable(obstacleDrawable)
                        img.alpha = 1f
                    }
                    2 -> {
                        img.setImageDrawable(coinDrawable)
                        img.alpha = 1f
                    }
                    0 -> {
                        img.alpha = 0f
                    }
                }
            }
        }

        for (i in hearts.indices) {
            val alpha = if (i < lives) 1f else 0.3f
            if (hearts[i].alpha != alpha) hearts[i].alpha = alpha
        }

        val scoreText = score.toString()
        if (txtScore.text.toString() != scoreText) {
            txtScore.text = scoreText
        }

        val distanceText = "$distance m"
        if (txtDistance.text.toString() != distanceText) {
            txtDistance.text = distanceText
        }
    }

    fun setControlMode(isButtonMode: Boolean) {
        val visibility = if (isButtonMode) View.VISIBLE else View.GONE
        fabLeft.visibility = visibility
        fabRight.visibility = visibility
    }
}
