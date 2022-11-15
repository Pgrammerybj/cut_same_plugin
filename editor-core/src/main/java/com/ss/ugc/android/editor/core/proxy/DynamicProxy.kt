package com.ss.ugc.android.editor.core.proxy

import com.google.gson.Gson
import com.ss.ugc.android.editor.core.EditorCoreInitializer
import com.ss.ugc.android.editor.core.monitor.IMonitorService
import com.ss.ugc.android.editor.core.monitor.MonitorState
import com.ss.ugc.android.editor.core.utils.DLog
import org.json.JSONObject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import kotlin.jvm.Throws

class DynamicProxy constructor(private val target: Any) : InvocationHandler {

    companion object {
        const val TAG = "DynamicProxy"
    }

    private var monitorService: IMonitorService? = EditorCoreInitializer.instance.monitorService

    private val gson = Gson()

    @Throws(Throwable::class)
    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        var result: Any? = null
        var state = MonitorState.OK
        var costMills: Long = 0
        try {
            val startTimeMills = System.currentTimeMillis()
            result = method?.invoke(target, *(args ?: emptyArray()))
            costMills = System.currentTimeMillis() - startTimeMills
            DLog.d(TAG, "invoke-->after, costMills = $costMills")
        } catch (th: Throwable) {
            DLog.e(TAG, th)
            state = MonitorState.ERROR
        }
        method?.apply {
            val extra = JSONObject()
            args?.also {
                extra.put("parameters", gson.toJson(it))
            }
            result?.also {
                extra.put("return_value", gson.toJson(it))
            }
            extra.put("elapsed_time", costMills)
            //todo 改成子线程调用
            monitorService?.monitorStatusRate(method.name, state, extra)
        }
        return result
    }
}