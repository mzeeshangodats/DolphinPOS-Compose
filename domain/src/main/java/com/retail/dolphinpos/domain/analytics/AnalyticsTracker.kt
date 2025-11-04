package com.retail.dolphinpos.domain.analytics

import android.os.Bundle

interface AnalyticsTracker {
    fun logEvent(name: String, params: Bundle?)
    fun setUserProperty(name: String, value: String?)
}
