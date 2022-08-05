/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.configuration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.ingenico.connect.android.example.kotlin.common.googlepay.GooglePayConfiguration
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.android.example.kotlin.compose.components.*
import com.ingenico.connect.android.example.kotlin.compose.theme.ComposeTheme
import com.ingenico.connect.gateway.sdk.client.android.sdk.configuration.SessionConfiguration
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.AmountOfMoney
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.PaymentContext
import java.util.*

@Composable
fun ConfigurationScreen(
    navController: NavHostController,
    paymentSharedViewModel: PaymentSharedViewModel,
    configurationViewModel: ConfigurationViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val uiState = configurationViewModel.uiState

    if (uiState.configurationStatus.value is ConfigurationStatus.Valid) {
        paymentSharedViewModel.configureConnectSDK(SessionConfiguration(
            uiState.sessionDetailFields[0].text,
            uiState.sessionDetailFields[1].text,
            uiState.sessionDetailFields[2].text,
            uiState.sessionDetailFields[3].text),
            PaymentContext(AmountOfMoney(
                uiState.paymentDetailsFields[0].text.toLong(),
                uiState.paymentDetailsFields[2].text),
                uiState.paymentDetailsFields[1].text,
                uiState.otherOptionsFields[0].isChecked.value,
                Locale.getDefault()
            ),
            uiState.otherOptionsFields[1].isChecked.value
        )
        if (uiState.otherOptionsFields[2].isChecked.value) {
            paymentSharedViewModel.googlePayConfiguration = GooglePayConfiguration(true,
                uiState.googlePayFields[0].text,
                uiState.googlePayFields[1].text)
        }

        uiState.configurationStatus.value = ConfigurationStatus.None
        navController.navigate(PaymentScreen.PRODUCT.route)
    }

    ConfigurationContent(uiState,
        onPrimaryButtonClicked = { configurationViewModel.validateForm() },
        onSecondaryButtonClicked = {
            configurationViewModel.parseClipBoardData(
                clipboard.getText().toString()
            )
        },
        showBottomSheet = { showBottomSheet(it) })
}

@Composable
private fun ConfigurationContent(
    uiState: ConfigurationUiState,
    onPrimaryButtonClicked: () -> Unit,
    onSecondaryButtonClicked: () -> Unit,
    showBottomSheet: (BottomSheetContent) -> Unit
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        SectionTitle(stringResource(id = R.string.payment_configuration_client_session_details), modifier = Modifier)
        uiState.sessionDetailFields.forEach { configurationInputField ->
            ConfigurationTextField(
                textFieldState = configurationInputField,
                onTrailingIconClicked = {
                    showBottomSheet(configurationInputField.bottomSheetContent)
                })
        }
        SecondaryButton(
            text = stringResource(id = R.string.payment_configuration_paste_json),
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End), onSecondaryButtonClicked = { onSecondaryButtonClicked() })
        SectionTitle(stringResource(id = R.string.payment_configuration_payment_details), modifier = Modifier.padding(top = 32.dp))
        uiState.paymentDetailsFields.forEach { configurationInputField ->
            ConfigurationTextField(
                textFieldState = configurationInputField,
                onTrailingIconClicked = {
                    showBottomSheet(configurationInputField.bottomSheetContent)
                })
        }
        SectionTitle(text = stringResource(id = R.string.payment_configuration_other_options), modifier = Modifier.padding(top = 32.dp))
        LabelledCheckbox(checkBoxField = uiState.otherOptionsFields[0], onTrailingIconClicked = {
            showBottomSheet(uiState.otherOptionsFields[0].bottomSheetContent)
        })
        LabelledCheckbox(checkBoxField = uiState.otherOptionsFields[1], onTrailingIconClicked = {
            showBottomSheet(uiState.otherOptionsFields[1].bottomSheetContent)
        })
        GooglePaySection(checkBoxField = uiState.otherOptionsFields[2], uiState.googlePayFields, showBottomSheet = { showBottomSheet(it) })
        PrimaryButton(
            text = stringResource(id = R.string.payment_configuration_proceed_to_checkout),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally),

            onClick = { onPrimaryButtonClicked() }
        )
    }
}

@Composable
private fun GooglePaySection(
    checkBoxField: CheckBoxField,
    googlePayFields: List<ConfigurationTextFieldState>,
    showBottomSheet: (BottomSheetContent) -> Unit
) {
    Row {
        Checkbox(
            checked = checkBoxField.isChecked.value,
            onCheckedChange = { checkBoxField.isChecked.value = it },
            enabled = checkBoxField.enabled.value
        )
        Column(modifier = Modifier.padding(start = 4.dp, top = 13.dp)) {
            Text(text = stringResource(id = R.string.payment_configuration_configure_google_pay))
            if (checkBoxField.isChecked.value) {
                googlePayFields.forEach { configurationInputField ->
                    ConfigurationTextField(
                        textFieldState = configurationInputField,
                        onTrailingIconClicked = {
                            showBottomSheet(configurationInputField.bottomSheetContent)
                        })
                }
            }
        }
    }
}

@Preview
@Composable
fun ConfigurationScreenPreview() {
    ComposeTheme {
        ConfigurationContent(
            uiState = ConfigurationUiState(),
            onPrimaryButtonClicked = {},
            onSecondaryButtonClicked = {},
            showBottomSheet = {}
        )
    }
}
