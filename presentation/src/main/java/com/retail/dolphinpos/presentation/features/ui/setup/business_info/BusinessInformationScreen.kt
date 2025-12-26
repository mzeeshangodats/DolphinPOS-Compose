package com.retail.dolphinpos.presentation.features.ui.setup.business_info

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
import com.retail.dolphinpos.common.components.BaseOutlinedEditTextSmallHeight
import com.retail.dolphinpos.common.components.BaseText
import com.retail.dolphinpos.common.components.HeaderAppBarWithBack
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.presentation.R
import com.retail.dolphinpos.presentation.util.DialogHandler
import com.retail.dolphinpos.presentation.util.Loader

@Composable
fun BusinessInformationScreen(
    navController: NavController,
    viewModel: BusinessInformationViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val storeName by viewModel.storeName.collectAsStateWithLifecycle()
    val addressLine1 by viewModel.addressLine1.collectAsStateWithLifecycle()
    val addressLine2 by viewModel.addressLine2.collectAsStateWithLifecycle()
    val zipCode by viewModel.zipCode.collectAsStateWithLifecycle()
    val phoneNumber by viewModel.phoneNumber.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Function to navigate to home
    val navigateToHome = {
        navController.navigate("home") {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Handle loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            Loader.show("Loading...")
        } else {
            Loader.hide()
        }
    }

    // Handle UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BusinessInformationUiEvent.ShowSuccess -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK",
                        iconRes = R.drawable.success_circle_icon
                    ) {
                        navigateToHome()
                    }
                }
                is BusinessInformationUiEvent.ShowError -> {
                    DialogHandler.showDialog(
                        message = event.message,
                        buttonText = "OK",
                        iconRes = R.drawable.cross_red
                    ) {}
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.light_grey))
            .verticalScroll(rememberScrollState())
    ) {
        // Header with back button and title
        HeaderAppBarWithBack(
            title = "Business Information",
            onBackClick = navigateToHome
        )

        // Spacer for card positioning
        Spacer(modifier = Modifier.height(16.dp))

        // Centered Card with 4dp padding and 50% screen width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.5f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Store Name Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.light_grey),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue store icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_store),
                            contentDescription = "Store Name",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Center-left: Label
                        BaseText(
                            text = "Store Name*",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: TextField
                        Box(
                            modifier = Modifier
                                .weight(1.5f)

                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = storeName,
                                onValueChange = { viewModel.updateStoreName(it) },
                                placeholder = "Store Name",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 2. Address Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(R.color.color_grey1),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue user/location icon
                        Icon(
                            painter = painterResource(id = R.drawable.ic_address),
                            contentDescription = "Address 1",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Label
                        BaseText(
                            text = "Address 1*",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 3. Address Fields Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.light_grey),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Address Line 1
                        Box(
                            modifier = Modifier
                                .weight(1f)

                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = addressLine1,
                                onValueChange = { viewModel.updateAddressLine1(it) },
                                placeholder = "Address Line 1",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Address Line 2
                        Box(
                            modifier = Modifier
                                .weight(1f)

                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = addressLine2,
                                onValueChange = { viewModel.updateAddressLine2(it) },
                                placeholder = "Address Line 2",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Address Line 3
//                        Box(
//                            modifier = Modifier
//                                .weight(1f)
//
//                        ) {
//                            BaseOutlinedEditTextSmallHeight(
//                                value = zipCode,
//                                onValueChange = { viewModel.updateZipCode(it) },
//                                placeholder = "Address Line 3",
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
                    }

                    // 4. ZIP Code Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.light_grey),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Blue user icon
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "ZIP Code",
                            tint = colorResource(id = R.color.primary),
                            modifier = Modifier.size(24.dp)
                        )
                        // Label
                        BaseText(
                            text = "ZIP Code*",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Right: TextField
                        Box(
                            modifier = Modifier
                                .weight(1.5f)

                        ) {
                            BaseOutlinedEditTextSmallHeight(
                                value = zipCode,
                                onValueChange = { viewModel.updateZipCode(it) },
                                placeholder = "ZIP Code*",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 5. Phone Number Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = colorResource(id = R.color.light_grey),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left: Blue phone icon
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Number",
                                tint = colorResource(id = R.color.primary),
                                modifier = Modifier.size(24.dp)
                            )
                            // Label
                            BaseText(
                                text = "Phone Number*",
                                color = Color.Black,
                                fontSize = 14f,
                                fontFamily = GeneralSans,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            // Right: TextField with Badge
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)

                            ) {
                                BaseOutlinedEditTextSmallHeight(
                                    value = phoneNumber,
                                    onValueChange = { viewModel.updatePhoneNumber(it) },
                                    placeholder = "Phone Number",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 6. Bottom Buttons (aligned to end/right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BaseButton(
                            text = "Cancel",
                            modifier = Modifier
                                .height(45.dp),
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline)),
                            onClick = { navigateToHome() }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        BaseButton(
                            text = "Save",
                            modifier = Modifier
                                .height(45.dp),
                            enabled = !isLoading,
                            onClick = {
                                viewModel.saveStoreInformation()
                            }
                        )
                    }
                }
            }
        }
    }
}

