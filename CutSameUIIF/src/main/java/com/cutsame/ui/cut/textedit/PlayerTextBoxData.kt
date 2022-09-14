package com.cutsame.ui.cut.textedit

import android.graphics.RectF

class PlayerTextBoxData {

    var originCanvasWidth: Int = 0//视频画布原始宽度
    var originCanvasHeight: Int = 0//视频画布原始高度
    var originSurfaceWidth: Int = 0//视频Surface宽度
    var originSurfaceHeight: Int = 0//视频Surface高度
    var scaleSizeW: Float = 0.toFloat()//视频做缩放动画的scale值
    var scaleSizeH: Float = 0.toFloat()
    var transY: Float = 0.toFloat()//缩放动画的平移值
    var leftRightMargin: Int = 0 //视频画布两边的margin
    var topMargin: Int = 0 //视频画布顶部的margin

    var angle: Float = 0.toFloat()
    var width: Float = 0.toFloat()
    var height: Float = 0.toFloat()
    var x: Float = 0.toFloat()
    var y: Float = 0.toFloat()

    companion object {

        fun createData(rectF: RectF?, angle: Float): PlayerTextBoxData? {
            if (rectF == null) {
                return null
            }
            val boxData = PlayerTextBoxData()
            boxData.angle = angle
            boxData.width = rectF.right - rectF.left
            boxData.height = rectF.bottom - rectF.top
            boxData.x = rectF.left
            boxData.y = rectF.top
            return boxData
        }
    }

}
