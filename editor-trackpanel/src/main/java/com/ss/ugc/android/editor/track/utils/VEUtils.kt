package com.ss.ugc.android.editor.track.utils

import com.ss.android.ttve.model.VEWaveformVisualizer
import com.ss.android.vesdk.VEUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object VEUtils {

    val waveMap : HashMap<String, FloatArray> = hashMapOf<String, FloatArray>()


    suspend  fun  getWaveArray(path: String, pointCount: Int): FloatArray {
     return   withContext(Dispatchers.IO){
           val key = path + pointCount
           if (waveMap[key] != null) {
              return@withContext waveMap[key]!!
           }
           val result = VEUtils.getMusicWaveData(
               path,
               VEWaveformVisualizer.MultiChannelMean or VEWaveformVisualizer.SampleMax,
               pointCount
           )?.waveBean
               ?: FloatArray(0)
           waveMap[key] = result
            result

       }
    }
}