package com.retail.dolphinpos.presentation.features.ui.setup

import androidx.lifecycle.ViewModel
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HardwareSetupViewModel @Inject constructor() : ViewModel() {

    private val _menus = MutableStateFlow<List<BottomMenu>>(emptyList())
    val menus: StateFlow<List<BottomMenu>> = _menus.asStateFlow()

    private val _selectedMenuIndex = MutableStateFlow(0)
    val selectedMenuIndex: StateFlow<Int> = _selectedMenuIndex.asStateFlow()

    init {
        loadMenus()
    }

    private fun loadMenus() {
        // Setup screen navigation menus (Home + Setup types)
        _menus.value = listOf(
            BottomMenu(menuName = "Home", destinationId = R.id.homeScreen),
            BottomMenu(menuName = "Business Information", destinationId = R.id.setupScreen),
            BottomMenu(menuName = "Credit Card Processing", destinationId = R.id.setupScreen),
            BottomMenu(menuName = "Customer Display", destinationId = R.id.setupScreen),
            BottomMenu(menuName = "Printer", destinationId = R.id.setupScreen),
            BottomMenu(menuName = "Dual Pricing", destinationId = R.id.setupScreen)
        )
        // Set default selection to "Printer Setup" (index 1, since Home is index 0)
        _selectedMenuIndex.value = 1
    }

    fun selectMenu(index: Int) {
        _selectedMenuIndex.value = index
    }
}
