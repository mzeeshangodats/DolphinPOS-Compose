package com.retail.dolphinpos.presentation.features.ui.auth.pin_code

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarAuth
import com.retail.dolphinpos.domain.model.auth.active_user.ActiveUserDetails
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader
import kotlinx.coroutines.flow.collectLatest
import java.time.format.DateTimeFormatter

@Composable
fun PinCodeScreen(
    navController: NavController,
    viewModel: VerifyPinViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val clockHistory by viewModel.history.collectAsState()

    val pinLimit = 4
    var pinValue by remember { mutableStateOf("") }
    var activeUserDetails by remember { mutableStateOf<ActiveUserDetails?>(null) }

    val pleaseWait = stringResource(id = R.string.plz_wait)
    val dismiss = stringResource(id = R.string.dismiss)

    // Handle back button press - navigate to splash screen
    BackHandler {
        navController.navigate("splash") {
            popUpTo(0) { inclusive = true }
        }
    }

    // Handle events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.verifyPinUiEvent.collectLatest { event ->
            when (event) {
                is VerifyPinUiEvent.ShowLoading -> {
                    Loader.show(pleaseWait)
                }

                is VerifyPinUiEvent.HideLoading -> {
                    Loader.hide()
                }

                is VerifyPinUiEvent.ShowDialog -> {
                    if (event.success) {
                        DialogHandler.showDialog(
                            message = event.message,
                            buttonText = "OK",
                            iconRes = R.drawable.success_circle_icon
                        ) {
                            val currentPin = pinValue
                            pinValue = ""
                            viewModel.getClockInOutHistory(currentPin)
                        }
                    } else {
                        DialogHandler.showDialog(
                            message = event.message,
                            buttonText = dismiss
                        ) {
                            pinValue = ""
                        }
                    }
                }

                is VerifyPinUiEvent.GetActiveUserDetails -> {
                    activeUserDetails = event.activeUserDetails
                }

//                is VerifyPinUiEvent.ShowBatchClosedDialog -> {
//                    DialogHandler.showDialog(
//                        message = event.message,
//                        buttonText = "OK",
//                    ) {
//                        pinValue = ""
//                        activeUserDetails?.let { details ->
//                            navController.navigate(
//                                "cashDenomination/${details.id}/${details.storeId}/${details.registerId}"
//                            )
//                        }
//                    }
//                }

                is VerifyPinUiEvent.ShowRegisterReleasedDialog -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK",
                    ) {
                        pinValue = ""
                        navController.navigate("selectRegister") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                is VerifyPinUiEvent.NavigateToCashDenomination -> {
                    pinValue = ""
                    activeUserDetails?.let { details ->
                        navController.navigate(
                            "cashDenomination/${details.id}/${details.storeId}/${details.registerId}"
                        )
                    }
                }

                is VerifyPinUiEvent.NavigateToCartScreen -> {
                    navController.navigate(
                        "home"
                    )
                }

                is VerifyPinUiEvent.NavigateToSelectRegister -> {
                    navController.navigate("selectRegister") {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is VerifyPinUiEvent.ShowNoInternetDialog -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = dismiss,
                        iconRes = R.drawable.no_internet_icon
                    ) {
                        pinValue = ""
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        HeaderAppBarAuth()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side - Clock In/Out History (or Date/Time when empty)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (clockHistory.isEmpty()) {
                    // Show current date and time like before
                    BaseText(
                        text = currentTime,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 48F
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BaseText(
                        text = currentDate,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 24F
                    )
                } else {
                    // Show history list styled like Pending Orders (table header + rows)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()
                            .padding(vertical = 60.dp)
                    ) {
                        // Heading
                        BaseText(
                            text = "Clock In/Out History",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium,
                            fontSize = 24F,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
//                            textAlign = TextAlign.Center
                        )

                        // Table with rounded corners and border
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colorResource(id = R.color.primary))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BaseText(text = "#", color = Color.White, modifier = Modifier.width(40.dp))
                                    BaseText(text = "Date", color = Color.White, modifier = Modifier.weight(1f))
                                    BaseText(text = "Clock In", color = Color.White, modifier = Modifier.weight(1f))
                                    BaseText(text = "Clock Out", color = Color.White, modifier = Modifier.weight(1f))
                                    BaseText(text = "Total Hours", color = Color.White, modifier = Modifier.weight(1f))
                                }

                                // List
                                LazyColumn {
                                    items(clockHistory.size) { index ->
                                        val entry = clockHistory[index]
                                        val rawIn = entry.check_in_time
                                        val rawOut = entry.check_out_time

                                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

                                        fun parseDateTime(raw: String?): Pair<String, String> {
                                            return try {
                                                if (raw.isNullOrBlank()) return "-" to "-"
                                                val dt = java.time.OffsetDateTime.parse(raw)
                                                val date = dt.toLocalDate().toString()
                                                val time = dt.toLocalTime().format(timeFormatter)
                                                date to time
                                            } catch (_: Exception) {
                                                try {
                                                    val dt = java.time.LocalDateTime.parse(raw)
                                                    val date = dt.toLocalDate().toString()
                                                    val time = dt.toLocalTime().format(timeFormatter)
                                                    date to time
                                                } catch (_: Exception) {
                                                    "-" to "-"
                                                }
                                            }
                                        }

                                        val (dateIn, timeIn) = parseDateTime(rawIn)
                                        val (_, timeOut) = parseDateTime(rawOut)

                                        val total = try {
                                            if (rawIn.isNullOrBlank() || rawOut.isNullOrBlank()) "-" else run {
                                                val start = java.time.OffsetDateTime.parse(rawIn)
                                                val end = java.time.OffsetDateTime.parse(rawOut)
                                                val minutes = java.time.Duration.between(start, end).toMinutes()
                                                val hrs = minutes / 60
                                                val mins = minutes % 60
                                                String.format("%02dh %02dm", hrs, mins)
                                            }
                                        } catch (_: Exception) { "-" }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (index % 2 == 0) Color.White else Color(0xFFF5F5F5)
                                                )
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            BaseText(text = "${index + 1}-", modifier = Modifier.width(40.dp), color = Color.Black)
                                            BaseText(text = dateIn, modifier = Modifier.weight(1f), color = Color.Black)
                                            BaseText(text = timeIn, modifier = Modifier.weight(1f), color = Color.Black)
                                            BaseText(text = timeOut, modifier = Modifier.weight(1f), color = Color.Black)
                                            BaseText(text = total, modifier = Modifier.weight(1f), color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Side - PIN Entry (no card)
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .padding(horizontal = 40.dp, vertical = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Title
                BaseText(
                    text = stringResource(id = R.string.enter_pin_code),
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorResource(id = R.color.bgColorTextView),
                    fontWeight = FontWeight.Medium,
                    fontSize = 24F
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Display Field
                PinDisplayField(
                    pinValue = pinValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Keypad
                Keypad(
                    pinValue = pinValue,
                    pinLimit = pinLimit,
                    onPinChange = { pinValue = it },
                    onNextClick = {
                        if (pinValue.length == pinLimit) {
                            viewModel.verifyPin(pinValue)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clock In / Clock Out Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Clock In (green, left)
                    Button(
                        onClick = {
                            if (pinValue.length == pinLimit) {
                                viewModel.clockInOut(pinValue, "check-in")
                            } else {
                                DialogHandler.showDialog(
                                    message = "Please input pin first",
                                    buttonText = "OK"
                                ) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.green_success)
                        ),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        BaseText(
                            text = "Clock-In",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18F,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Clock Out (green, right)
                    Button(
                        onClick = {
                            if (pinValue.length == pinLimit) {
                                viewModel.clockInOut(pinValue, "check-out")
                            } else {
                                DialogHandler.showDialog(
                                    message = "Please input pin first",
                                    buttonText = "OK"
                                ) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.green_success)
                        ),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        BaseText(
                            text = "Clock-Out",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18F,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinDisplayField(
    pinValue: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) { index ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BaseText(
                    text = if (index < pinValue.length) pinValue[index].toString() else "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorResource(id = R.color.textColorPrimary),
                    fontWeight = FontWeight.Medium,
                    fontSize = 32F,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(
                    modifier = Modifier.width(40.dp),
                    thickness = 2.dp,
                    color = colorResource(id = R.color.pricing_calculator_clr)
                )
            }
        }
    }
}

@Composable
fun Keypad(
    pinValue: String,
    pinLimit: Int,
    onPinChange: (String) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: 7, 8, 9
        KeypadRow(
            buttons = listOf("7", "8", "9"),
            pinValue = pinValue,
            pinLimit = pinLimit,
            onPinChange = onPinChange
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Row 2: 4, 5, 6
        KeypadRow(
            buttons = listOf("4", "5", "6"),
            pinValue = pinValue,
            pinLimit = pinLimit,
            onPinChange = onPinChange
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Row 3: 1, 2, 3
        KeypadRow(
            buttons = listOf("1", "2", "3"),
            pinValue = pinValue,
            pinLimit = pinLimit,
            onPinChange = onPinChange
        )

        Spacer(modifier = Modifier.height(5.dp))

        // Row 4: Clear, 0, Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Clear Button (Primary Color)
            KeypadButton(
                text = stringResource(id = R.string.clear),
                onClick = {
                    if (pinValue.isNotEmpty()) {
                        onPinChange(pinValue.dropLast(1))
                    }
                },
                modifier = Modifier.weight(1f),
                isPrimary = true
            )

            // 0 Button (Gray)
            KeypadButton(
                text = "0",
                onClick = {
                    if (pinValue.length < pinLimit) {
                        onPinChange(pinValue + "0")
                    }
                },
                modifier = Modifier.weight(1f),
                isPrimary = false
            )

            // Next Button (Primary Color)
            KeypadButton(
                text = stringResource(id = R.string.next),
                onClick = {
                    if (pinValue.length == pinLimit) {
                        onNextClick()
                    }
                },
                modifier = Modifier.weight(1f),
                isPrimary = true
            )
        }
    }
}

@Composable
fun KeypadRow(
    buttons: List<String>,
    pinValue: String,
    pinLimit: Int,
    onPinChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        buttons.forEach { digit ->
            KeypadButton(
                text = digit,
                onClick = {
                    if (pinValue.length < pinLimit) {
                        onPinChange(pinValue + digit)
                    }
                },
                modifier = Modifier.weight(1f),
                isPrimary = false // Number buttons are gray
            )
        }
    }
}

@Composable
fun KeypadButton(
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

