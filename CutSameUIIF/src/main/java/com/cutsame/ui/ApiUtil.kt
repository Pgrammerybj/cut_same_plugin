package com.cutsame.ui

enum class Environment {
    PRODUCT_CN,
    PRODUCT_OVER_SEA,
    BOE_CN
}

object ApiUtil {
    var ENV: Environment = Environment.PRODUCT_CN

    val host = when (ENV) {
        Environment.PRODUCT_CN -> "http://common.voleai.com"
        Environment.PRODUCT_OVER_SEA -> "http://ck-common.byteintl.com"
        Environment.BOE_CN -> "http://common.voleai.com"
    }

    val extra_param_order_id: String
        get() = "7117128998756794382"
}