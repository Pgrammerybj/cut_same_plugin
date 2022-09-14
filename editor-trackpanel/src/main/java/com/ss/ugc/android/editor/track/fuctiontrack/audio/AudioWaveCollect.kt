package com.ss.ugc.android.editor.track.fuctiontrack.audio

data class AudioWaveCollect(
    val wavePoints: List<List<Pair<Long, Float>>>,
    val beats: Set<Long>
) {
    internal fun isEmpty() = wavePoints.isEmpty() && beats.isEmpty()
}
