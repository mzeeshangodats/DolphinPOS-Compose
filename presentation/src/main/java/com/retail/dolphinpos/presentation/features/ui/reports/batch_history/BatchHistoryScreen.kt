package com.retail.dolphinpos.presentation.features.ui.reports.batch_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBar
import com.retail.dolphinpos.common.components.LogoutConfirmationDialog
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.common.utils.PreferenceManager
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.Loader
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BatchHistoryScreen(
    navController: NavController,
    viewModel: BatchHistoryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    BatchHistoryContent(navController = navController, viewModel = viewModel, preferenceManager = preferenceManager)
}

@Composable
fun BatchHistoryContent(
    navController: NavController,
    viewModel: BatchHistoryViewModel = hiltViewModel(),
    preferenceManager: PreferenceManager
) {
    val batches by viewModel.batches.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Get username and clock-in status from preferences
    val userName = preferenceManager.getName()
    val isClockedIn = preferenceManager.isClockedIn()
    val clockInTime = preferenceManager.getClockInTime()

    LaunchedEffect(Unit) {
        viewModel.loadBatchHistory()
    }

    // Clean up loader when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            Loader.hide()
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BatchHistoryUiEvent.ShowLoading -> Loader.show("Loading...")
                is BatchHistoryUiEvent.HideLoading -> Loader.hide()
                is BatchHistoryUiEvent.ShowError -> {
                    // Handle error
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderAppBar(
            title = "Batch History",
            onLogout = {
                showLogoutDialog = true
            },
            userName = userName,
            isClockedIn = isClockedIn,
            clockInTime = clockInTime
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                BaseText(
                    text = "Loading batch history...",
                    color = Color.Gray,
                    fontSize = 16f,
                    fontFamily = GeneralSans
                )
            }
        } else {
            if (batches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    BaseText(
                        text = "No batch history available",
                        color = Color.Gray,
                        fontSize = 16f,
                        fontFamily = GeneralSans
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(batches) { batch ->
                        BatchHistoryItem(batch = batch)
                    }
                }
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = {
                    showLogoutDialog = false
                    // Handle logout - navigate to login
                    navController.navigate("pinCode") {
                        popUpTo(0) { inclusive = false }
                    }
                },
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}

@Composable
fun BatchHistoryItem(batch: BatchHistoryItemData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BaseText(
                    text = batch.batchNo,
                    fontSize = 16f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                BaseText(
                    text = if (batch.isClosed) "Closed" else "Open",
                    fontSize = 14f,
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Medium,
                    color = if (batch.isClosed) Color.Gray else colorResource(id = R.color.primary)
                )
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray,
                thickness = 1.dp
            )

            InfoRow("Starting Cash:", "$${String.format("%.2f", batch.startingCashAmount)}")
            batch.closingCashAmount?.let {
                InfoRow("Closing Cash:", "$${String.format("%.2f", it)}")
            }
            InfoRow("Started:", formatTimestamp(batch.startedAt))
            batch.closedAt?.let {
                InfoRow("Closed:", formatTimestamp(it))
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BaseText(
            text = label,
            fontSize = 14f,
            fontFamily = GeneralSans,
            color = Color.Gray
        )
        BaseText(
            text = value,
            fontSize = 14f,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

