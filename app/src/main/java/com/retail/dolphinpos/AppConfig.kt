package com.retail.dolphinpos

open class AppConfig {
    companion object {
        val isDevMode: Boolean
            get() = BuildConfig.DEBUG
    }
}