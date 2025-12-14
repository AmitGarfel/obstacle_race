package com.example.obstacle_race

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

private lateinit var gameOver_TXT_title: MaterialTextView

private lateinit var gameover_BTN_again: MaterialButton


class GameOverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_over)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViews()
        initViews()
    }

    private fun findViews() {
        gameOver_TXT_title = findViewById(R.id.gameOver_TXT_title)
        gameover_BTN_again = findViewById(R.id.gameover_BTN_again)
    }

    private fun initViews() {

        val msg = intent.extras?.getString("MESSAGE_KEY") ?: "GAME OVER"
        gameOver_TXT_title.text = msg

        gameover_BTN_again.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


}