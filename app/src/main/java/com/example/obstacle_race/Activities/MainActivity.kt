package com.example.obstacle_race.Activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.example.obstacle_race.R
import com.example.obstacle_race.logic.GameFlowManager
import com.example.obstacle_race.logic.GameManager
import com.example.obstacle_race.ui.GameDialogs
import com.example.obstacle_race.ui.GameViewManager
import com.example.obstacle_race.utilities.IntentKeys
import com.example.obstacle_race.utilities.LocationHelper
import com.example.obstacle_race.utilities.SensorManagerHelper
import com.example.obstacle_race.utilities.SharedPreferencesManager
import com.example.obstacle_race.utilities.SignalManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView

class MainActivity : AppCompatActivity() {

    private lateinit var hearts: Array<AppCompatImageView>
    private lateinit var matrixViews: Array<Array<AppCompatImageView>>
    private lateinit var txtScore: MaterialTextView
    private lateinit var txtDistance: MaterialTextView
    private lateinit var fabLeft: FloatingActionButton
    private lateinit var fabRight: FloatingActionButton
    private lateinit var fabBack: FloatingActionButton

    private lateinit var game: GameManager
    private lateinit var flow: GameFlowManager
    private lateinit var gameViewManager: GameViewManager
    private lateinit var dialogs: GameDialogs
    private lateinit var signalManager: SignalManager
    private var sensorHelper: SensorManagerHelper? = null

    private lateinit var playerName: String
    private var controlMode: String = IntentKeys.MODE_BUTTON
    private var initialSpeed: Long = 600L

    private var navigatingToHighScores = false
    private var shouldShowPauseDialogOnResume = false
    private var isGameStarting = true

    private val fusedLocation by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }
    private lateinit var locationHelper: LocationHelper

    // Game end snapshot
    private var endedCoins: Int = 0
    private var endedDistance: Int = 0
    private var endedFinalScore: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SharedPreferencesManager.init(this)

        initialSpeed = readIntentExtras()

        dialogs = GameDialogs(this)
        signalManager = SignalManager(this).also { it.init() }

        locationHelper = LocationHelper(
            context = this,
            fused = fusedLocation
        )

        findViews()

        gameViewManager = GameViewManager(
            matrixViews,
            hearts,
            txtScore,
            txtDistance,
            fabLeft,
            fabRight
        )

        game = GameManager(
            initialSpeed = initialSpeed,
            onEvent = { event -> runOnUiThread { handleGameEvent(event) } }
        )

        val isButtonMode = controlMode == IntentKeys.MODE_BUTTON
        gameViewManager.setControlMode(isButtonMode)

        sensorHelper = if (!isButtonMode) {
            SensorManagerHelper(
                this,
                moveLeft = { game.moveCarLeft() },
                moveRight = { game.moveCarRight() },
                adjustSpeed = { game.updateGameSpeed(it) }
            ).apply { setInitialSpeed(initialSpeed) }
        } else {
            null
        }

        flow = GameFlowManager(
            game = game,
            sensorHelper = sensorHelper,
            isSensorMode = !isButtonMode
        )

        fabLeft.setOnClickListener { game.moveCarLeft() }
        fabRight.setOnClickListener { game.moveCarRight() }
        fabBack.setOnClickListener { showPauseDialog() }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showPauseDialog()
                }
            }
        )

        isGameStarting = true
        flow.start()
        refreshUI()
    }

    private fun findViews() {
        hearts = arrayOf(
            findViewById(R.id.main_IMG_heart0),
            findViewById(R.id.main_IMG_heart1),
            findViewById(R.id.main_IMG_heart2)
        )

        txtScore = findViewById(R.id.main_TXT_coins)
        txtDistance = findViewById(R.id.main_TXT_distance)

        fabLeft = findViewById(R.id.main_FAB_left)
        fabRight = findViewById(R.id.main_FAB_right)
        fabBack = findViewById(R.id.main_FAB_back_to_menu)

        matrixViews = Array(10) { row ->
            Array(5) { col ->
                findViewById(
                    resources.getIdentifier("main_IMG_${row}${col}", "id", packageName)
                )
            }
        }
    }

    private fun readIntentExtras(): Long {
        playerName = intent.getStringExtra(IntentKeys.PLAYER_NAME) ?: "Player"
        controlMode = intent.getStringExtra(IntentKeys.CONTROL_MODE) ?: IntentKeys.MODE_BUTTON
        return intent.getLongExtra(IntentKeys.GAME_SPEED, 600L)
    }

    private fun handleGameEvent(event: GameManager.GameEvent) {
        when (event) {
            is GameManager.GameEvent.Tick -> refreshUI()
            is GameManager.GameEvent.Crash -> signalManager.crashFeedback()
            is GameManager.GameEvent.Coin -> {
                signalManager.coinFeedback()
                refreshUI()
            }
            is GameManager.GameEvent.GameOver -> onGameEnded()
        }
    }

    private fun refreshUI() {
        gameViewManager.updateUI(
            matrix = game.getMatrix(),
            currentLane = game.currentLane,
            lives = game.lives,
            score = game.score,
            distance = game.stepCounter
        )
    }

    private fun showPauseDialog() {
        if (!flow.isActive) return

        if (!flow.isPausedByDialog) {
            flow.pauseByDialog()
        }

        dialogs.showPauseDialog(
            onContinue = { flow.resume() },
            onHighScores = {
                navigatingToHighScores = true
                shouldShowPauseDialogOnResume = true
                startActivity(Intent(this, HighScoresActivity::class.java))
            },
            onBackToMenu = { goToMenu() }
        )
    }

    private fun onGameEnded() {
        flow.stop()

        endedCoins = game.score
        endedDistance = game.stepCounter
        endedFinalScore = game.getFinalScore()

        SharedPreferencesManager.getInstance().saveFinalScore(
            playerName = playerName,
            finalScore = endedFinalScore,
            hasLocationPermission = { locationHelper.hasLocationPermission() },
            fetchLastKnownLocation = { cb -> locationHelper.fetchLastKnownLocation(cb) },
            onDone = { showFinalGameOverDialog() }
        )
    }

    private fun showFinalGameOverDialog() {
        dialogs.showGameOverDialog(
            coins = endedCoins,
            distance = endedDistance,
            finalScore = endedFinalScore,
            onPlayAgain = { restartGame() },
            onHighScores = {
                val menuIntent = Intent(this, MenuActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(menuIntent)
                startActivity(Intent(this, HighScoresActivity::class.java))
                finish()
            },
            onBackToMenu = { goToMenu() }
        )
    }

    private fun restartGame() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(IntentKeys.PLAYER_NAME, playerName)
            putExtra(IntentKeys.CONTROL_MODE, controlMode)
            putExtra(IntentKeys.GAME_SPEED, initialSpeed)
        }
        finish()
        startActivity(restartIntent)
    }

    private fun goToMenu() {
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (flow.isActive && !flow.isPausedByDialog && !isGameStarting) {
            flow.pauseByDialog()
            shouldShowPauseDialogOnResume = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (navigatingToHighScores) {
            navigatingToHighScores = false
            if (flow.isActive) showPauseDialog()
            return
        }
        if (shouldShowPauseDialogOnResume) {
            shouldShowPauseDialogOnResume = false
            showPauseDialog()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) isGameStarting = false
        if (!hasFocus && flow.isActive && !flow.isPausedByDialog && !isGameStarting) {
            flow.pauseByDialog()
            shouldShowPauseDialogOnResume = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorHelper?.stop()
        signalManager.release()
        game.release()
    }
}