/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ingenico.connect.android.example.kotlin.compose.card.textfield.CardNumberField
import com.ingenico.connect.android.example.kotlin.compose.card.textfield.CardTextField
import com.ingenico.connect.android.example.kotlin.common.PaymentCardUiState
import com.ingenico.connect.android.example.kotlin.common.PaymentCardViewModel
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.ingenico.connect.android.example.kotlin.common.utils.FormValidationResult
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.ingenico.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.ingenico.connect.android.example.kotlin.compose.components.FailedText
import com.ingenico.connect.android.example.kotlin.compose.components.LabelledCheckbox
import com.ingenico.connect.android.example.kotlin.compose.components.PrimaryButton
import com.ingenico.connect.android.example.kotlin.compose.components.ProgressIndicator
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.EncryptedPaymentRequest
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.iin.IinStatus
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentProductGroup
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProductField

@ExperimentalComposeUiApi
@Composable
fun CardScreen(
    navController: NavHostController,
    paymentSharedViewModel: PaymentSharedViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit,
    paymentCardViewModel: PaymentCardViewModel = viewModel(),
    cardScreenViewModel: CardScreenViewModel = viewModel()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val cardFields = cardScreenViewModel.cardFields

    LaunchedEffect(true) {
        paymentCardViewModel.getPaymentProduct(
            paymentSharedViewModel.selectedPaymentProduct
        )
    }

    val formValidationResult by
        paymentCardViewModel.formValidationResult.observeAsState(FormValidationResult.NotValidated)

    analyzeFormValidationResult(formValidationResult, cardScreenViewModel)

    val encryptedPaymentRequestStatus by paymentCardViewModel.encryptedPaymentRequestStatus.observeAsState(Status.None)

    when(encryptedPaymentRequestStatus){
        is Status.ApiError -> {
            cardScreenViewModel.cardFieldsEnabled(true)
        }
        is Status.Loading -> {
            cardScreenViewModel.cardFieldsEnabled(false)
        }
        is Status.Success -> {
            val encryptedDataFields =
                ((encryptedPaymentRequestStatus as Status.Success).data as EncryptedPaymentRequest).encryptedFields

            updateValuesOnSuccessStatus(cardScreenViewModel, paymentCardViewModel)

            keyboardController?.hide()
            navController.navigate("${PaymentScreen.RESULT.route}/$encryptedDataFields"){
                popUpTo(PaymentScreen.CONFIGURATION.route)
            }
        }
        is Status.Failed -> {
            // Show generic error
        }
        is Status.None -> {
            // Init status; nothing to do here
        }
    }

    val paymentProductFieldsUiState by paymentCardViewModel.paymentProductFieldsUiState.observeAsState(
        PaymentCardUiState.Loading
    )

    if (paymentProductFieldsUiState is PaymentCardUiState.Success) {
        updateValuesOnSuccessUIState(paymentProductFieldsUiState, cardScreenViewModel, paymentCardViewModel, cardFields)
    }

    CardContent(
        uiState = paymentProductFieldsUiState,
        cardFields = cardFields,
        assetsBaseUrl = ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl,
        isFormValid = formValidationResult is FormValidationResult.Valid,
        isFormSubmitted = encryptedPaymentRequestStatus is Status.Loading,
        onPrimaryButtonClicked = { paymentCardViewModel.onPayClicked() },
        showBottomSheet = { showBottomSheet(it) },
        issuerIdentificationNumberChanged = { issuerIdentificationNumber ->
            if (paymentSharedViewModel.selectedPaymentProduct is BasicPaymentProductGroup) {
                cardFields.cardNumberField.isLoading = true
            }
            paymentCardViewModel.getPaymentProductId(issuerIdentificationNumber)
        },
        onValueChanged = { paymentProductField, value ->
            paymentCardViewModel.fieldChanged(paymentProductField, value)
        },
        rememberCardValue = { paymentCardViewModel.saveCardForLater(it) }
    )
}

private fun analyzeFormValidationResult(
    formValidationResult: FormValidationResult,
    cardScreenViewModel: CardScreenViewModel
) {
    when(formValidationResult) {
        is FormValidationResult.InvalidWithValidationErrorMessage -> {
            cardScreenViewModel.setFieldErrors(
                (formValidationResult as FormValidationResult.InvalidWithValidationErrorMessage).exceptions
            )
        }
        is FormValidationResult.Valid -> {
            cardScreenViewModel.setFieldErrors(emptyList())
        }
        is FormValidationResult.NotValidated -> {
            cardScreenViewModel.setFieldErrors(emptyList())
        }
        is FormValidationResult.Invalid -> {
            // No option, when form is invalid it will always come in the
            // FormValidationResult.InvalidWithValidationErrorMessage case
        }
    }
}

