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
 * Class for holding the state of a Card field.
 * Derivative of the parent class TextFieldState
 */
class CardTextFieldState(
    text: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: Any = "",
    enabled: Boolean = true,
    liveValidating: Boolean = false,
    val leadingIcon: ImageVector,
    val trailingIcon: ImageVector? = null,
    var mask: String? = null,
    var tooltipImageUrl: String? = null,
    var tooltipText: String? = null,
    var maxSize: Int = Int.MAX_VALUE,
    var paymentProductField: PaymentProductField? = null,
    val id: String
) : TextFieldState(
    validator = ::isFieldValid,
    errorText = ::fieldValidationError,
    text = text,
    keyboardOptions = keyboardOptions,
    label = label,
    enabled = enabled,
    liveValidating = liveValidating
) {

    var networkErrorMessage: String by mutableStateOf("")
}

private fun fieldValidationError(errorText: Any): Any {
    return errorText
}

private fun isFieldValid(value: String): IsValid {
    return if (value.isNotBlank()) IsValid.Yes else IsValid.No(R.string.payment_configuration_field_not_valid_error)
}
