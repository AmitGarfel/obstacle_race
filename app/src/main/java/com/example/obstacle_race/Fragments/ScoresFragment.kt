package com.example.obstacle_race.Fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.obstacle_race.R
import com.example.obstacle_race.logic.HighScore
import com.example.obstacle_race.utilities.SharedPreferencesManager

class ScoresFragment : Fragment() {

    interface OnScoreSelectedListener {
        fun onScoreSelected(lat: Double, lng: Double)
        fun onScoreSelectedNoLocation()
    }

    private var listener: OnScoreSelectedListener? = null
    private lateinit var listScores: ListView
    private var scoresData: List<HighScore> = emptyList()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnScoreSelectedListener
            ?: throw RuntimeException("$context must implement OnScoreSelectedListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_scores, container, false)
        listScores = view.findViewById(R.id.list_scores)

        loadScores()

        listScores.setOnItemClickListener { _, _, position, _ ->
            val s = scoresData[position]
            val lat = s.lat
            val lng = s.lng

            if (lat != null && lng != null) {
                listener?.onScoreSelected(lat, lng)
            } else {
                listener?.onScoreSelectedNoLocation()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (this::listScores.isInitialized) {
            loadScores()
        }
    }

    fun refreshScores() {
        if (!isAdded) return
        if (!this::listScores.isInitialized) return
        loadScores()
    }

    private fun loadScores() {
        val prefs = SharedPreferencesManager.getInstance()
        scoresData = prefs.getHighScores()

        val items = scoresData.mapIndexed { index, hs ->
            val medal = when (index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "  "
            }

            val loc = if (hs.lat != null && hs.lng != null) "📍" else "—"
            "$medal $loc ${hs.displayText}"
        }

        listScores.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            items
        )
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
