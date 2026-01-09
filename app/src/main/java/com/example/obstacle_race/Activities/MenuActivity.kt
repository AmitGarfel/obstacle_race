package com.example.obstacle_race.Activities

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.obstacle_race.R
import com.example.obstacle_race.utilities.IntentKeys
import com.example.obstacle_race.utilities.SharedPreferencesManager
import com.google.android.material.button.MaterialButton

class MenuActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        prefs = SharedPreferencesManager.getInstance()

        if (!prefs.hasPlayerName()) {
            showNameDialog { name ->
                prefs.savePlayerName(name)
                Toast.makeText(this, "Hi $name!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.menu_BTN_button_slow).setOnClickListener {
            startGame(controlMode = IntentKeys.MODE_BUTTON, speed = 350L)
        }

        findViewById<MaterialButton>(R.id.menu_BTN_button_fast).setOnClickListener {
            startGame(controlMode = IntentKeys.MODE_BUTTON, speed = 220L)
        }

        findViewById<MaterialButton>(R.id.menu_BTN_sensor).setOnClickListener {
            startGame(controlMode = IntentKeys.MODE_SENSOR, speed = 250L)
        }

        findViewById<MaterialButton>(R.id.menu_BTN_highscores).setOnClickListener {
            startActivity(Intent(this, HighScoresActivity::class.java))
        }
    }

    private fun startGame(controlMode: String, speed: Long) {
        val playerName = prefs.getPlayerName()

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(IntentKeys.CONTROL_MODE, controlMode)
            putExtra(IntentKeys.GAME_SPEED, speed)
            putExtra(IntentKeys.PLAYER_NAME, playerName)
        }

        startActivity(intent)
        finish()
    }

    // Forces a non-empty player name before continuing.
    private fun showNameDialog(onNameEntered: (String) -> Unit) {
        val input = EditText(this).apply {
            hint = "Enter player name"
        }

        AlertDialog.Builder(this)
            .setTitle("Player Name")
            .setMessage("Please enter your name")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    onNameEntered(name)
                } else {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                    showNameDialog(onNameEntered)
                }
            }
            .show()
    }
}
