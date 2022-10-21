package com.ss.ugc.android.editor.track.utils

import android.content.res.Configuration
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener
import com.ss.ugc.android.editor.track.TrackSdk
import java.util.LinkedList

interface OrientationListener {
    fun onOrientationChanged(orientation: Int)
}

object OrientationManager {
    private var screenOrientation = Configuration.ORIENTATION_PORTRAIT
    private val listenerList = LinkedList<OrientationListener>()
    private val isPad = PadUtil.isPad
    private val uiHandler = Handler(Looper.getMainLooper())
    private var hadInit = false

    fun initOrientation() {
        if (!hadInit) {
            hadInit = true
            screenOrientation = TrackSdk.application.resources.configuration.orientation
        }
    }

    fun getOrientation(): Int {
        return screenOrientation
    }

    fun isLand(): Boolean = screenOrientation == Configuration.ORIENTATION_LANDSCAPE

    private val orientationListener: OrientationEventListener by lazy {
        val context = TrackSdk.application
        object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                val o = context.resources.configuration.orientation
                // Log.i(TAG, "manager orientation $o")
                if (o != Configuration.ORIENTATION_UNDEFINED && o != screenOrientation) {
                    screenOrientation = o
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        notify(o)
                    } else {
                        uiHandler.post { notify(o) }
                    }
                }
            }
        }
    }

    fun startMonitor() {
        if (!isPad) return
        orientationListener.enable()
    }

    @Synchronized
    fun register(listener: OrientationListener) {
        if (!isPad) return
        if (listener !in listenerList) {
            listenerList.add(listener)
        }
    }

    @Synchronized
    fun unregister(listener: OrientationListener) {
        if (!isPad) return
        listenerList.removeLastOccurrence(listener)
    }

    @Synchronized
    fun notify(orientation: Int) {
        listenerList.forEach { listener ->
            listener.onOrientationChanged(orientation)
        }
    }

    @Synchronized
    fun updateOrientation(orientation: Int) {
//        TrackSdk.i(TAG, "debug update orientation $orientation")
        if (orientation != Configuration.ORIENTATION_UNDEFINED && orientation != screenOrientation) {
            screenOrientation = orientation

            if (Looper.myLooper() == Looper.getMainLooper()) {
                notify(orientation)
            } else {
                uiHandler.post { notify(orientation) }
            }
        }
    }
}