private fun updateValuesOnSuccessStatus(
    cardScreenViewModel: CardScreenViewModel,
    paymentCardViewModel: PaymentCardViewModel
) {
    cardScreenViewModel.cardFieldsEnabled(true)

    paymentCardViewModel.formValidationResult.value = FormValidationResult.NotValidated
    paymentCardViewModel.encryptedPaymentRequestStatus.value = Status.None
    paymentCardViewModel.paymentProductFieldsUiState.value = PaymentCardUiState.None
}

private fun updateValuesOnSuccessUIState(
    paymentCardUiState: PaymentCardUiState,
    cardScreenViewModel: CardScreenViewModel,
    paymentCardViewModel: PaymentCardViewModel,
    cardFields: CardFields
) {
    cardFields.cardNumberField.isLoading = false

    val paymentFields = (paymentCardUiState as PaymentCardUiState.Success)

    cardScreenViewModel.updateFields(
        paymentProductFields = paymentFields.paymentFields,
        logoUrl = ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl + paymentFields.logoUrl,
        accountOnFile = paymentFields.accountOnFile,
    )

    paymentCardViewModel.updateValueInPaymentRequest(
        cardFields.cardNumberField.id,
        cardFields.cardNumberField.text
    )
    paymentCardViewModel.updateValueInPaymentRequest(
        cardFields.expiryDateField.id,
        cardFields.expiryDateField.text
    )
    paymentCardViewModel.updateValueInPaymentRequest(
        cardFields.securityNumberField.id,
        cardFields.securityNumberField.text
    )
    paymentCardViewModel.updateValueInPaymentRequest(
        cardFields.cardHolderField.id,
        cardFields.cardHolderField.text
    )

    paymentCardViewModel.shouldEnablePayButton()
}

@Composable
fun CardContent(
    uiState: PaymentCardUiState,
    cardFields: CardFields,
    assetsBaseUrl: String,
    isFormValid: Boolean,
    isFormSubmitted: Boolean,
    onPrimaryButtonClicked: () -> Unit,
    showBottomSheet: (BottomSheetContent) -> Unit,
    issuerIdentificationNumberChanged: (String) -> Unit,
    onValueChanged: (PaymentProductField, String) -> Unit,
    rememberCardValue: (Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is PaymentCardUiState.ApiError -> {
                FailedText()
            }
            is PaymentCardUiState.IinFailed -> {
                IinFailedStateContent(
                    uiState = uiState,
                    cardFields = cardFields,
                    assetsBaseUrl = assetsBaseUrl,
                    isFormValid = isFormValid,
                    isFormSubmitted = isFormSubmitted,
                    onPrimaryButtonClicked = { onPrimaryButtonClicked() },
                    showBottomSheet = { showBottomSheet(it) },
                    issuerIdentificationNumberChanged = { issuerIdentificationNumberChanged(it) },
                    onValueChanged = { paymentProductField, value ->
                        onValueChanged(
                            paymentProductField,
                            value
                        )
                    },
                    rememberCardValue = { rememberCardValue(it) }
                )
            }
            is PaymentCardUiState.Loading -> {
                ProgressIndicator()
            }

            is PaymentCardUiState.Success -> {
                CardItems(
                    cardFields = cardFields,
                    assetsBaseUrl = assetsBaseUrl,
                    isFormValid = isFormValid,
                    isFormSubmitted = isFormSubmitted,
                    onPrimaryButtonClicked = { onPrimaryButtonClicked() },
                    showBottomSheet = { showBottomSheet(it) },
                    issuerIdentificationNumberChanged = { issuerIdentificationNumberChanged(it) },
                    onValueChanged = { paymentProductField, value ->
                        onValueChanged(
                            paymentProductField,
                            value
                        )
                    },
                    rememberCardValue = { rememberCardValue(it) }
                )
            }

            is PaymentCardUiState.None -> {
                // Init status; nothing to do here
            }
            is PaymentCardUiState.Failed -> {
                // Show generic error
            }
        }
    }
}

