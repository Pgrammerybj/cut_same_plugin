package com.ss.ugc.android.editor.track.fuctiontrack.audio

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.core.content.ContextCompat
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.PlayController
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.TrackParams
import com.ss.ugc.android.editor.track.fuctiontrack.audio.AudioItemHolder.Companion.RECORD_WAVE_COLOR
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.widget.HorizontalScrollContainer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val MUSIC_TRACK_BG_COLOR = if(ThemeStore.getCustomAudioBgColor()!=null)
    ContextCompat.getColor(TrackSdk.application,ThemeStore.getCustomAudioBgColor()!!)
else
    Color.parseColor("#343434")
val RECORD_TRACK_BG_COLOR = if(ThemeStore.getCustomRecordBgColor()!=null)
    ContextCompat.getColor(TrackSdk.application,ThemeStore.getCustomRecordBgColor()!!)
else
    Color.parseColor("#343434")
val SOUND_TRACK_BG_COLOR = Color.parseColor("#43a3d1")

class AudioTrackAdapter(
    private val activity: AppCompatActivity,
    trackGroup: TrackGroup,
    private val scrollContainer: HorizontalScrollContainer,
    playController: PlayController,
    frameDelegate: KeyframeStateDelegate
) : BaseTrackAdapter(trackGroup, scrollContainer, playController, frameDelegate) {


    private val paint = Paint()
    private val textBackgroundPaint = Paint()
    private val wavePointPaint = Paint()
    var isRecord = false
    private val recordingAnim = ValueAnimator.ofFloat(0F, 1F)

    init {
        paint.isAntiAlias = true
        paint.color = RECORD_TRACK_BG_COLOR
        paint.textSize = AudioItemHolder.WAVE_TEXT_SIZE

        textBackgroundPaint.color = Color.parseColor("#66101010")
        textBackgroundPaint.isAntiAlias = true

        wavePointPaint.color = if(ThemeStore.getCustomRecordWaveColor()!=null)
           ContextCompat.getColor(activity,ThemeStore.getCustomRecordWaveColor()!!)
        else
            RECORD_WAVE_COLOR

        //wavePointPaint.color = AudioItemHolder.RECORD_WAVE_COLOR
        wavePointPaint.style = Paint.Style.STROKE
        wavePointPaint.strokeWidth =  SizeUtil.dp2px(1f).toFloat()
        wavePointPaint.isAntiAlias = true
        wavePointPaint.strokeCap = Paint.Cap.ROUND

        recordingAnim.repeatCount = ValueAnimator.INFINITE
        recordingAnim.duration = 100
        recordingAnim.addUpdateListener {
            trackGroup.invalidate()
        }

    }

    private var startTimestamp = 0L
    private var rectTop = 0F
    private var rectBottom = 0F
    private var scrollByVerticalPx = 0
    private var recordIndex = 0
    private var recordCount  = 0

    private var recordNameBounds = Rect()


    override fun updateSelected(data: Pair<NLETrackSlot, TrackParams>?, dataUpdate: Boolean) {
        super.updateSelected(data, dataUpdate)
        if (!dataUpdate) {
            requestSelectedItemOnScreen(data)
//            viewModel.setSelected(data?.first?.id)
        }
    }

    override fun getDesireHeight(trackCount: Int): Int {
        val recordingTrackIndex = if (isRecord) recordIndex else -1
        return if (trackCount <= recordingTrackIndex) {
            super.getDesireHeight(trackCount) + getItemHeight()
        } else {
            super.getDesireHeight(trackCount)
        }
    }


    var recordWavePoints: List<Float>? = null

    val AUDIO_RECORD_NAME: String = activity.resources.getString(R.string.ck_record_insert)

    private val   waveUpPath = Path()
    override fun drawDecorate(canvas: Canvas) {
        val wavePoints = recordWavePoints ?: return
        if (wavePoints.isEmpty() || !isRecord) return
        val waveLength = (wavePoints.size * 30 * timelineScale)
        val targetScrollX = (startTimestamp * timelineScale + waveLength + 33 * timelineScale) * 1
        val scrollStepY = when {
            scrollByVerticalPx == 0 -> 0
            scrollByVerticalPx < 0 -> max(
                -((getItemHeight() + getItemMargin()) / 3),
                scrollByVerticalPx
            )
            else -> min(
                (getItemHeight() + getItemMargin()) / 3,
                scrollByVerticalPx
            )
        }
        scrollByVerticalPx -= scrollStepY
        // 录音超长可能需要补黑场，为了避免频繁调用补黑场接口的耗时，
        // 这里先给一个假的maxScrollX，等录音结束之后再调用补黑场接口。
        scrollContainer.assignMaxScrollX(targetScrollX.toInt())
//        Log.d("audio-record", "scroll y is $scrollStepY")
        scrollBy(targetScrollX.toInt() - scrollX, scrollStepY, true)

        val paddingHorizontal = TrackGroup.getPaddingHorizontal().toFloat()
        val left: Float = startTimestamp * timelineScale +
                paddingHorizontal + AudioItemHolder.DIVIDER_WIDTH
        val right = paddingHorizontal + targetScrollX

        paint.color = RECORD_TRACK_BG_COLOR
        canvas.drawRect(left, rectTop, right, rectBottom, paint)

        canvas.save()
        canvas.clipRect(left, rectTop, right, rectBottom)
        canvas.translate(left, rectTop)
        paint.color = Color.WHITE
        val text = String.format(AUDIO_RECORD_NAME, recordCount)
        paint.getTextBounds(text, 0, text.length, recordNameBounds)

        val waveWidth = 5
        var waveStart = 0F

        waveUpPath.moveTo(0F, getItemHeight() / 2F)
        wavePoints.forEach {
            waveUpPath.moveTo(waveStart, getItemHeight() / 2 + it * SizeUtil.dp2px(13F))
            waveUpPath.lineTo(waveStart, getItemHeight() / 2 - it * SizeUtil.dp2px(13F))
            waveStart += waveWidth
        }
        canvas.drawPath(waveUpPath, wavePointPaint)
        canvas.drawRect(
            0F,
            (getItemHeight() - SizeUtil.dp2px(16F)).toFloat(),
            recordNameBounds.width() + AudioItemHolder.TEXT_LEFT_MARGIN * 2,
            getItemHeight().toFloat(),
            textBackgroundPaint
        )
        val y =
            getItemHeight() - (abs(paint.ascent()) - paint.descent()) / 2 - AudioItemHolder.TEXT_BOTTOM_MARGIN
        canvas.drawText(text, AudioItemHolder.TEXT_LEFT_MARGIN, y, paint)
        canvas.restore()
        waveUpPath.reset()
    }

    override fun createHolder(parent: ViewGroup, index: Int) =
        AudioItemHolder(activity)

    override fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        offsetInTimeline: Long,
        currPosition: Long
    ) {

    }

    override fun onClip(
        slot: NLETrackSlot,
        start: Long,
        timelineOffset: Long,
        duration: Long
    ) {
        trackGroup.trackGroupActionListener?.onClip(slot, start, timelineOffset, duration)
    }


    fun onRecordStart(recording: Boolean, recordPosition: Long, recordLayer: Int, recordCount : Int) {
        if (isRecord) return
        this.recordCount = recordCount
        isRecord = recording
        recordIndex = recordLayer
        startTimestamp = recordPosition
        // 在录制过程中要将item及其topMargin和bottomMargin都显示出来
        rectTop = (recordIndex * (getItemHeight() + getItemMargin())).toFloat()
        rectBottom = rectTop + getItemHeight()
        scrollByVerticalPx =
            trackGroup.getScrollByVerticalPxOfRequestOnScreen(recordIndex)
        Log.d("audio-record", "scroll y  first $scrollByVerticalPx   $recordIndex")
        if (rectTop >= trackGroup.measuredHeight) {
            trackGroup.invalidate()
        }
        recordingAnim.start()
    }

}
