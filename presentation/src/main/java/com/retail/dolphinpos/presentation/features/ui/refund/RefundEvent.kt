package com.retail.dolphinpos.presentation.features.ui.refund

import com.retail.dolphinpos.domain.model.refund.Refund

sealed class RefundEvent {
    data class RefundSuccess(val refund: Refund) : RefundEvent()
    data class RefundError(val message: String) : RefundEvent()
}

