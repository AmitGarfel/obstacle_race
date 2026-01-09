package com.example.obstacle_race.utilities

import android.content.Context
import android.content.SharedPreferences
import com.example.obstacle_race.logic.HighScore
import java.lang.Double.doubleToRawLongBits
import java.lang.Double.longBitsToDouble

class SharedPreferencesManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_HIGH_SCORES, Context.MODE_PRIVATE)

    private val playerPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_PLAYER, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_HIGH_SCORES = "HIGH_SCORES"
        private const val PREFS_PLAYER = "PLAYER_PREFS"

        private const val KEY_PLAYER_NAME = "player_name"

        private const val KEY_USE_LOCATION = "use_location"
        private const val KEY_LOCATION_PREF_SET = "location_pref_set"

        private const val KEY_PENDING_SCORE_ID = "pending_score_id"

        private const val KEY_ID = "id_"
        private const val KEY_NAME = "name_"
        private const val KEY_SCORE = "score_"
        private const val KEY_LAT = "lat_"
        private const val KEY_LNG = "lng_"

        @Volatile
        private var instance: SharedPreferencesManager? = null

        fun init(context: Context): SharedPreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPreferencesManager(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): SharedPreferencesManager {
            return instance ?: throw IllegalStateException("SharedPreferencesManager must be initialized")
        }
    }

    fun savePlayerName(name: String) {
        playerPrefs.edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    fun getPlayerName(defaultName: String = "Player"): String {
        return playerPrefs.getString(KEY_PLAYER_NAME, defaultName) ?: defaultName
    }

    fun hasPlayerName(): Boolean {
        return !playerPrefs.getString(KEY_PLAYER_NAME, null).isNullOrBlank()
    }

    fun getHighScores(): List<HighScore> {
        val scores = mutableListOf<HighScore>()

        for (i in 0 until 10) {
            val name = sharedPreferences.getString(KEY_NAME + i, null)
            val score = sharedPreferences.getInt(KEY_SCORE + i, -1)

            if (name != null && score >= 0) {
                val id =
                    if (sharedPreferences.contains(KEY_ID + i)) sharedPreferences.getLong(KEY_ID + i, 0L)
                    else (name.hashCode().toLong() shl 32) xor score.toLong() xor i.toLong()

                val lat =
                    if (sharedPreferences.contains(KEY_LAT + i))
                        longBitsToDouble(sharedPreferences.getLong(KEY_LAT + i, 0L))
                    else null

                val lng =
                    if (sharedPreferences.contains(KEY_LNG + i))
                        longBitsToDouble(sharedPreferences.getLong(KEY_LNG + i, 0L))
                    else null

                scores.add(HighScore(id = id, name = name, score = score, lat = lat, lng = lng))
            }
        }

        return normalizeTop10(scores)
    }

    fun updateHighScoreLocation(scoreId: Long, lat: Double?, lng: Double?) {
        val scores = getHighScores().toMutableList()
        val idx = scores.indexOfFirst { it.id == scoreId }
        if (idx == -1) return

        scores[idx] = scores[idx].copy(lat = lat, lng = lng)
        writeTop10(normalizeTop10(scores))
    }

    fun setLocationPreference(enabled: Boolean) {
        playerPrefs.edit()
            .putBoolean(KEY_USE_LOCATION, enabled)
            .putBoolean(KEY_LOCATION_PREF_SET, true)
            .apply()
    }

    fun isLocationPrefSet(): Boolean = playerPrefs.getBoolean(KEY_LOCATION_PREF_SET, false)
    fun shouldGetLocation(): Boolean = playerPrefs.getBoolean(KEY_USE_LOCATION, false)

    fun setPendingScoreId(id: Long) {
        playerPrefs.edit().putLong(KEY_PENDING_SCORE_ID, id).apply()
    }

    fun consumePendingScoreId(): Long? {
        if (!playerPrefs.contains(KEY_PENDING_SCORE_ID)) return null
        val id = playerPrefs.getLong(KEY_PENDING_SCORE_ID, -1L)
        playerPrefs.edit().remove(KEY_PENDING_SCORE_ID).apply()
        return if (id > 0) id else null
    }

    fun clearPendingScoreId() {
        playerPrefs.edit().remove(KEY_PENDING_SCORE_ID).apply()
    }

    // Saves the score, and attaches location if enabled/available.
    fun saveFinalScore(
        playerName: String,
        finalScore: Int,
        hasLocationPermission: () -> Boolean,
        fetchLastKnownLocation: ((Double?, Double?) -> Unit) -> Unit,
        onDone: () -> Unit
    ) {
        val scores = getHighScores()
        val isTop10 = scores.size < 10 || finalScore > scores.last().score
        if (!isTop10) {
            onDone()
            return
        }

        val scoreId = addHighScoreToList(
            existing = scores,
            name = playerName,
            score = finalScore,
            id = System.currentTimeMillis()
        )

        val prefSet = isLocationPrefSet()
        val enabled = shouldGetLocation()

        // No user choice yet: keep a pending id so enabling later can update the last score.
        if (!prefSet) {
            if (hasLocationPermission()) {
                fetchLastKnownLocation { lat, lng ->
                    if (lat != null && lng != null) {
                        updateHighScoreLocation(scoreId, lat, lng)
                        clearPendingScoreId()
                    } else {
                        setPendingScoreId(scoreId)
                    }
                    onDone()
                }
            } else {
                setPendingScoreId(scoreId)
                onDone()
            }
            return
        }

        // User explicitly disabled location.
        if (!enabled) {
            clearPendingScoreId()
            onDone()
            return
        }

        // User enabled location: try to attach it now, otherwise keep pending.
        if (hasLocationPermission()) {
            fetchLastKnownLocation { lat, lng ->
                if (lat != null && lng != null) {
                    updateHighScoreLocation(scoreId, lat, lng)
                    clearPendingScoreId()
                } else {
                    setPendingScoreId(scoreId)
                }
                onDone()
            }
        } else {
            setPendingScoreId(scoreId)
            onDone()
        }
    }

    private fun normalizeTop10(scores: List<HighScore>): List<HighScore> =
        scores.sortedByDescending { it.score }.take(10)

    private fun writeTop10(sortedTop10: List<HighScore>) {
        sharedPreferences.edit().apply {
            clear()
            sortedTop10.forEachIndexed { index, hs ->
                putLong(KEY_ID + index, hs.id)
                putString(KEY_NAME + index, hs.name)
                putInt(KEY_SCORE + index, hs.score)

                if (hs.lat != null && hs.lng != null) {
                    putLong(KEY_LAT + index, doubleToRawLongBits(hs.lat))
                    putLong(KEY_LNG + index, doubleToRawLongBits(hs.lng))
                } else {
                    remove(KEY_LAT + index)
                    remove(KEY_LNG + index)
                }
            }
            apply()
        }
    }

    private fun addHighScoreToList(
        existing: List<HighScore>,
        name: String,
        score: Int,
        id: Long
    ): Long {
        val updated = existing.toMutableList().apply {
            add(HighScore(id = id, name = name, score = score))
        }
        writeTop10(normalizeTop10(updated))
        return id
    }
}
