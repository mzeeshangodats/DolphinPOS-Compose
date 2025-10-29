package com.retail.dolphinpos.presentation.features.ui.reports

import androidx.lifecycle.ViewModel
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor() : ViewModel() {
    
    private val _menus = MutableStateFlow<List<BottomMenu>>(emptyList())
    val menus: StateFlow<List<BottomMenu>> = _menus.asStateFlow()

    private val _selectedMenuIndex = MutableStateFlow(0)
    val selectedMenuIndex: StateFlow<Int> = _selectedMenuIndex.asStateFlow()

    init {
        loadMenus()
    }

    private fun loadMenus() {
        // Reports screen navigation menus (Home + Report types)
        _menus.value = listOf(
            BottomMenu(menuName = "Home", destinationId = com.retail.dolphinpos.presentation.R.id.homeScreen),
            BottomMenu(menuName = "Sales Report", destinationId = com.retail.dolphinpos.presentation.R.id.reportsScreen),
            BottomMenu(menuName = "Inventory Report", destinationId = com.retail.dolphinpos.presentation.R.id.reportsScreen),
            BottomMenu(menuName = "Transaction Report", destinationId = com.retail.dolphinpos.presentation.R.id.reportsScreen)
        )
        // Set default selection to "Sales Report" (index 1, since Home is index 0)
        _selectedMenuIndex.value = 1
    }

    fun selectMenu(index: Int) {
        _selectedMenuIndex.value = index
    }
}
