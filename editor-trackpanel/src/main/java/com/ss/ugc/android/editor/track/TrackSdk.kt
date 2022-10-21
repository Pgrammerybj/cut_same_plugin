package com.ss.ugc.android.editor.track

import android.content.Context

class TrackSdk {

    companion object {
      @JvmStatic
      fun init(application: Context){
          Companion.application = application
      }
      lateinit  var application : Context
    }

}