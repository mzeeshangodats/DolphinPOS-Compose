package com.retail.dolphinpos.presentation.features.ui.auth.cash_denomination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.domain.model.auth.cash_denomination.Denomination
import com.retail.dolphinpos.domain.model.auth.cash_denomination.DenominationType
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashDenominationScreen(
    navController: NavController,
    viewModel: CashDenominationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val denominations by viewModel.denominations.collectAsState()
    val totalAmount by viewModel.totalAmount.collectAsState()
    val selectedDenomination by viewModel.selectedDenomination.collectAsState()
    val currentCount by viewModel.currentCount.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.cashDenominationUiEvent.collectLatest { event ->
            when (event) {
                is CashDenominationUiEvent.NavigateToHome -> {
                    navController.navigate("home")
                }
                is CashDenominationUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK"
                    ) {}
                }
            }
        }
    }

    // Show global dialog
    DialogHandler.GlobalDialogHost()

    Box(modifier = Modifier.fillMaxSize()) {
        HeaderAppBarAuth()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp)
                .background(colorResource(id = R.color.light_grey))
        ) {

            // Left Side - Denominations Grid (60%)
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 20.dp)
            ) {
                // Total Cash Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .padding(start = 5.dp, bottom = 10.dp),
                    shape = RoundedCornerShape(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        BaseText(
                            text = stringResource(id = R.string.cash),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colorResource(id = R.color.colorTextLightMode),
                            fontWeight = FontWeight.Medium,
                            fontSize = 18F
                        )

                        BaseText(
                            text = currencyFormatter.format(totalAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            color = colorResource(id = R.color.colorTextLightMode),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24F,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Denominations Grid
                DenominationsGrid(
                    denominations = denominations,
                    selectedDenomination = selectedDenomination,
                    onDenominationClick = { denomination ->
                        viewModel.selectDenomination(denomination)
                    }
                )
            }

            // Right Side - Keypad and Controls (40%)
            Column(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BaseText(
                    text = stringResource(id = R.string.counting_cash_in_drawer),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorResource(id = R.color.colorHint),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20F
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Counter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.6f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Counter Label
                    BaseText(
                        text = selectedDenomination?.let { "Count for ${it.label}" }
                            ?: stringResource(id = R.string.counter_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorResource(id = R.color.primary),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14F
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Count Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .border(
                                1.dp,
                                colorResource(id = R.color.borderOutline),
                                RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BaseText(
                            text = currentCount,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18F
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Keypad
                CashDenominationKeypad(
                    onDigitClick = { digit ->
                        viewModel.addDigit(digit)
                    },
                    onClearClick = {
                        viewModel.clearCount()
                    },
                    onDoubleZeroClick = {
                        viewModel.addDoubleZero()
                    },
                    modifier = Modifier.fillMaxWidth(0.6f)
                )

                Spacer(modifier = Modifier.height(25.dp))

                // Start Batch Button
                BaseButton(
                    text = stringResource(id = R.string.start_batch),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(48.dp),
                    backgroundColor = colorResource(id = R.color.green_success)
                ) {
                    val batchNo = viewModel.generateBatchNo()
                    viewModel.startBatch(batchNo)
                }

//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Open Cash Drawer Button
//                BaseButton(
//                    text = stringResource(id = R.string.open_cash_drawer),
//                    modifier = Modifier
//                        .fillMaxWidth(0.5f)
//                        .height(48.dp),
//                    backgroundColor = Color.Gray,
//                    fontSize = 14
//                ) {
//                    // Handle open cash drawer
//                }
            }
        }
    }
}

@Composable
fun DenominationsGrid(
    denominations: List<Denomination>,
    selectedDenomination: Denomination?,
    onDenominationClick: (Denomination) -> Unit
) {
    val cashDenominations = denominations.filter { it.type == DenominationType.CASH }
    val coinDenominations = denominations.filter { it.type == DenominationType.COIN }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cash Section
        if (cashDenominations.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                DenominationHeader(title = stringResource(id = R.string.cash_tray))
            }

            items(cashDenominations) { denomination ->
                DenominationItem(
                    denomination = denomination,
                    isSelected = selectedDenomination?.value == denomination.value,
                    onClick = { onDenominationClick(denomination) }
                )
            }
        }

        // Coins Section
        if (coinDenominations.isNotEmpty()) {
            item(span = { GridItemSpan(3) }) {
                DenominationHeader(title = stringResource(id = R.string.coin_tray))
            }

            items(coinDenominations) { denomination ->
                DenominationItem(
                    denomination = denomination,
                    isSelected = selectedDenomination?.value == denomination.value,
                    onClick = { onDenominationClick(denomination) }
                )
            }
        }
    }
}

@Composable
fun DenominationHeader(title: String) {
    BaseText(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        color = colorResource(id = R.color.colorHint),
        fontWeight = FontWeight.SemiBold,
        fontSize = 18F,
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
fun DenominationItem(
    denomination: Denomination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorResource(id = R.color.primary)
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BaseText(
                text = denomination.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else colorResource(id = R.color.colorTextLightMode),
                fontWeight = FontWeight.Bold,
                fontSize = 16F,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            BaseText(
                text = "Count: ${denomination.count}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else colorResource(id = R.color.colorHint),
                fontWeight = FontWeight.Normal,
                fontSize = 12F,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CashDenominationKeypad(
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onDoubleZeroClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1: 7, 8, 9
        KeypadRow(
            buttons = listOf("7", "8", "9"),
            onButtonClick = onDigitClick
        )

        // Row 2: 4, 5, 6
        KeypadRow(
            buttons = listOf("4", "5", "6"),
            onButtonClick = onDigitClick
        )

        // Row 3: 1, 2, 3
        KeypadRow(
            buttons = listOf("1", "2", "3"),
            onButtonClick = onDigitClick
        )

        // Row 4: Clear, 0, 00
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Clear Button
            CashKeypadButton(
                text = stringResource(id = R.string.clear),
                onClick = onClearClick,
                modifier = Modifier.weight(1f),
                isPrimary = true
            )

            // 0 Button
            CashKeypadButton(
                text = "0",
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(1f),
                isPrimary = false
            )

            // 00 Button
            CashKeypadButton(
                text = "00",
                onClick = onDoubleZeroClick,
                modifier = Modifier.weight(1f),
                isPrimary = false
            )
        }
    }
}

@Composable
fun KeypadRow(
    buttons: List<String>,
    onButtonClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        buttons.forEach { digit ->
            CashKeypadButton(
                text = digit,
                onClick = { onButtonClick(digit) },
                modifier = Modifier.weight(1f),
                isPrimary = false
            )
        }
    }
}

@Composable
fun CashKeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) {
                colorResource(id = R.color.primary)
            } else {
                colorResource(id = R.color.pricing_calculator_clr)
            },
            contentColor = Color.White
        )
    ) {
        BaseText(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 18F
        )
    }
}
