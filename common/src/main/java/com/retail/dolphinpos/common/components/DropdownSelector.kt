package com.retail.dolphinpos.common.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    items: List<String>,
    selectedText: String,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        BaseText(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(id = R.color.primary),
            fontWeight = FontWeight.SemiBold
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Light
                ),
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                },
                modifier = modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = colorResource(id = R.color.borderOutline),
                    unfocusedBorderColor = colorResource(id = R.color.borderOutline),
                    cursorColor = colorResource(id = R.color.primary)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color.White
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GeneralSans,
                                    fontWeight = FontWeight.Normal
                                )
                            ) 
                        },
                        onClick = {
                            onItemSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelectorSmallHeight(
    label: String,
    items: List<String>,
    selectedText: String,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    labelFontSize: Float = 12f
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        BaseText(
            text = label,
            fontSize = labelFontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = GeneralSans,
            color = colorResource(id = R.color.primary)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = GeneralSans,
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp
                ),
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                },
                modifier = modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(top = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    focusedBorderColor = colorResource(id = R.color.borderOutline),
                    unfocusedBorderColor = colorResource(id = R.color.borderOutline),
                    cursorColor = colorResource(id = R.color.primary)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = Color.White
            ) {
                items.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = GeneralSans,
                                    fontWeight = FontWeight.Normal
                                )
                            ) 
                        },
                        onClick = {
                            onItemSelected(index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
