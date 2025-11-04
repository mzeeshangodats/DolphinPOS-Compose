package com.retail.dolphinpos.domain.usecases.analytics

import com.retail.dolphinpos.domain.analytics.AnalyticsTracker
import javax.inject.Inject

class GetAnalyticsTrackerUseCase @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) {
    operator fun invoke(): AnalyticsTracker = analyticsTracker
}
