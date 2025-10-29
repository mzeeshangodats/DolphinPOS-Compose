package com.retail.dolphinpos.common.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans

@Composable
fun ClockInOutDialog(
    pinValue: String,
    onPinChange: (String) -> Unit,
    onClockOut: () -> Unit,
    onClockIn: () -> Unit,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close button in top right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_icon),
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Blue square icon with white clock
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = colorResource(id = R.color.primary),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.clock_in_out_icon),
                            contentDescription = "Clock Icon",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    BaseText(
                        text = "Clock In/Out",
                        fontSize = 20F,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Label
                    BaseText(
                        text = "PIN*",
                        fontSize = 14F,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // PIN Input Field with Clear Button (similar to PaymentInput)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = colorResource(id = R.color.borderOutline),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .height(50.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = pinValue,
                            onValueChange = { newValue ->
                                // Limit to 4 digits
                                if (newValue.length <= 4) {
                                    onPinChange(newValue)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                fontFamily = GeneralSans,
                                fontSize = 12.sp,
                                color = Color.Black
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            cursorBrush = SolidColor(Color.Black),
                            decorationBox = { innerTextField ->
                                if (pinValue.isEmpty()) {
                                    BaseText(
                                        text = "Enter code",
                                        fontSize = 12F,
                                        color = Color.Gray,
                                        fontFamily = GeneralSans
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Remove Icon Button (similar to PaymentInput)
                        IconButton(
                            onClick = { onPinChange("") },
                            modifier = Modifier.size(24.dp),
                            enabled = pinValue.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.clear_text_icon),
                                contentDescription = "Clear PIN",
                                modifier = Modifier.size(16.dp),
                                tint = if (pinValue.isNotEmpty()) Color.Gray else Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Clock Out Button (Left)
                        BaseButton(
                            text = "Clock Out",
                            onClick = {
                                if (pinValue.isNotEmpty()) {
                                    onClockOut()
                                }
                            },
                            enabled = pinValue.isNotEmpty(),
                            backgroundColor = Color.White,
                            textColor = colorResource(id = R.color.colorHint),
                            fontSize = 16,
                            height = 48.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color = colorResource(id = R.color.cart_screen_btn_clr)
                            ),
                            cornerRadius = 8.dp,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Clock In Button (Right)
                        BaseButton(
                            text = "Clock In",
                            onClick = {
                                if (pinValue.isNotEmpty()) {
                                    onClockIn()
                                }
                            },
                            backgroundColor = colorResource(id = R.color.primary),
                            fontSize = 16,
                            height = 48.dp,
                            cornerRadius = 8.dp,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // View History Link
                    TextButton(
                        onClick = onViewHistory,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BaseText(
                            text = "View Clock In/Out History",
                            fontSize = 14F,
                            color = colorResource(id = R.color.primary),
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

