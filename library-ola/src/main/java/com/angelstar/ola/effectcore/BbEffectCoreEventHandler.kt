package com.angelstar.ola.effectcore

import android.os.Handler
import android.os.Message
import android.util.Log
import com.voice.core.BBEffectCore
import com.voice.core.BBEffectEventHandler

class BbEffectCoreEventHandler(handle: Handler) {

    var engine: BBEffectCore? = null

    val STATE_CHANGED: Int = 10001

    val eventHandler = object : BBEffectEventHandler {

        override fun onAudioRouteChanged(routing: Int) {
            Log.i(TAG, "onAudioRouteChanged - routing: $routing")
        }

        override fun onStateChanged(state: Int) {
            val obtain = Message.obtain()
            obtain.what = STATE_CHANGED
            obtain.arg1 = state
            Log.i(TAG, "onStateChanged - state: $state")
            handle.sendMessage(obtain)
        }

        override fun onKMarkSliceScoreUpdate(
            beginPosMs: Int,
            endPosMs: Int,
            pitch: Float,
            score: Float
        ) {
        }

        override fun onKMarkSentenceScoreUpdate(
            index: Int,
            currentScore: Float,
            totalScore: Float
        ) {
        }

        override fun onRecordAudioFrame(
            sampleRate: Int,
            channels: Int,
            samples: Int,
            data: ByteArray?
        ) {
        }

        override fun onProductProgress(progress: Float) {
            Log.i(TAG, "onProductProgress: $progress")
        }

        override fun onAudioVolumeIndication(p0: Int, p1: Long) {
            Log.i(TAG, "onAudioVolumeIndication: $p0,$p1")
        }
    }

    companion object {
        private const val TAG = "Ola-Audio"
    }
}