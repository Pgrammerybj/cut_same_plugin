package com.cutsame.ui.template.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cutsame.solution.template.model.TemplateItem

class TemplatePreviewModel(application: Application) : AndroidViewModel(application) {
    private val _pageStateIdle = MutableLiveData<Boolean>()

    private val _selectedPage = MutableLiveData<TemplateItem>()

    fun selectPage(item: TemplateItem) {
        _selectedPage.value = item
    }

    fun updatePageState(idle: Boolean) {
        if (_pageStateIdle.value != idle) {
            _pageStateIdle.value = idle
        }
    }
}