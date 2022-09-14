package com.ss.ugc.android.editor.track.fuctiontrack.keyframe

import android.os.Bundle

open class TemplateData {
    @Transient
    internal var extensionBundle: Bundle = Bundle()
        get() {
            if (field == null) {
                field = Bundle()
            }
            return field
        }
}
