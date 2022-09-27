package com.angelstar.ola.player

import android.os.Bundle
import com.ss.ugc.android.editor.core.NLEEditorContext

interface IPlayerActivityDelegate {

    var nleEditorContext: NLEEditorContext?

    fun onCreate(savedInstanceState: Bundle?)

    fun onResume()

    fun onPause()

    fun play()

    fun pause()
}