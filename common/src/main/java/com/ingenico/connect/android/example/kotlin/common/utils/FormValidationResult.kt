/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common.utils

import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationErrorMessage

/**
 * This class is an example how to store the result of a form validation.
 */
sealed class FormValidationResult {
    data class Invalid(val exceptions: List<Exception>?) : FormValidationResult()
    data class InvalidWithValidationErrorMessage(val exceptions: List<ValidationErrorMessage>) : FormValidationResult()
    object Valid : FormValidationResult()
    object NotValidated: FormValidationResult()
}
