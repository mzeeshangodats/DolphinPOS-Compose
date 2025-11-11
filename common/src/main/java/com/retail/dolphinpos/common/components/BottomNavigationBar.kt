package com.retail.dolphinpos.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retail.dolphinpos.common.R
import com.retail.dolphinpos.common.utils.GeneralSans
import com.retail.dolphinpos.domain.model.home.bottom_nav.BottomMenu

@Composable
fun BottomNavigationBar(
    menus: List<BottomMenu>,
    selectedIndex: Int = 0,
    onMenuClick: (BottomMenu) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // actionBarSize equivalent
            .background(Color.White)
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        menus.forEachIndexed { index, menu ->
            BottomNavButton(
                menu = menu,
                isSelected = selectedIndex == index,
                onClick = { onMenuClick(menu) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    menu: BottomMenu,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        colorResource(id = R.color.primary)
    } else {
        colorResource(id = R.color.nav_bar_button_clr)
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        Color.Black
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .padding(horizontal = 1.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = menu.menuName,
            fontSize = 12.sp,
            fontFamily = GeneralSans,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
