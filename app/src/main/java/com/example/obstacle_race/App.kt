package com.example.obstacle_race

import android.app.Application
import com.example.obstacle_race.utilities.SharedPreferencesManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedPreferencesManager.init(this)
    }
}
