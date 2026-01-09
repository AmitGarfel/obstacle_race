package com.example.obstacle_race.logic

data class HighScore(
    val id: Long,
    val name: String,
    val score: Int,
    val lat: Double? = null,
    val lng: Double? = null
) {
    val displayText: String
        get() = "$name - $score"
}
