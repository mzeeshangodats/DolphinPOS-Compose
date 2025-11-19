package com.retail.dolphinpos.presentation.features.ui.setup.business_info

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.retail.dolphinpos.common.components.BaseButton
import com.retail.dolphinpos.common.components.BaseOutlinedEditText
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
                        .padding(4.dp)
                ) {
                    // Store Name
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        BaseText(
                            text = "Store Name",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = storeName,
                            onValueChange = { viewModel.updateStoreName(it) },
                            placeholder = "Enter Store Name"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Address Line 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Address Line 1",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = addressLine1,
                            onValueChange = { viewModel.updateAddressLine1(it) },
                            placeholder = "Enter Address Line 1"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Address Line 2
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Address Line 2",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = addressLine2,
                            onValueChange = { viewModel.updateAddressLine2(it) },
                            placeholder = "Enter Address Line 2"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Zip Code
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Zip Code",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = zipCode,
                            onValueChange = { viewModel.updateZipCode(it) },
                            placeholder = "Enter Zip Code"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Number
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        BaseText(
                            text = "Phone Number",
                            color = Color.Black,
                            fontSize = 14f,
                            fontFamily = GeneralSans,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BaseOutlinedEditText(
                            value = phoneNumber,
                            onValueChange = { viewModel.updatePhoneNumber(it) },
                            placeholder = "Enter Phone Number"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Save and Cancel)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BaseButton(
                            text = "Cancel",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            backgroundColor = Color.White,
                            textColor = Color.Black,
                            border = BorderStroke(1.dp, colorResource(id = R.color.borderOutline)),
                            onClick = { navigateToHome() }
                        )

                        BaseButton(
                            text = "Save",
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
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

