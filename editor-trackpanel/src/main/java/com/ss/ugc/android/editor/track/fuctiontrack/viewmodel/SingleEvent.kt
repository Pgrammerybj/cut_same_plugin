package com.ss.ugc.android.editor.track.fuctiontrack.viewmodel

open class SingleEvent {
    /**
     * Can only judge once
     */
    var isHandled: Boolean = false
        private set
        get() {
            if (!field) {
                field = true
                return false
            }
            return true
        }
}




class FadeState(
    val drawFade: Boolean,
    val fadeInDuration: Long = 0L,
    val fadeOutDuration: Long = 0L
) : SingleEvent()