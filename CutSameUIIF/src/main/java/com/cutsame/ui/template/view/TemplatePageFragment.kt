package com.cutsame.ui.template.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cutsame.solution.template.model.TemplateCategory
import com.cutsame.ui.R
import com.cutsame.ui.template.TemplateFeedPreviewActivity
import com.cutsame.ui.template.TemplateListHolder
import com.cutsame.ui.template.viewmodel.TemplateNetPageModel
import com.cutsame.ui.template.viewmodel.TemplateNetPageViewModelFactory

class TemplatePageFragment : Fragment() {
    private val ARGUMENT_CATEGORY = "arg_category"

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var feedRecyclerView: RecyclerView
    private lateinit var templateRecyclerAdapter: TemplateRecyclerAdapter
    private lateinit var templateNetPageModel: TemplateNetPageModel

    companion object {
        fun newInstance(templateCategory: TemplateCategory) =
            TemplatePageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARGUMENT_CATEGORY, templateCategory)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val category = arguments?.getParcelable<TemplateCategory>(ARGUMENT_CATEGORY)
            ?: throw Exception("no category")

        templateNetPageModel =
            ViewModelProviders.of(this, TemplateNetPageViewModelFactory(category))
                .get(TemplateNetPageModel::class.java)
}

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.page_template_feed_net, container, false)
        initView(rootView)
        initComponent()
        return rootView
    }

    override fun onStart() {
        super.onStart()
        initListener()
    }

    private fun initView(rootView: View) {
        swipeRefreshLayout = rootView.findViewById(R.id.refreshLayoutFeed)
        feedRecyclerView = rootView.findViewById(R.id.feedRv)
        feedRecyclerView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        feedRecyclerView.itemAnimator = null

        val feedLayoutManager =
            androidx.recyclerview.widget.StaggeredGridLayoutManager(
                2,
                androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
            )
        feedLayoutManager.gapStrategy =
            androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE
        feedRecyclerView.layoutManager = feedLayoutManager

        templateRecyclerAdapter = TemplateRecyclerAdapter(templateNetPageModel)
        feedRecyclerView.adapter = templateRecyclerAdapter
    }

    private fun initComponent() {
        templateNetPageModel.templateItems.observe(viewLifecycleOwner) {
            swipeRefreshLayout.isRefreshing = false
            templateRecyclerAdapter.updateItems(it!!)
            templateRecyclerAdapter.notifyDataSetChanged()
        }

        templateNetPageModel.hasMoreTemplate.observe(viewLifecycleOwner) {
            templateRecyclerAdapter.hasMore = it!!
        }

        templateNetPageModel.loadFeedList(true)
    }

    private fun initListener() {
        swipeRefreshLayout.setOnRefreshListener {
            templateNetPageModel.loadFeedList(true)
        }

        templateRecyclerAdapter.setItemClickListener(object :
            TemplateRecyclerAdapter.ItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                TemplateListHolder.setTemplateList(templateNetPageModel.templateItems)
                TemplateListHolder.nextCursor = templateNetPageModel.nextCursor
                TemplateFeedPreviewActivity.launch(
                    requireActivity(),
                    templateNetPageModel.category,
                    view,
                    position
                )
            }
        })
    }

}