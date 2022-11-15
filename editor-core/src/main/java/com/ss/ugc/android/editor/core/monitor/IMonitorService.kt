package com.ss.ugc.android.editor.core.monitor

import org.json.JSONObject

enum class MonitorState {
    OK,    //0
    ERROR  //1
}

interface IMonitorService {

    fun monitorStatusRate(serviceName: String, state: MonitorState, extra: JSONObject? = null)
}