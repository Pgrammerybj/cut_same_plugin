package com.angelstar.ola.player

import android.os.Bundle
import com.angelstar.ola.ITemplateVideoStateListener
import com.ss.ugc.android.editor.core.NLEEditorContext
import com.ss.ugc.android.editor.main.IEditorViewStateListener

interface IPlayerActivityDelegate {

    var nleEditorContext: NLEEditorContext?

    var viewStateListener: ITemplateVideoStateListener?

    fun onCreate(savedInstanceState: Bundle?)

    fun onResume()

    fun onPause()

    fun play()

    fun pause()

    fun onDestroy()
}