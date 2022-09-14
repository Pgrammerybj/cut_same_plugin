package com.cutsame.ui.template

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.cutsame.solution.CutSameSolution
import com.cutsame.solution.template.model.TemplateCategory
import com.cutsame.ui.R
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.exten.FastMain
import com.cutsame.ui.template.view.TemplatePreviewPagerAdapter
import com.cutsame.ui.template.viewmodel.TemplateNetPageModel
import com.cutsame.ui.template.viewmodel.TemplateNetPageViewModelFactory
import com.cutsame.ui.template.viewmodel.TemplatePreviewModel
import kotlinx.android.synthetic.main.activity_template_feed_preview.*
import kotlinx.android.synthetic.main.activity_template_feed_preview.icBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

const val ARG_KEY_CATEGORY = "ARG_KEY_CATEGORY"
const val ARG_KEY_CLICKED_POSITION = "ARG_KEY_CLICKED_POSITION"

const val LOAD_MORE_LIMIT = 2

class TemplateFeedPreviewActivity : AppCompatActivity(), CoroutineScope {

    companion object {
        fun launch(
            activity: Activity,
            category: TemplateCategory,
            view: View,
            position: Int = 0
        ) {
            val intent = Intent(activity, TemplateFeedPreviewActivity::class.java)
            intent.putExtras(activity.intent)
            intent.putExtra(ARG_KEY_CATEGORY, category)
            intent.putExtra(ARG_KEY_CLICKED_POSITION, position)

            val options = ActivityOptionsCompat.makeClipRevealAnimation(
                view, 0, 0, view.width, view.height
            )
            ActivityCompat.startActivity(
                activity,
                intent,
                options.toBundle()
            )
        }
    }

    override val coroutineContext = SupervisorJob() + FastMain

    private val category: TemplateCategory by lazy {
        intent.getParcelableExtra<TemplateCategory>(
            ARG_KEY_CATEGORY
        ) ?: throw Exception("no category")
    }

    private val position: Int by lazy {
        intent.getIntExtra(ARG_KEY_CLICKED_POSITION, 0)
    }

    private lateinit var previewAdapter: TemplatePreviewPagerAdapter

    private val templatePreviewModel by lazy {
        CutSameSolution
        ViewModelProviders.of(this, ViewModelProvider.AndroidViewModelFactory(application))
            .get(TemplatePreviewModel::class.java)
    }

    private val pageChangeListener = object : androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            templatePreviewModel.selectPage(previewAdapter.getItemAt(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            templatePreviewModel.updatePageState(state == androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE)
        }
    }

    private val templateInfoModel by lazy {
        ViewModelProviders.of(
            this, TemplateNetPageViewModelFactory(category)
        ).get(TemplateNetPageModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()
        setContentView(R.layout.activity_template_feed_preview)
        initComponent(templateInfoModel)
        initView()
    }

    private fun initView() {
        icBack.setGlobalDebounceOnClickListener {
            finish()
        }

        viewPager.adapter = previewAdapter
        viewPager.addOnPageChangeListener(pageChangeListener)
        if (position > 0) {
            viewPager.currentItem = position
        } else if (position == 0) {
            pageChangeListener.onPageSelected(0)
        }
    }

    private fun initComponent(templateInfoModel: TemplateNetPageModel) {
        val loadedData = TemplateListHolder.getTemplateList()?.value ?: ArrayList()
        templateInfoModel.templateItems.value = loadedData
        templateInfoModel.nextCursor = TemplateListHolder.nextCursor
        previewAdapter = TemplatePreviewPagerAdapter(
            supportFragmentManager,
            loadedData
        )

        templateInfoModel.templateItems.observe(this, Observer { templateList ->
            templateList ?: return@Observer
            if (templateList.isEmpty()) return@Observer
            previewAdapter.items = templateList
        })
    }
}
