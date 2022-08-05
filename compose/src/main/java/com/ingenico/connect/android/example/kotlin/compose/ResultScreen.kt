/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.compose.components.PrimaryButton
import com.ingenico.connect.android.example.kotlin.compose.components.SecondaryButton
import com.ingenico.connect.android.example.kotlin.compose.theme.WorldlineMint

@Composable
fun ResultScreen(navController: NavHostController, encryptedFieldsData: String?) {
    ResultContent(
        onPrimaryButtonClicked = {
            navController.navigate(PaymentScreen.CONFIGURATION.route) {
                popUpTo(PaymentScreen.CONFIGURATION.route) { inclusive = true }
            }
        },
        encryptedFieldsData = encryptedFieldsData ?: ""
    )
}

@Composable
fun ResultContent(
    onPrimaryButtonClicked: () -> Unit,
    encryptedFieldsData: String
) {
    var showEncryptedFieldsData by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.padding(16.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(id = R.string.payment_result_title),
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = stringResource(id = R.string.payment_result_explanation_text),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.body2
                )
                TextButton(
                    onClick = { showEncryptedFieldsData = true },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.payment_result_show_encrypted_fields_data),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body2,
                        color = WorldlineMint
                    )
                }

                if (showEncryptedFieldsData && encryptedFieldsData.isNotBlank()) {
                    Text(
                        text = stringResource(id = R.string.payment_result_encrypted_fields_data_title),
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.subtitle2
                    )
                    Text(
                        text = encryptedFieldsData,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.body2,
                        fontSize = 11.sp
                    )
                    SecondaryButton(
                        text = stringResource(id = R.string.payment_result_copy_encrypted_fields_data_to_clipboard),
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(alignment = Alignment.End),
                        onSecondaryButtonClicked = {
                            clipboard.setText(
                                AnnotatedString(
                                    encryptedFieldsData
                                )
                            )
                        }
                    )
                }
            }
        },
        bottomBar = {
            PrimaryButton(
                onClick = { onPrimaryButtonClicked() },
                text = stringResource(id = R.string.payment_result_return_to_start),
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    )
}

@Preview
@Composable
fun ResultScreenPreview() {
    ResultContent(onPrimaryButtonClicked = {}, encryptedFieldsData = "Some Text")
}
