package com.ss.ugc.android.editor.main.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.ss.ugc.android.editor.core.vm.EditViewModelFactory.Companion.viewModelProvider
import com.ss.ugc.android.editor.main.R
import kotlinx.android.synthetic.main.fragment_template_config_info.*

/**
 * Oct 15, 2021
 * 模版生产工具-模版信息编辑-更多设置页面
 */

class TemplateConfigInfoFragment: Fragment() {

    private lateinit var templateInfoViewModel: TemplateInfoViewModel

    companion object {
        const val FRAG_TAG = "MoreConfigFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_template_config_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        templateInfoViewModel = viewModelProvider(this).get(TemplateInfoViewModel::class.java)

        // 初始化按钮UI
        templateInfoViewModel.keepAudio.observe(viewLifecycleOwner) {
            if (it != null) {
                setSwitchUI(audio_switch, it)
            }
        }
        templateInfoViewModel.alignmentMode.observe(viewLifecycleOwner) {
            if (it != null) {
                setSwitchUI(alignment_switch, it)
            }
        }

        // 初始化监听器
        // 0 - 初始化退回监听器
        template_config_info_back.setOnClickListener {
            requireParentFragment().childFragmentManager.beginTransaction().detach(this).commit()
        }
        // 1 - 初始化保留原声监听器
        audio_switch.setOnClickListener {
            templateInfoViewModel.setKeepAudio()
        }
        // 2 - 初始化适配剪裁监听器
        alignment_switch.setOnClickListener {
            templateInfoViewModel.setAlignmentMode()
        }
    }

    private fun setSwitchUI(switch: ImageView, selected: Boolean) {
        if (selected) {
            switch.setImageResource(R.drawable.ic_greenswitch_selected)
        } else {
            switch.setImageResource(R.drawable.ic_greenswitch_unselected)
        }
    }
}
