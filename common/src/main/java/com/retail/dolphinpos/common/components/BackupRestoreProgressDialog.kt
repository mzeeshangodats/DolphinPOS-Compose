package com.retail.dolphinpos.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.retail.dolphinpos.common.utils.GeneralSans

@Composable
fun BackupRestoreProgressDialog(
    message: String,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = { /* Non-cancellable */ }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = modifier.fillMaxWidth(0.4f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontFamily = GeneralSans,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }
    }
}

