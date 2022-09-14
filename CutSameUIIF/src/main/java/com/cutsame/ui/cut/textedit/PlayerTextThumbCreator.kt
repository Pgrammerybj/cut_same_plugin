package com.cutsame.ui.cut.textedit

import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListenerAdapter
import com.cutsame.ui.cut.textedit.listener.PlayerTextItemThumbBitmapListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*

/**
 * 获取文字封面图，因为获取封面图是30*30，通过打印结果大约一张封面图是28K,因为模板里面一般不会有很多文字
 * 所以整体持有Bitmap的HashMap不会造成内存OOM，每个itemviewholder持有bitmap,每次展示会释放之前的bitmap, 第二次进入重新抽取
 * 因为有可能改变视频的裁剪区域，抽取封面图就不一致了，每次进入都重新抽取，之前的会在展示的时候进行释放
 */
class PlayerTextThumbCreator {
    private val handler: Handler
    private var curSize = 0
    private var dataList: List<PlayerTextEditItemData>? = null
    private var playerTextEditListenerAdapter: PlayerTextEditListenerAdapter? = null
    private var allBitmapListener: PlayerTextThumbAllBitmapListener? = null
    private val allThumbBitmap =
        HashMap<String, Bitmap>()

    fun setPlayerTextEditListenerAdapter(adapter: PlayerTextEditListenerAdapter?): PlayerTextThumbCreator {
        playerTextEditListenerAdapter = adapter
        return this
    }

    fun getThumb(
        dataList: List<PlayerTextEditItemData>,
        width: Int,
        height: Int,
        listener: PlayerTextThumbAllBitmapListener?
    ) {
        if (playerTextEditListenerAdapter == null) {
            if (listener != null) {
                listener.thumbAllBitmap(null)
                return
            }
        }
        if (dataList.isEmpty()) {
            if (listener != null) {
                listener.thumbAllBitmap(null)
                return
            }
        }
        allBitmapListener = listener
        this.dataList = dataList
        curSize = 0

        val timeAry = IntArray(dataList.size)
        for (i in timeAry.indices) {
            timeAry[i] = dataList[i].getFrameTime().toInt()
        }
        GlobalScope.launch(Dispatchers.Default) {
            delay(200) // 如果在seek/play/pause还未完成启动了抽帧，那些操作将半身不遂....
            playerTextEditListenerAdapter!!.getItemFrameBitmap(
                timeAry,
                width,
                height,
                object : PlayerTextItemThumbBitmapListener {
                    override fun frameBitmap(startTime: String?, bitmap: Bitmap?) {
                        startTime ?: return
                        if (bitmap != null && !bitmap.isRecycled) {
                            allThumbBitmap[startTime] = bitmap
                        }
                        handler.sendEmptyMessage(1000)
                    }
                }
            )
        }

    }

    fun release() {
        for (key in allThumbBitmap.keys) {
            val bitmap = allThumbBitmap[key]
            bitmap?.recycle()
        }
        allThumbBitmap.clear()
    }

    private class MainHandler internal constructor(creator: PlayerTextThumbCreator?) :
        Handler() {
        private val creatorObj: WeakReference<PlayerTextThumbCreator?> = WeakReference(creator)
        override fun handleMessage(msg: Message) {
            if (creatorObj.get() == null) {
                return
            }
            if (msg.what == 1000) {
                creatorObj.get()!!.curSize++
                if (creatorObj.get()!!.curSize == creatorObj.get()!!.dataList!!.size) {
                    //抽取完了，就退出了
                    if (creatorObj.get()!!.allBitmapListener != null) {
                        creatorObj.get()!!.allBitmapListener!!.thumbAllBitmap(creatorObj.get()!!.allThumbBitmap)
                    }
                }
            }
        }

    }

    interface PlayerTextThumbAllBitmapListener {
        fun thumbAllBitmap(bitmapHashMap: HashMap<String, Bitmap>?)
    }

    init {
        handler = MainHandler(this)
    }
}