package com.ss.ugc.android.editor.track.frame


/**
 *  author : wenlongkai
 *  date : 2020/5/22 11:07 AM
 *  description :
 */
object LVEditAbility {

    const val TAG = "LVSandbox"



    private val mainEditAbility by lazy {
        EditAbility()
    }



    fun getEditAbility(): IEditAbility {
        return mainEditAbility
    }


}
