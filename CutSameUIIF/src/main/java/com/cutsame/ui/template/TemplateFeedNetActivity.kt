package com.cutsame.ui.template

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.cutsame.ui.R
import com.cutsame.ui.exten.FastMain
import com.cutsame.ui.template.view.TemplatePagerAdapter
import com.cutsame.ui.template.viewmodel.TemplateNetModel
import com.cutsame.ui.template.viewmodel.TemplateNetViewModelFactory
import kotlinx.android.synthetic.main.activity_template_feed_net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TemplateFeedNetActivity : AppCompatActivity(), CoroutineScope {
    override val coroutineContext = SupervisorJob() + FastMain
    private lateinit var templateNetModel: TemplateNetModel
    private lateinit var templatePagerAdapter: TemplatePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_template_feed_net)
        initView()
        initComponent()
    }

    override fun onStart() {
        super.onStart()
        initListener()
    }

    private fun initView() {
        templatePagerAdapter = TemplatePagerAdapter(supportFragmentManager)
        categoryViewPager.adapter = templatePagerAdapter
        tabCategory.setupWithViewPager(categoryViewPager)
    }

    private fun initComponent() {
        templateNetModel = ViewModelProviders.of(
            this@TemplateFeedNetActivity, TemplateNetViewModelFactory()
        ).get(TemplateNetModel::class.java)

        templateNetModel.categoryList.observe(this@TemplateFeedNetActivity, Observer {
            templatePagerAdapter.updateItems(it!!)
            if (!templateNetModel.isLoadSuccess) {
                categoryViewPager.visibility = View.GONE
                retryLayout.visibility = View.VISIBLE
                noCategoryTv.visibility = View.GONE
            } else {
                if (it.isNullOrEmpty()) {
                    categoryViewPager.visibility = View.GONE
                    retryLayout.visibility = View.GONE
                    noCategoryTv.visibility = View.VISIBLE
                } else {
                    categoryViewPager.visibility = View.VISIBLE
                    retryLayout.visibility = View.GONE
                    noCategoryTv.visibility = View.GONE
                }
            }
        })
    }

    private fun initListener() {
        backIv.setOnClickListener {
            finish()
        }

        retryBtn.setOnClickListener {
            templateNetModel.loadCategoryList()
        }
    }
}