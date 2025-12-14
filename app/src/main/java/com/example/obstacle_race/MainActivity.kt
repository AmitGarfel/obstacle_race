package com.example.obstacle_race

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var main_IMG_hearts: Array<AppCompatImageView>
    private lateinit var main_FAB_left: FloatingActionButton
    private lateinit var main_FAB_right: FloatingActionButton

    private lateinit var matrixViews: Array<Array<AppCompatImageView>>

    private val ROWS = 10
    private val COLS = 3
    private var matrix = Array(ROWS) { Array(COLS) { 0 } }

    private var currentLane = 1
    private var lives = 3

    private var stepCounter = 0
    private val OBSTACLE_FREQUENCY = 3

    private var gameRunning = true
    private var timer: java.util.Timer? = null
    private var timerTask: java.util.TimerTask? = null

    private val CAR_ROW = ROWS - 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViews()
        initViews()
    }

    private fun findViews() {
        // Hearts
        main_IMG_hearts = arrayOf(
            findViewById(R.id.main_IMG_heart0),
            findViewById(R.id.main_IMG_heart1),
            findViewById(R.id.main_IMG_heart2)
        )

        // Buttons
        main_FAB_left = findViewById(R.id.main_FAB_left)
        main_FAB_right = findViewById(R.id.main_FAB_right)

        // Matrix 10x3
        matrixViews = Array(ROWS) { row ->
            Array(COLS) { col ->
                findViewById(
                    resources.getIdentifier(
                        "main_IMG_${row}${col}",
                        "id",
                        packageName
                    )
                )
            }
        }
    }

    private fun initViews() {
        main_FAB_left.setOnClickListener { moveCarLeft() }
        main_FAB_right.setOnClickListener { moveCarRight() }

        updateHeartsUI()
        refreshUI()
        startGameLoop()
    }

    private fun moveCarLeft() {
        if (currentLane > 0) {
            currentLane--
            refreshUI()
        }
    }

    private fun moveCarRight() {
        if (currentLane < COLS - 1) {
            currentLane++
            refreshUI()
        }
    }

    private fun addNewObstacle() {
        val lane = (0 until COLS).random()
        matrix[0][lane] = 1
    }

    private fun moveMatrixDown() {
        // Shift matrix down
        for (row in ROWS - 1 downTo 1) {
            for (col in 0 until COLS) {
                matrix[row][col] = matrix[row - 1][col]
            }
        }

        // Clear top row
        for (col in 0 until COLS) matrix[0][col] = 0

        // Collision check
        if (matrix[CAR_ROW][currentLane] == 1) {
            matrix[CAR_ROW][currentLane] = 0
            lives--
            Log.d("GAME_DEBUG", "Crash detected! Lives left: $lives")
            updateHeartsUI()
            crashFeedback()

            if (lives == 0) {
                gameRunning = false
                stopGameLoop()
                changeActivity("GAME OVER 💔😭")
            }
        }
    }

    private fun refreshUI() {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                when {
                    row == ROWS - 1 && col == currentLane ->
                        matrixViews[row][col].setImageResource(R.drawable.car)

                    matrix[row][col] == 1 ->
                        matrixViews[row][col].setImageResource(R.drawable.obstacle)

                    else ->
                        matrixViews[row][col].setImageResource(0)
                }
            }
        }
    }

    private fun updateHeartsUI() {
        for (i in main_IMG_hearts.indices) {
            main_IMG_hearts[i].alpha = if (i < lives) 1f else 0.3f
        }
    }

    private fun crashFeedback() {
        Toast.makeText(this, "Crash!", Toast.LENGTH_SHORT).show()
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun startGameLoop() {
        if (timer != null) return

        timer = java.util.Timer()
        timerTask = object : java.util.TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (!gameRunning) return@runOnUiThread

                    if (stepCounter % OBSTACLE_FREQUENCY == 0) addNewObstacle()
                    stepCounter++



                    moveMatrixDown()
                    refreshUI()
                }
            }
        }

        timer!!.scheduleAtFixedRate(timerTask, 0, 600)
    }

    private fun stopGameLoop() {
        timerTask?.cancel()
        timer?.cancel()
        timer = null
        timerTask = null
    }

    private fun changeActivity(message: String) {
        val intent = Intent(this, GameOverActivity::class.java)
        val bundle = Bundle()
        bundle.putString("MESSAGE_KEY", message)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopGameLoop()
    }
}