@Composable
private fun IinFailedStateContent(
    uiState: PaymentCardUiState.IinFailed,
    cardFields: CardFields,
    assetsBaseUrl: String,
    isFormValid: Boolean,
    isFormSubmitted: Boolean,
    onPrimaryButtonClicked: () -> Unit,
    showBottomSheet: (BottomSheetContent) -> Unit,
    issuerIdentificationNumberChanged: (String) -> Unit,
    onValueChanged: (PaymentProductField, String) -> Unit,
    rememberCardValue: (Boolean) -> Unit
) {
    when (uiState.throwable.message) {
        IinStatus.UNKNOWN.name -> {
            cardFields.cardNumberField.networkErrorMessage =
                stringResource(
                    id = R.string.gc_general_paymentProductFields_validationErrors_iin_label
                )
        }
        IinStatus.EXISTING_BUT_NOT_ALLOWED.name -> {
            cardFields.cardNumberField.networkErrorMessage =
                stringResource(
                    id = R.string.gc_general_paymentProductFields_validationErrors_allowedInContext_label
                )
        }
    }
    cardFields.cardNumberField.isLoading = false
    CardItems(
        cardFields = cardFields,
        assetsBaseUrl = assetsBaseUrl,
        isFormValid = isFormValid,
        isFormSubmitted = isFormSubmitted,
        onPrimaryButtonClicked = { onPrimaryButtonClicked() },
        showBottomSheet = { showBottomSheet(it) },
        issuerIdentificationNumberChanged = { issuerIdentificationNumberChanged(it) },
        onValueChanged = { paymentProductField, value ->
            onValueChanged(
                paymentProductField,
                value
            )
        },
        rememberCardValue = { rememberCardValue(it) }
    )
}

@Composable
fun CardItems(
    cardFields: CardFields,
    assetsBaseUrl: String,
    isFormValid: Boolean,
    isFormSubmitted: Boolean,
    onPrimaryButtonClicked: () -> Unit,
    showBottomSheet: (BottomSheetContent) -> Unit,
    issuerIdentificationNumberChanged: (String) -> Unit,
    onValueChanged: (PaymentProductField, String) -> Unit,
    rememberCardValue: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        CardNumberField(
            cardNumberTextFieldState = cardFields.cardNumberField,
            issuerIdentificationNumberChanged = { issuerIdentificationNumberChanged(it) },
            onValueChanged = { paymentProductField, value ->
                onValueChanged(
                    paymentProductField,
                    value
                )
            })
        ExpiryDateSecurityNumberRow(cardFields, assetsBaseUrl, showBottomSheet, onValueChanged)
        CardTextField(
            cardTextFieldState = cardFields.cardHolderField,
            onValueChanged = { paymentProductField, value ->
                onValueChanged(
                    paymentProductField,
                    value
                )
            }
        )
        LabelledCheckbox(
            checkBoxField = cardFields.rememberCardField,
            onCheckedChange = { rememberCardValue(it) })
        PrimaryButton(
            onClick = { onPrimaryButtonClicked() },
            text = stringResource(id = R.string.gc_app_paymentProductDetails_payButton),
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid,
            showLoadingStatus = isFormSubmitted
        )
    }
}

@Composable
private fun ExpiryDateSecurityNumberRow(
    cardFields: CardFields,
    assetsBaseUrl: String,
    showBottomSheet: (BottomSheetContent) -> Unit,
    onValueChanged: (PaymentProductField, String) -> Unit
) {
    Row {
        Column(modifier = Modifier.weight(2f)) {
            CardTextField(
                cardTextFieldState = cardFields.expiryDateField,
                onValueChanged = { paymentProductField, value ->
                    onValueChanged(
                        paymentProductField,
                        value
                    )
                }
            )
        }
        Column(
            modifier = Modifier
                .weight(2f)
                .padding(start = 8.dp)
        ) {
            CardTextField(
                cardTextFieldState = cardFields.securityNumberField,
                onTrailingIconClicked = {
                    showBottomSheet(
                        BottomSheetContent(
                            text = cardFields.securityNumberField.tooltipText ?: "",
                            imageUrl = assetsBaseUrl + cardFields.securityNumberField.tooltipImageUrl
                        )
                    )
                },
                onValueChanged = { paymentProductField, value ->
                    onValueChanged(
                        paymentProductField,
                        value
                    )
                }
            )
        }
    }
}

@Preview
@Composable
fun CardScreenPreview() {
    CardItems(
        cardFields = CardFields(),
        assetsBaseUrl = "",
        isFormValid = false,
        isFormSubmitted = false,
        onPrimaryButtonClicked = {},
        showBottomSheet = {},
        issuerIdentificationNumberChanged = {},
        onValueChanged = { _, _ -> },
        rememberCardValue = {}
    )
}
