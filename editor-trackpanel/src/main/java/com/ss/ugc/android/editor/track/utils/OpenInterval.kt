package com.ss.ugc.android.editor.track.utils





import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

fun RectF.set(left: Int, top: Int, right: Int, bottom: Int) {
    set(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

fun Canvas.drawRect(left: Int, top: Int, right: Int, bottom: Int, paint: Paint) {
    drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
}

private interface OpenInterval<T : Comparable<T>> {
    /**
     * The minimum value in the range (exclusive).
     */
    val start: T

    /**
     * The maximum value in the range (exclusive).
     */
    val endInclusive: T

    /**
     * Checks whether the specified [value] belongs to the open-interval.
     */
    operator fun contains(value: T): Boolean = value > start && value < endInclusive

    /**
     * Checks whether the range is empty.
     */
    fun isEmpty(): Boolean = start >= endInclusive
}

class IntOpenInterval(override val start: Int, override val endInclusive: Int) : OpenInterval<Int>

infix fun Int.openUntil(other: Int): IntOpenInterval {
    return IntOpenInterval(this, other)
}

class LongOpenInterval(override val start: Long, override val endInclusive: Long) :
    OpenInterval<Long>

infix fun Long.openUntil(other: Long): LongOpenInterval {
    return LongOpenInterval(this, other)
}

class FloatOpenInterval(override val start: Float, override val endInclusive: Float) :
    OpenInterval<Float>

infix fun Float.openUntil(other: Float): FloatOpenInterval {
    return FloatOpenInterval(this, other)
}
