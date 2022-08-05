/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.configuration

import androidx.compose.foundation.text.KeyboardOptions
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.ingenico.connect.android.example.kotlin.compose.components.IsValid
import com.ingenico.connect.android.example.kotlin.compose.components.TextFieldState

class ConfigurationTextFieldState(
    text: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    label: Any = "",
    enabled: Boolean = true,
    liveValidating: Boolean = false,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent("")
) : TextFieldState(
    validator = ::isFieldValid,
    errorText = ::fieldValidationError,
    text = text,
    keyboardOptions = keyboardOptions,
    label = label,
    enabled = enabled,
    liveValidating = liveValidating
)

private fun fieldValidationError(errorText: Any): Any {
    return errorText
}

private fun isFieldValid(value: String): IsValid {
    return if (value.isNotBlank()) IsValid.Yes else IsValid.No(R.string.payment_configuration_field_not_valid_error)
}
