package com.retail.dolphinpos.presentation.features.ui.home

import com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData
import com.retail.dolphinpos.domain.model.home.catrgories_products.Products

sealed class HomeUiEvent {
    object ShowLoading : HomeUiEvent()
    object HideLoading : HomeUiEvent()
    data class ShowError(val message: String) : HomeUiEvent()
    data class ShowSuccess(val message: String) : HomeUiEvent()
    data class HoldCartSuccess(val message: String) : HomeUiEvent()
    data class OrderCreatedSuccessfully(val message: String) : HomeUiEvent()
    data class PopulateCategoryList(val categoryList: List<CategoryData>) : HomeUiEvent()
    data class ShowVariantSelection(val product: Products) : HomeUiEvent()
    object ClearSearchQuery : HomeUiEvent()
    object NavigateToPinCode : HomeUiEvent()
}
