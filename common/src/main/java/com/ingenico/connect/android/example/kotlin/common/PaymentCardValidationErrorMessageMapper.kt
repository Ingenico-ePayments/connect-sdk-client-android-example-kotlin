/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common

import android.content.Context
import com.ingenico.connect.android.example.kotlin.common.utils.extensions.getStringByName
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationErrorMessage
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationRuleLength
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationRuleRange

/**
 * Maps card validation ErrorMessageId to complete error formatted in the current selected locale
 */
object PaymentCardValidationErrorMessageMapper {

    private const val TRANSLATION_PREFIX_VALIDATION = "gc.general.paymentProductFields.validationErrors."
    private const val TRANSLATION_POSTFIX_LABEL = ".label"

    private const val VALIDATION_LENGTH_EXCEPTION_EXACT = "length.exact"
    private const val VALIDATION_LENGTH_EXCEPTION_MAX = "length.max"
    private const val VALIDATION_LENGTH_EXCEPTION_BETWEEN = "length.between"
    private const val VALIDATION_LENGTH_MIN_PLACEHOLDER = "{minLength}"
    private const val VALIDATION_LENGTH_MAX_PLACEHOLDER = "{maxLength}"

    fun mapValidationErrorMessageToString(context: Context, validationErrorMessage: ValidationErrorMessage): String {
        var errorMessage =
            context.getStringByName(
                "$TRANSLATION_PREFIX_VALIDATION${validationErrorMessage.errorMessage}$TRANSLATION_POSTFIX_LABEL"
            ) ?: validationErrorMessage.errorMessage
        return when (val validationRule = validationErrorMessage.rule) {
            is ValidationRuleLength -> {
                errorMessage =
                    context.getStringByName(
                        """
                        $TRANSLATION_PREFIX_VALIDATION${mapLengthExceptionToString(validationRule)}$TRANSLATION_POSTFIX_LABEL 
                        """.trimIndent()
                    ) ?: validationErrorMessage.errorMessage
                replaceLengthPlaceholders(errorMessage, validationRule.minLength, validationRule.maxLength)
            }
            is ValidationRuleRange -> {
                replaceLengthPlaceholders(errorMessage, validationRule.minValue, validationRule.maxValue)
            }

            else -> {
                errorMessage
            }
        }
    }

    private fun replaceLengthPlaceholders(errorMessage: String, minLength: Int, maxLength: Int): String {
        var errorMessageWithoutPlaceholders =
            errorMessage.replace(VALIDATION_LENGTH_MIN_PLACEHOLDER, minLength.toString())
        errorMessageWithoutPlaceholders = errorMessageWithoutPlaceholders.replace(
            VALIDATION_LENGTH_MAX_PLACEHOLDER, maxLength.toString())
        return errorMessageWithoutPlaceholders
    }

    private fun mapLengthExceptionToString(validationRuleLength: ValidationRuleLength): String {
        return when (validationRuleLength.minLength) {
            validationRuleLength.maxLength -> {
                VALIDATION_LENGTH_EXCEPTION_EXACT
            }
            null, 0 -> {
                VALIDATION_LENGTH_EXCEPTION_MAX
            }
            else -> {
                VALIDATION_LENGTH_EXCEPTION_BETWEEN
            }
        }
    }
}
