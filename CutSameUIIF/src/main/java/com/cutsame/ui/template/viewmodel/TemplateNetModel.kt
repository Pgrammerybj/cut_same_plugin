package com.cutsame.ui.template.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.template.CategoryLoadCallback
import com.cutsame.solution.template.TemplateFetcher
import com.cutsame.solution.template.model.TemplateCategory
import com.cutsame.ui.ApiUtil

class TemplateNetModel : ViewModel() {
    private val CATEGORY_LIST_PATH = "/api/categorylistinfo"

    private val fetcher : TemplateFetcher? = CutSameSolution.getTemplateFetcher()

    var isLoadSuccess: Boolean = true

    val categoryList by lazy {
        MutableLiveData<List<TemplateCategory>>()
    }

    init {
        loadCategoryList()
    }

    /**
     * 拉取模板分类
     */
    fun loadCategoryList() {
        val extraRequestParams = mutableMapOf(
            "order_id" to ApiUtil.extra_param_order_id
        )
        Log.d("TemplateNetModel", "loadCategoryList order_id: ${ApiUtil.extra_param_order_id}")
        fetcher?.loadCategoryList(CATEGORY_LIST_PATH, extraMap = extraRequestParams, callback = object : CategoryLoadCallback {
            override fun onSuccess(categoryList: List<TemplateCategory>) {
                isLoadSuccess = true
                this@TemplateNetModel.categoryList.postValue(categoryList)
            }

            override fun onError(errorCode: String, errorMsg: String) {
                isLoadSuccess = false
                this@TemplateNetModel.categoryList.postValue(emptyList())
            }
        })
    }

    override fun onCleared() {
        fetcher?.stopFetcher()
        super.onCleared()
    }

}