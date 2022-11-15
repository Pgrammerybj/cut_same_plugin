package com.volcengine.ck.cutsame_api

import android.app.Application
import android.content.Context
import android.content.Intent
import com.bytedance.news.common.service.manager.IService

/**
 *Author: gaojin
 *Time: 2022/5/6 01:25
 */

interface ICutSameExternalService : IService {
    fun getTemplateUIIntent(context: Context): Intent?
    fun initCutSame(application: Application)
}