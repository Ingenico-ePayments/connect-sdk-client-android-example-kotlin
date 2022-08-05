/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.card.textfield

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.android.example.kotlin.compose.components.IsValid
import com.ingenico.connect.android.example.kotlin.compose.components.TextFieldState
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProductField

/**
 * Class for holding the state of a Card number field.
 * Derivative of the parent class TextFieldState
 */
class CardNumberTextFieldState(
    text: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: Any = "",
    enabled: Boolean = true,
    liveValidating: Boolean = false,
    val leadingIcon: ImageVector,
    trailingIconUrl: String = "",
    isLoading: Boolean = false,
    val id : String,
    var mask: String? = null,
    var maxSize: Int = Int.MAX_VALUE,
    var lastCheckedCardValue: String = "",
    var paymentProductField: PaymentProductField? = null
) : TextFieldState(
    validator = ::isFieldValid,
    errorText = ::fieldValidationError,
    text = text,
    keyboardOptions = keyboardOptions,
    label = label,
    enabled = enabled,
    liveValidating = liveValidating
) {
    var trailingImageUrl: String by mutableStateOf(trailingIconUrl)
    var isLoading: Boolean by mutableStateOf(isLoading)
    var networkErrorMessage: String by mutableStateOf("")
}

private fun fieldValidationError(errorText: Any): Any {
    return errorText
}

private fun isFieldValid(value: String): IsValid {
    return if (value.isNotBlank()) IsValid.Yes else IsValid.No(R.string.payment_configuration_field_not_valid_error)
}
