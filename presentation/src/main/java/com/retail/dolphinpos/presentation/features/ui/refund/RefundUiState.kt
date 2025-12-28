package com.retail.dolphinpos.presentation.features.ui.refund

import com.retail.dolphinpos.domain.model.refund.Refund

data class RefundUiState(
    val isLoading: Boolean = false,
    val refund: Refund? = null,
    val error: String? = null
)

