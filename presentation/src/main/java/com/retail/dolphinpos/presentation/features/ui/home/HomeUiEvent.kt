package com.retail.dolphinpos.presentation.features.ui.home

sealed class HomeUiEvent {
    object ShowLoading : HomeUiEvent()
    object HideLoading : HomeUiEvent()
    data class ShowError(val message: String) : HomeUiEvent()
    data class HoldCartSuccess(val message: String) : HomeUiEvent()
    data class OrderCreatedSuccessfully(val message: String) : HomeUiEvent()
    data class PopulateCategoryList(val categoryList: List<com.retail.dolphinpos.domain.model.home.catrgories_products.CategoryData>) : HomeUiEvent()
}
