package com.ana.falldetector.init

import android.content.Context
import androidx.lifecycle.LiveData
import com.ana.falldetector.model.Drop
import com.ana.falldetector.model.DropRepository

const val CHANNEL_1_ID_NOTIFICATIONS = "FreeFall Detection"
const val CHANNEL_1_ID_NOTIFICATIONS_TEXT = "Notifications let you know that the app is running in the background"
const val CHANNEL_1_ID = "1"

object FallDetector {

    /*
    In order for the library to function properly, it must be initialized by passing
    the ApplicationContext. Preferably, this should be done in the app by extending the Application
    element and calling the initialize(context: Context) method in init {} or onCreate()
     */

    var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context
    }

    /*
    this method allows easy access to ROOM database list, by providing a LiveData element that
    stores the list of drops.
     */

    fun getAllDrops(): LiveData<List<Drop>>? {
        return DropRepository.getAllDrops()
    }
}
