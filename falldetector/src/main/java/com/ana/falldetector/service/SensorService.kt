package com.ana.falldetector.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ana.falldetector.R
import com.ana.falldetector.init.CHANNEL_1_ID
import com.ana.falldetector.init.CHANNEL_1_ID_NOTIFICATIONS
import com.ana.falldetector.init.CHANNEL_1_ID_NOTIFICATIONS_TEXT
import com.ana.falldetector.init.FallDetector
import com.ana.falldetector.model.Drop
import com.ana.falldetector.model.DropRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/*
acceleration variables represent the minimum and maximum thresholds
of when the library considers the device to have free fallen

because the ACCELEROMETER sensor is used, when the device is in free-fall,
the acceleration levels reach approximately 0.0. Based on trial and error,
the thresholds 0.0F and 0.5F have been established.
 */
const val ACCELERATION_GRAVITY_MIN_FALL = 0.0F
const val ACCELERATION_GRAVITY_MAX_FALL = 0.5F

/*
seemingly, when it comes to the ACCELEROMETER, when the device is still,
it reads an acceleration level that includes gravity. Based on trial and error,
the thresholds 9.5F and 10.2F have been established, in order to record when the
device has stopped falling
 */
const val ACCELERATION_GRAVITY_MIN_REGULAR = 9.5F
const val ACCELERATION_GRAVITY_MAX_REGULAR = 10.2F

/*
the following constants are used for notification purposes
 */
const val NOTIFICATION_TITLE = "FreeFall detector"
const val NOTIFICATION_MESSAGE = "Fall detected"
const val NOTIFICATION_ID_FALL_DETECTED = 2
const val ONGOING_NOTIFICATION_ID = 1

class SensorService : Service(), SensorEventListener {

    /*
    in order to properly use the library, the app must
    initialize the library and pass its context by using the
    FallDetector object.
     */
    private val context = FallDetector.applicationContext

    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor

    /*
    the acceleration threshold and timestamp are stored once the
    acceleration level reaches the minimum - maximum free fall drop range (0.0-0.5)
     */
    private var accelerationThreshold: Float = 0F
    private var thresholdTimeStamp: Long = 0L

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    /*
    This method is called every time a sensor changes on the device. Because the library uses
    ACCELEROMETER, it is necessary to first check which sensor is sending signals with an if statement.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

            /*
            if the accelerationThreshold has been set and is not 0 (!=0), we move on to tracking when the
            acceleration reaches back to its normal levels (8.5..10.2)
            if it has reached normal levels, the drop is recorded along with its duration
             */
            if (accelerationThreshold != 0F) {

                //calculate acceleration based on the three axis (x, y, z)
                val acceleration = calculateAcceleration(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )

                //if acceleration is in between thresholds, the drop is recorded
                if (acceleration in ACCELERATION_GRAVITY_MIN_REGULAR..ACCELERATION_GRAVITY_MAX_REGULAR) {

                    val dropDurationInNano = event.timestamp - thresholdTimeStamp
                    val dropDurationInSeconds = convertNanoSecondsToSeconds(dropDurationInNano)

                    //reset acceleration thresholds to 0 to keep monitoring new falls
                    accelerationThreshold = 0F
                    thresholdTimeStamp = 0L

                    //pass record of drop to Repository
                    GlobalScope.launch(Dispatchers.IO) {
                        DropRepository.recordDrop(Drop(dropDurationInSeconds, Date().time))
                    }

                    //display notifications based on SDK
                    if (Build.VERSION.SDK_INT >= 26) {
                        displayNotificationApi26()
                    } else {
                        displayNotificationApiLower26()
                    }
                }
            }

            else {

                //if acceleration threshold is 0, we calculate acceleration based on the three axis (x, y, z).
                val acceleration = calculateAcceleration(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )

                //assign the thresholds if event is within the free fall boundaries (0.0..0.5)
                if (acceleration in ACCELERATION_GRAVITY_MIN_FALL..ACCELERATION_GRAVITY_MAX_FALL) {
                    accelerationThreshold = acceleration
                    thresholdTimeStamp = event.timestamp
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= 26) {

            createNotificationChannels()

            val notification: android.app.Notification = NotificationCompat.Builder(
                context!!, CHANNEL_1_ID_NOTIFICATIONS
            )
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_MESSAGE)
                .build()

            //maintain Service in foreground (even when app closed) - SDK >=26
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        } else {
            val notification: android.app.Notification = NotificationCompat.Builder(
                context!!, CHANNEL_1_ID_NOTIFICATIONS
            )
                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_MESSAGE)
                .build()

            //maintain Service in foreground (even when app closed) - SDK < 26
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }

        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    //create notification channel for Api >= 26
    private fun createNotificationChannels() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_1_ID_NOTIFICATIONS
            val descriptionText = CHANNEL_1_ID_NOTIFICATIONS_TEXT
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_1_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    //display notification for Api26 by passing Channel ID
    private fun displayNotificationApi26() {
        val builder = NotificationCompat.Builder(context!!, CHANNEL_1_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_MESSAGE)
            .setSmallIcon(R.drawable.ic_info_outline_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_FALL_DETECTED, builder.build())
        }

    }

    //display notification for Api26 without passing Channel ID
    private fun displayNotificationApiLower26() {
        val builder = NotificationCompat.Builder(context!!)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_MESSAGE)
            .setSmallIcon(R.drawable.ic_info_outline_black_24dp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_FALL_DETECTED, builder.build())
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    private fun calculateAcceleration(x: Float, y: Float, z: Float): Float {
        return sqrt(x.pow(2) + y.pow(2) + z.pow(2))
    }

    private fun convertNanoSecondsToSeconds(nano: Long): Double {
        val converter: Double = 10.0.pow(-9)
        return nano.toDouble().times(converter)
    }
}