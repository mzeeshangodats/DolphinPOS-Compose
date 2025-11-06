package com.retail.dolphinpos.presentation.features.ui.reports

import androidx.lifecycle.ViewModel
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu
import com.retail.dolphinpos.presentation.R
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
            BottomMenu(
                menuName = "Home",
                destinationId = R.id.homeScreen
            ),
            BottomMenu(
                menuName = "Batch Report",
                destinationId = R.id.batchReportScreen
            ),
            BottomMenu(
                menuName = "Batch History",
                destinationId = R.id.batchHistoryScreen
            ),
            BottomMenu(
                menuName = "Transaction Activity",
                destinationId = R.id.transactionActivityScreen
            )
        )
        // Set default selection to "Sales Report" (index 1, since Home is index 0)
        _selectedMenuIndex.value = 1
    }

    fun selectMenu(index: Int) {
        _selectedMenuIndex.value = index
    }

    fun resetToDefault() {
        _selectedMenuIndex.value = 1 // Reset to Batch Report (index 1)
    }
}
