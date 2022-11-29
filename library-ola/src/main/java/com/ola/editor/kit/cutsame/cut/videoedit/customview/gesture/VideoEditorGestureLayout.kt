package com.ola.editor.kit.cutsame.cut.videoedit.customview.gesture

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.core.view.GestureDetectorCompat

open class VideoEditorGestureLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    ScaleGestureDetector.OnScaleGestureListener,
    View.OnTouchListener {
    /**
     * 给外部的统一回调
     */
    protected var mOnGestureListener: OnGestureListener? = null

    /**
     * 内部监听
     */
    private lateinit var mGestureDetector: GestureDetectorCompat
    private lateinit var mScaleGestureDetector: ScaleGestureDetector
    private lateinit var mRotationGestureDetector: RotateGestureDetector
    private lateinit var mMoveGestureDetector: MoveGestureDetector

    private var mScaleFactor = 1f
    private var mDoubleTapSlopSquare: Float = 0.toFloat()
    private var mFirstUpX: Float = 0.toFloat()
    private var mFirstUpY: Float = 0.toFloat()
    private var isTwoPoint = false

    private val mSimpleOnRotateGestureListener =
        object : RotateGestureDetector.SimpleOnRotateGestureListener() {
            override fun onRotate(detector: RotateGestureDetector): Boolean {
                val curRotate = detector.rotationDegreesDelta
                if (mOnGestureListener != null) {
                    mOnGestureListener!!.onRotation(curRotate)
                }
                return true
            }

            override fun onRotateEnd(detector: RotateGestureDetector) {
                val curRotate = detector.rotationDegreesDelta
                if (mOnGestureListener != null) {
                    mOnGestureListener!!.onRotationEnd(curRotate)
                }
            }

            override fun onRotateBegin(detector: RotateGestureDetector): Boolean {
                if (mOnGestureListener != null) {
                    mOnGestureListener!!.onRotationBegin(detector)
                }
                return true
            }
        }

    private val mSimpleOnMoveGestureListener = object : MoveGestureDetector.OnMoveGestureListener {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            if (mOnGestureListener != null) {
                mOnGestureListener!!.onMove(detector)
            }
            return true
        }

        override fun onMoveBegin(
            detector: MoveGestureDetector,
            downX: Float,
            downY: Float
        ): Boolean {
            if (mOnGestureListener != null) {
                mOnGestureListener!!.onMoveBegin(detector, downX, downY)
            }
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            if (mOnGestureListener != null) {
                mOnGestureListener!!.onMoveEnd(detector)
            }
        }
    }

    init {
        initLayout(context)
    }

    private fun initLayout(context: Context) {
        mRotationGestureDetector =
            RotateGestureDetector(context, mSimpleOnRotateGestureListener)
        mMoveGestureDetector =
            MoveGestureDetector(context, mSimpleOnMoveGestureListener)
        mGestureDetector = GestureDetectorCompat(context, this)
        mGestureDetector.setOnDoubleTapListener(this)
        mScaleGestureDetector = ScaleGestureDetector(this)
        val doubleTapSlop = ViewConfiguration.get(getContext()).scaledDoubleTapSlop.toFloat()
        mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop
        setOnTouchListener(this)
    }

    /***
     * 需要处理两个手指的时候不触发一个手指的所有事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 两个手指的事件永远传递
        mScaleGestureDetector.onTouchEvent(this, event)
        mRotationGestureDetector.onTouchEvent(event)

        val action = event.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_DOWN) {
            isTwoPoint = false
        }
        if (event.pointerCount > 1) {
            isTwoPoint = true
        }

        mMoveGestureDetector.onTouchEvent(event)

        if (!isTwoPoint) {
            mGestureDetector.onTouchEvent(event)
        }

        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        mFirstUpX = e.x
        mFirstUpY = e.y
        return false
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null || e2 == null) {
            return false
        }

        if (mOnGestureListener != null) {
            mOnGestureListener!!.onScroll(e1, e2, distanceX, distanceY)
            return true
        }
        return false
    }

    override fun onLongPress(e: MotionEvent) {
        mOnGestureListener?.onLongPress(e)
    }

    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return mOnGestureListener != null && mOnGestureListener!!.onFling(
            e1,
            e2,
            velocityX,
            velocityY
        )
    }

    fun setOnGestureListener(onGestureListener: OnGestureListener?) {
        mOnGestureListener = onGestureListener
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return mOnGestureListener != null && mOnGestureListener!!.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) {
            val width = measuredWidth
            val height = measuredHeight
            val widthEdge = width / 10
            val heightEdge = height / 10
            if (mOnGestureListener == null) {
                return false
            }

            if (!isConsideredDoubleTap2(e)) {
                return false
            }
            return if (isConsideredEdge(e, width, height, widthEdge, heightEdge)) {
                false
            } else mOnGestureListener!!.onDoubleClick(e)
        }
        return false
    }

    private fun isConsideredEdge(
        e: MotionEvent,
        width: Int,
        height: Int,
        widthEdge: Int,
        heightEdge: Int
    ): Boolean {
        // 边缘不响应双击事件, 防止拍摄过程中由于拇指误触屏幕边缘导致切换前后摄像头.
        return e.x < widthEdge || width - e.x < widthEdge ||
                e.y < heightEdge || height - e.y < heightEdge
    }

    // Similar to GestureDetector.isConsideredDoubleTap(),
    // we should guarantee that the distance between first up and second up is within touch slop.
    private fun isConsideredDoubleTap2(secondUp: MotionEvent): Boolean {
        val deltaX = secondUp.x - mFirstUpX
        val deltaY = secondUp.y - mFirstUpY
        return deltaX * deltaX + deltaY * deltaY < mDoubleTapSlopSquare
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (mOnGestureListener == null) {
            return false
        }
        val action = motionEvent.action
        when (action) {
            MotionEvent.ACTION_DOWN -> mOnGestureListener!!.onDown(motionEvent)
            MotionEvent.ACTION_POINTER_DOWN -> mOnGestureListener!!.onPointerDown()
            MotionEvent.ACTION_POINTER_UP -> mOnGestureListener!!.onPointerUp()
            MotionEvent.ACTION_UP -> mOnGestureListener!!.onUp(motionEvent)
            MotionEvent.ACTION_CANCEL -> {
                // mOnGestureListener!!.onUp(motionEvent)
            }
        }
        return false
    }

    override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
        if (mOnGestureListener != null) {
            val needContinue = mOnGestureListener!!.onScale(detector)
            if (needContinue) {
                mScaleFactor = detector.scaleFactor
            }
            return needContinue
        }
        return false
    }

    override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
        return mOnGestureListener?.onScaleBegin(detector) ?: false
    }

    override fun onScaleEnd(view: View, detector: ScaleGestureDetector) {
        mOnGestureListener?.onScaleEnd(mScaleFactor)
    }
}
