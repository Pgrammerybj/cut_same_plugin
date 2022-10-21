package com.ss.ugc.android.editor.main.template

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bytedance.android.winnow.WinnowAdapter
import com.bytedance.ies.nle.editor_jni.NLEEditor
import com.ss.ugc.android.editor.base.draft.TemplateDraftItem
import com.ss.ugc.android.editor.base.fragment.BaseFragment
import com.ss.ugc.android.editor.base.view.EditorDialog
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.Toaster
import com.ss.ugc.android.editor.main.EditorHelper
import com.ss.ugc.android.editor.main.R
import kotlinx.android.synthetic.main.fragment_template_draft.*
import java.io.File

/**
 * 从原[ import com.ss.ugc.android.editor.main.template.TemplateDraftFragment] 迁移，没有做任何改动
 *
 * TODO , 重构模板草稿，  目前有潜在bug
 */
class TemplateDraftFragment : BaseFragment() {
    companion object {
        const val MAX_RENAME_LINES = 1
        const val TAG = "TemplateDraftFragment"
        const val HOME_TAG = "TemplateHomeFragment"
    }

    private val draftModel: TemplateDraftViewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )[TemplateDraftViewModel::class.java]
    }

    private lateinit var mAdapter: WinnowAdapter
    private var manageSelectedDrafts: ArrayList<TemplateDraftItem> = ArrayList()
    private var draftRenameInput: String? = null

    override fun getContentView(): Int = R.layout.fragment_template_draft

    private fun initManageDraftListener() {
        draftModel.manageDraftEvent.postValue(ManageState.NONE)
        btnTemplateManage.setOnClickListener {
            draftModel.manageDraftEvent.apply {
                if (this.value != null) {
                    if (this.value == ManageState.NONE) {
                        this.postValue(ManageState.MANAGE_SELECT_EMPTY)
                    } else if (this.value == ManageState.MANAGE_SELECT_EMPTY || this.value == ManageState.MANAGE_SELECTED) {
                        this.postValue(ManageState.NONE)
                    }
                } else {
                    this.postValue(ManageState.MANAGE_SELECT_EMPTY)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initManageDraftListener()
        templateRecyclerView.apply {
            layoutManager =
                StaggeredGridLayoutManager(
                    2,
                    StaggeredGridLayoutManager.VERTICAL
                )

            mAdapter = WinnowAdapter.create(TemplateDraftHolderV2::class.java)
                .addHolderListener(object : WinnowAdapter.HolderListener<TemplateDraftHolderV2>() {

                    override fun onHolderBind(holder: TemplateDraftHolderV2) {
                        super.onHolderBind(holder)

                        val managerEvent = draftModel.manageDraftEvent.value
                        DLog.d(TAG, "onHolderBind : $managerEvent  position:${holder.bindingAdapterPosition}")
                        if (managerEvent != null) {
                            when (managerEvent) {
                                ManageState.MANAGE_SELECT_EMPTY, ManageState.MANAGE_SELECTED -> {
                                    holder.itemView.apply {
                                        holder.templateSelector.visibility = View.VISIBLE
                                        holder.templateSelector.setOnClickListener {
                                            if (manageSelectedDrafts.contains(holder.data)) {
                                                holder.templateSelector.setImageResource(R.drawable.ic_empty_select)
                                                holder.templateImageMask.visibility = View.GONE
                                                manageSelectedDrafts.remove(holder.data)
                                            } else {
                                                holder.templateSelector.setImageResource(R.drawable.ic_purple_select)
                                                holder.templateImageMask.layoutParams.height =
                                                    holder.templateImage.height
                                                holder.templateImageMask.visibility = View.VISIBLE
                                                manageSelectedDrafts.add(holder.data)
                                            }
                                            draftModel.manageDraftEvent.postValue(if (manageSelectedDrafts.isEmpty()) ManageState.MANAGE_SELECT_EMPTY else ManageState.MANAGE_SELECTED)
                                        }
                                        holder.templateMoreSettingsButton.alpha = 0.3F
                                    }
                                }
                                ManageState.NONE -> {
                                    holder.itemView.apply {
                                        holder.templateImageMask.visibility = View.GONE
                                        holder.templateSelector.visibility = View.GONE
                                        holder.templateSelector.setImageResource(R.drawable.ic_empty_select)
                                        holder.templateMoreSettingsButton.alpha = 1F
                                    }
                                }
                                else -> {}
                            }
                        }


                        holder.templateImage.setOnClickListener {
                            if (draftModel.manageDraftEvent.value == ManageState.NONE && activity != null) {
                                if (checkAllTemplateResourcesExist(holder.data)) {
                                    EditorHelper.onEditorWithDraftID(activity!!, holder.data.uuid)
                                } else {
                                    Toaster.show(
                                        resources.getString(R.string.delete_draft_res_not_found),
                                        Toast.LENGTH_LONG
                                    )
                                    draftModel.onDeleteDraft(holder.data)
                                    (adapter as WinnowAdapter).removeItem(holder.data)
                                    (adapter as WinnowAdapter).notifyDataSetChanged()
                                }
                            }
                        }
                        holder.templateMoreSettingsButton.setOnClickListener {
                            if (draftModel.manageDraftEvent.value == ManageState.NONE) {
                                this@TemplateDraftFragment.more_options_full_layout.apply {
                                    this.visibility = View.VISIBLE

                                    val moreOptionsPanelLayout = more_options_full_layout
                                    moreOptionsPanelLayout.visibility = View.VISIBLE
                                    draft_more_options_layout.visibility = View.VISIBLE
                                    menu_rename_tv.setOnClickListener {
                                        moreOptionsPanelLayout.visibility = View.GONE
                                        if (checkAllTemplateResourcesExist(holder.data)) {
                                            this.visibility = View.GONE
                                            configRenameEditorDialog(holder.data).show()
                                        }
                                    }
                                    menu_duplicate_tv.setOnClickListener {
                                        moreOptionsPanelLayout.visibility = View.GONE
                                        if (checkAllTemplateResourcesExist(holder.data)) {
                                            val newDraftItem =
                                                draftModel.onDuplicateDraft(holder.data)
                                            (adapter as WinnowAdapter).addItem(0, newDraftItem)
                                            (adapter as WinnowAdapter).notifyDataSetChanged()
                                        }
                                    }
                                    menu_delete_tv.setOnClickListener {
                                        EditorDialog.Builder(requireContext())
                                            .setTitle(getString(R.string.delete_drafts))
                                            .setContent(getString(R.string.delete_single_draft_body))
                                            .setConfirmText(getString(R.string.delete))
                                            .setConfirmListener(object :
                                                EditorDialog.OnConfirmListener {
                                                override fun onClick() {
                                                    (adapter as WinnowAdapter).removeItem(holder.data)
                                                    (adapter as WinnowAdapter).notifyDataSetChanged()
                                                    draftModel.onDeleteDraft(holder.data)
                                                }
                                            })
                                            .setCancelText(getString(R.string.cancel_button))
                                            .build()
                                            .show()
                                        moreOptionsPanelLayout.visibility = View.GONE
                                    }
                                    more_options_color_mask.setOnClickListener {
                                        moreOptionsPanelLayout.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    }
                })
            this.adapter = mAdapter
            draftModel.draftsLiveData.observe(viewLifecycleOwner) {
                mAdapter.updateAll(it)
            }
        }

        val manageModeDeleteListener = View.OnClickListener {
            EditorDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_drafts))
                .setContent("删除后，这${manageSelectedDrafts.size}个模板将无法复原。")
                .setConfirmText(getString(R.string.delete))
                .setConfirmListener(object : EditorDialog.OnConfirmListener {
                    override fun onClick() {
                        for (item in manageSelectedDrafts) {
                            mAdapter.removeItem(item)
                            mAdapter.notifyDataSetChanged()
                        }
                        btnTemplateManage.text = getString(R.string.manage_template_draft_text)
                        manage_mode_delete_layout.visibility = View.GONE
                        for (draft in manageSelectedDrafts) {
                            draftModel.onDeleteDraft(draft)
                        }
                        draftModel.manageDraftEvent.postValue(ManageState.NONE)
                    }
                })
                .setCancelText(getString(R.string.cancel_button))
                .build()
                .show()
        }

        draftModel.manageDraftEvent.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it) {
                    ManageState.NONE -> {
                        btnTemplateManage.text = getString(R.string.manage_template_draft_text)
                        manage_mode_delete_layout.visibility = View.GONE
                        manageSelectedDrafts.clear()
                        mAdapter.notifyDataSetChanged()
                    }
                    ManageState.MANAGE_SELECT_EMPTY -> {
                        btnTemplateManage.text =
                            getString(R.string.cancel_text)
                        val trashIcon =
                            requireContext().getDrawable(R.drawable.ic_trash)?.mutate()?.apply {
                                setTint(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.transparent_30p_white
                                    )
                                )
                            }
                        delete_tv.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            trashIcon,
                            null,
                            null
                        )
                        delete_tv.setTextColor(resources.getColor(R.color.transparent_50p_white))
                        manage_mode_delete_layout.visibility = View.VISIBLE
                        delete_tv.setOnClickListener(null)
                        mAdapter.notifyDataSetChanged()
                    }
                    ManageState.MANAGE_SELECTED -> {
                        delete_tv.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            R.drawable.ic_trash,
                            0,
                            0
                        )
                        delete_tv.setTextColor(resources.getColor(R.color.white))
                        delete_tv.setOnClickListener(manageModeDeleteListener)
                    }
                }
            }
        }

        btnBack.setOnClickListener {
            closeFragment()
        }
    }

    private fun checkAllTemplateResourcesExist(templateDraftItem: TemplateDraftItem): Boolean {
        val tempEditor = NLEEditor()
        tempEditor.restore(templateDraftItem.draftData)

        for (node in tempEditor.allResources) {
            if (node.resourceFile?.isNotBlank() == true && !File(node.resourceFile).exists()) {
                Toaster.show(getString(R.string.draft_resource_gone_tv), Toast.LENGTH_LONG)
                mAdapter.removeItem(templateDraftItem)
                return false
            }
        }
        return true
    }

    private fun configRenameEditorDialog(item: TemplateDraftItem): EditorDialog {
        return EditorDialog.Builder(requireContext())
            .setTitle(getString(R.string.rename_draft_toast_header))
            .setConfirmText(getString(R.string.done_button))
            .setConfirmListener(object : EditorDialog.OnConfirmListener {
                override fun onClick() {
                    if (draftRenameInput.isNullOrBlank()) {
                        Toaster.show(
                            resources.getString(R.string.empty_draft_name_error),
                            Toast.LENGTH_LONG
                        )
                        return
                    }
                    draftModel.onRenameDraft(item.uuid, draftRenameInput)
                    mAdapter.updateItem(item)
                }
            })
            .setCancelText(getString(R.string.cancel_button))
            .setCancelListener(object : EditorDialog.OnCancelListener {
                override fun onClick() {
                    draftRenameInput = null
                }
            })
//            .setEnableEditText(true, MAX_RENAME_LINES, TemplateInfoFragment.MAX_NAME_INPUT, true)
            .setTextInputChangeListener(object : EditorDialog.OnTextInputChangeListener() {
                override fun onInputChange(input: String?) {
                    draftRenameInput = input
                }
            })
            .setEditTextDefaultText(item.name)
            .build()
    }

    fun closeFragment() {
        activity?.supportFragmentManager?.apply {
            findFragmentByTag(HOME_TAG)?.onResume()
        }
        activity?.supportFragmentManager?.beginTransaction()
            ?.remove(this)
            ?.commit()
    }
}