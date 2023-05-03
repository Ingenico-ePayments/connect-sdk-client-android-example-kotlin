/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.card

import android.app.Application
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.AndroidViewModel
import com.ingenico.connect.android.example.kotlin.compose.R
import com.ingenico.connect.android.example.kotlin.common.PaymentCardValidationErrorMessageMapper
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.CARD_HOLDER
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.CARD_NUMBER
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.EXPIRY_DATE
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.SECURITY_NUMBER
import com.ingenico.connect.android.example.kotlin.compose.card.textfield.CardNumberTextFieldState
import com.ingenico.connect.android.example.kotlin.compose.card.textfield.CardTextFieldState
import com.ingenico.connect.android.example.kotlin.compose.components.CheckBoxField
import com.ingenico.connect.android.example.kotlin.compose.components.TextFieldState
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.AccountOnFile
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProductField
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationErrorMessage

class CardScreenViewModel(application: Application) : AndroidViewModel(application) {

    var cardFields by mutableStateOf(CardFields())
        private set

    var isAccountOnFileDataLoaded = false

    /**
     * When the payment product fields properties change.
     * Update the card fields with the new properties values.
     */
    fun updateFields(
        paymentProductFields: List<PaymentProductField>,
        logoUrl: String,
        accountOnFile: AccountOnFile?
    ) {
        paymentProductFields.forEach { paymentProductField ->
            when (paymentProductField.id) {
                cardFields.cardNumberField.id -> {
                    updateCardNumberField(logoUrl, accountOnFile, paymentProductField)
                }

                cardFields.expiryDateField.id -> {
                    updateExpiryDateField(accountOnFile, paymentProductField)
                }

                cardFields.securityNumberField.id -> {
                   updateSecurityNumberField(accountOnFile, paymentProductField)
                }

                cardFields.cardHolderField.id -> {
                    updateCardHolderField(accountOnFile, paymentProductField)
                }
            }
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) isAccountOnFileDataLoaded = true
    }

    private fun updateCardNumberField(
        logoUrl: String,
        accountOnFile: AccountOnFile?,
        paymentProductField: PaymentProductField
    ) {
        cardFields.cardNumberField.apply {
            label = paymentProductField.displayHints.placeholderLabel
            mask = paymentProductField.displayHints.mask
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            trailingImageUrl = logoUrl
            this.paymentProductField = paymentProductField
        }

        accountOnFileAttributes(
            accountOnFile,
            paymentProductField.id,
            cardFields.cardNumberField
        )
    }

    private fun updateExpiryDateField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.expiryDateField.apply {
            label = paymentProductField.displayHints.placeholderLabel
            mask = paymentProductField.displayHints.mask
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            this.paymentProductField = paymentProductField
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.expiryDateField
            )
        }
    }

    private fun updateSecurityNumberField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.securityNumberField.apply {
            label = paymentProductField.displayHints.placeholderLabel
            mask = paymentProductField.displayHints.mask
            maxSize = paymentProductField.dataRestrictions.validator.length.maxLength
            tooltipImageUrl = paymentProductField.displayHints.tooltip.imageURL
            tooltipText = paymentProductField.displayHints.tooltip.label
            this.paymentProductField = paymentProductField
        }

        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.securityNumberField
            )
        }
    }

    private fun updateCardHolderField(accountOnFile: AccountOnFile?, paymentProductField: PaymentProductField) {
        cardFields.cardHolderField.apply {
            label = paymentProductField.displayHints.placeholderLabel
            this.paymentProductField = paymentProductField
        }
        if (accountOnFile != null && !isAccountOnFileDataLoaded) {
            accountOnFileAttributes(
                accountOnFile,
                paymentProductField.id,
                cardFields.cardHolderField
            )
        }
    }

    /**
     * Update field errors after modification in one of the fields.
     */
    fun setFieldErrors(fieldErrors: List<ValidationErrorMessage>) { cardFields.cardNumberField.networkErrorMessage = ""
        cardFields.expiryDateField.networkErrorMessage = ""
        cardFields.securityNumberField.networkErrorMessage = ""
        cardFields.cardHolderField.networkErrorMessage = ""
        fieldErrors.forEach { validationErrorMessage ->
            val errorMessage =
                PaymentCardValidationErrorMessageMapper.mapValidationErrorMessageToString(
                    getApplication<Application>().applicationContext,
                    validationErrorMessage
                )
            when (validationErrorMessage.paymentProductFieldId) {
                cardFields.cardNumberField.id -> {
                    cardFields.cardNumberField.networkErrorMessage = errorMessage
                }
                cardFields.expiryDateField.id -> {
                    cardFields.expiryDateField.networkErrorMessage = errorMessage
                }
                cardFields.securityNumberField.id -> {
                    cardFields.securityNumberField.networkErrorMessage = errorMessage
                }
                cardFields.cardHolderField.id -> {
                    cardFields.cardHolderField.networkErrorMessage = errorMessage
                }
            }
        }
    }

    /**
     * Disable/Enable all fields
     */
    fun cardFieldsEnabled(enabled: Boolean) {
        cardFields.apply {
            cardNumberField.enabled = enabled
            expiryDateField.enabled = enabled
            securityNumberField.enabled = enabled
            cardHolderField.enabled = enabled
        }
    }

    private fun accountOnFileAttributes(
        accountOnFile: AccountOnFile?,
        paymentProductFieldId: String,
        textFieldState: TextFieldState
    ) {
        accountOnFile?.let {
            cardFields.rememberCardField.visible.value = false
            accountOnFile.attributes.firstOrNull { it.key == paymentProductFieldId }
                ?.let { attribute ->
                    textFieldState.text = if (paymentProductFieldId == cardFields.cardNumberField.id) {
                        cardFields.cardNumberField.mask =
                            accountOnFile.displayHints.labelTemplate[0].mask.replace(
                                "9",
                                "*"
                            )
                        accountOnFile.label
                    } else {
                        attribute.value
                    }

                    if (!attribute.isEditingAllowed) {
                        textFieldState.enabled = false
                    }
                }
        }
    }
}

data class CardFields(
    val cardNumberField: CardNumberTextFieldState = CardNumberTextFieldState(
        leadingIcon = Icons.Filled.CreditCard,
        id = CARD_NUMBER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)

    ),
    val expiryDateField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.CalendarToday,
        id = EXPIRY_DATE,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    ),
    val securityNumberField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.Lock,
        trailingIcon = Icons.Outlined.Info,
        id = SECURITY_NUMBER,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    ),
    val cardHolderField: CardTextFieldState = CardTextFieldState(
        leadingIcon = Icons.Filled.Person,
        id = CARD_HOLDER
    ),
    val rememberCardField: CheckBoxField = CheckBoxField(R.string.gc_app_paymentProductDetails_rememberMe)
)

