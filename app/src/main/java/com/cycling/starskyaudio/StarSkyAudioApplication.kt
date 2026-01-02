package com.cycling.starskyaudio

import android.app.Application
import com.cycling.starsky.StarSky

class StarSkyAudioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        StarSky.init(application = this)
            .setOpenCache(true)
            .setNotificationEnabled(true)
            .setAutoPlay(false)
            .setRestoreState(true)
            .apply()
    }
}
