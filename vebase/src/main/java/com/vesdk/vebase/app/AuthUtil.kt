package com.vesdk.vebase.app

class AuthUtil {

    companion object {
        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AuthUtil()
        }
    }

    private var result: Int = -1

    fun init(result: Int) {
        this.result = result
    }


    fun checkAuth(): Boolean {
        return result == 0
    }

}

