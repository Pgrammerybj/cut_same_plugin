package com.cutsame.ui.template.viewmodel

import androidx.annotation.NonNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cutsame.solution.template.model.TemplateCategory

@Suppress("UNCHECKED_CAST")
class TemplateNetViewModelFactory : ViewModelProvider.Factory {
    @NonNull
    override fun <T : ViewModel?> create(@NonNull modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateNetModel::class.java)) {
            return TemplateNetModel() as T
        }
        throw RuntimeException("unknown class :" + modelClass.name)
    }
}

@Suppress("UNCHECKED_CAST")
class TemplateNetPageViewModelFactory(
    private val category: TemplateCategory
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TemplateNetPageModel::class.java)) {
            return TemplateNetPageModel(category) as T
        }
        throw RuntimeException("unknown class :" + modelClass.name)
    }
}