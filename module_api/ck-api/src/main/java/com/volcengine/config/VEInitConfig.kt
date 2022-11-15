package com.volcengine.config

/**
 *Author: gaojin
 *Time: 2022/9/26 21:14
 */

object VEInitConfig {
    var ckLicenseName: String = ""
    var audioKey: String = ""
    var audioToken: String = ""


    fun checkValidate() {
        if (ckLicenseName.isEmpty() || audioKey.isEmpty() || audioToken.isEmpty()) {
            throw IllegalArgumentException("SDK Auth Params is invalidate")
        }
    }
}