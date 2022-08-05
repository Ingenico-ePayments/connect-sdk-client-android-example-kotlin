/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.compose.theme.Green

@Composable
fun Header(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showBackButton = shouldShowBackButton(navBackStackEntry?.destination?.route)

    HeaderContent(showBackButton = showBackButton, onBackPressed = { navController.popBackStack() })
}

@Composable
fun HeaderContent(showBackButton: Boolean, onBackPressed: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = { onBackPressed() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            },
            backgroundColor = Color.White,
            elevation = 0.dp
        )

        Image(
            painter = painterResource(id = R.drawable.logo_example_merchant),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 16.dp)
                .width(250.dp)
                .height(50.dp)
                .align(Alignment.CenterHorizontally)
        )
        Row(
            modifier = Modifier
                .padding(top = 16.dp, end = 16.dp)
                .align(Alignment.End)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(id = R.string.gc_app_general_securePaymentText),
                color = Green,
                fontSize = 11.sp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Preview
@Composable
fun HeaderPreview() {
    HeaderContent(showBackButton = true, onBackPressed = {})
}

private fun shouldShowBackButton(route: String?): Boolean {
    return when (route) {
        PaymentScreen.CONFIGURATION.route -> false
        PaymentScreen.PRODUCT.route -> true
        PaymentScreen.CARD.route -> true
        PaymentScreen.RESULT.route -> false
        else -> false
    }
}
