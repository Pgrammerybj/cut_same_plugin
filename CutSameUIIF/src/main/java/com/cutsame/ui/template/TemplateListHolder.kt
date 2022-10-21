package com.cutsame.ui.template

import androidx.lifecycle.MutableLiveData
import com.cutsame.solution.template.model.TemplateItem
import java.lang.ref.WeakReference

object TemplateListHolder {
    private var templateListWeakRef: WeakReference<MutableLiveData<List<TemplateItem>>>? = null
    var nextCursor = 0

    fun setTemplateList(list: MutableLiveData<List<TemplateItem>>) {
        templateListWeakRef = WeakReference(list)
    }

    fun getTemplateList(): MutableLiveData<List<TemplateItem>>? {
        return templateListWeakRef?.get()
    }
}