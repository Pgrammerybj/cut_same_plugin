package com.cutsame.ui.template.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.template.TemplateLoadCallback
import com.cutsame.solution.template.model.TemplateCategory
import com.cutsame.solution.template.model.TemplateItem
import com.cutsame.ui.ApiUtil

class TemplateNetPageModel(val category: TemplateCategory) :
    ViewModel() {
    private val TEMPLATE_LIST_PATH = "/api/categorylist"

    val templateItems by lazy { MutableLiveData<List<TemplateItem>>() }
    val hasMoreTemplate by lazy { MutableLiveData<Boolean>() }
    var nextCursor = 0
    private val fetcher = CutSameSolution.getTemplateFetcher()

    /**
     * 拉取对应模板分类下的模板列表
     * @param refresh 是否是刷新数据，如果是刷新，将会清空
     */
    fun loadFeedList(refresh: Boolean = false) {
        if (refresh) nextCursor = 0
        val extraRequestParams = mutableMapOf(
            "order_id" to ApiUtil.extra_param_order_id
        )
        fetcher?.loadTemplateList(TEMPLATE_LIST_PATH, category, nextCursor, extraMap = extraRequestParams, callback = object :
            TemplateLoadCallback {
            override fun onSuccess(
                templateList: List<TemplateItem>,
                hasMore: Boolean,
                nextCursor: Int
            ) {
                this@TemplateNetPageModel.nextCursor = nextCursor
                hasMoreTemplate.postValue(hasMore)
                val oldValue = templateItems.value ?: ArrayList()
                if (refresh) {
                    templateItems.postValue(templateList)
                } else {
                    templateItems.postValue(oldValue + templateList)
                }
            }

            override fun onError(errorCode: String, errorMsg: String) {
                templateItems.postValue(emptyList())
            }
        })
    }
}