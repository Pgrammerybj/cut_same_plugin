package com.volcengine.ckapi

import com.bytedance.news.common.service.manager.IService

/**
 *Author: gaojin
 *Time: 2022/5/5 19:22
 */

interface ICKExternalService : IService {
    fun getEntranceClass(): Class<*>
}