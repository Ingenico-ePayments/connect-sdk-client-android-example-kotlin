/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.card

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.ingenico.connect.android.example.kotlin.xml.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProductField
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields.PreferredInputType.DATE_PICKER
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields.PreferredInputType.EMAIL_ADDRESS_KEYBOARD
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields.PreferredInputType.INTEGER_KEYBOARD
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields.PreferredInputType.PHONE_NUMBER_KEYBOARD
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.displayhints.DisplayHintsProductFields.PreferredInputType.STRING_KEYBOARD
import com.squareup.picasso.Picasso

/**
 * Custom view class for displaying a cardField.
 */
class PaymentCardField(context: Context, attributeSet: AttributeSet) : ConstraintLayout(context, attributeSet) {

    private var cardFieldAfterTextChangedListener: CardFieldAfterTextChangedListener? = null
    private var paymentCardMaskTextWatcher: PaymentCardMaskTextWatcher? = null
    lateinit var paymentProductField: PaymentProductField

    private val cardField: ConstraintLayout
    private var cardFieldTextInputLayout: TextInputLayout
    private var cardFieldTextInputEditText: TextInputEditText
    private var cardFieldProgressBar: ProgressBar
    private val cardFieldImageView: ImageView
    private var lastCheckedCardValue = ""
    private var isIssuerIdentificationNumberListenerInitialized = false
    private var isAfterTextChangedListenerInitialized = false

    init {
        inflate(context, R.layout.view_payment_card_field, this)
        cardField = findViewById(R.id.paymentCardField)
        cardFieldTextInputLayout = findViewById(R.id.paymentCardFieldTextInputLayout)
        cardFieldTextInputEditText = findViewById(R.id.paymentCardFieldTextInputEditText)
        cardFieldProgressBar = findViewById(R.id.paymentCardFieldProgressBar)
        cardFieldImageView = findViewById(R.id.paymentCardFieldImageView)

        // Set attributes parameters from file
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.PaymentCardField, 0, 0).apply {
            try {
                cardFieldTextInputLayout.startIconDrawable = getDrawable(R.styleable.PaymentCardField_startIcon)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Init function for this field.
     * Essential for the proper functioning of this field.
     */
    fun setPaymentProductField(
        paymentProductField: PaymentProductField,
        cardFieldAfterTextChangedListener: CardFieldAfterTextChangedListener
    ) {
        this.paymentProductField = paymentProductField
        this.cardFieldAfterTextChangedListener = cardFieldAfterTextChangedListener
        cardField.visibility = View.VISIBLE
        cardFieldTextInputEditText.hint = this.paymentProductField.displayHints.placeholderLabel
        setEditTextInputType(this.paymentProductField.displayHints.preferredInputType)
        addTooltipWhenAvailable()
        addMaskTextWatcherWhenAvailable()
        setAfterTextChangedListener()
        hideError()
        invalidate()
        requestLayout()
    }

    /**
     * Provides updates when iin changed.
     * Only necessary for the card number field.
     */
    fun setIssuerIdentificationNumberListener() {
        if (!isIssuerIdentificationNumberListenerInitialized) {
            isIssuerIdentificationNumberListenerInitialized = true

            cardFieldTextInputEditText.doAfterTextChanged { editable ->
                val currentCardNumber = editable.toString().replace(" ", "")
                val currentFormattedCardNumber = (currentCardNumber + "xxxxxxxx").take(8)

                if (currentCardNumber.length >= 6 && currentFormattedCardNumber != lastCheckedCardValue) {
                    cardFieldAfterTextChangedListener?.issuerIdentificationNumberChanged(currentCardNumber)
                }
                lastCheckedCardValue = currentFormattedCardNumber
            }
        }
    }

    fun setLoadingIndicator() {
        cardFieldTextInputLayout.isErrorEnabled = false
        cardFieldTextInputLayout.error = null
        cardFieldImageView.visibility = View.GONE
        cardFieldProgressBar.visibility = View.VISIBLE
    }

    fun setImage(imageURL: String) {
        cardFieldProgressBar.visibility = View.GONE
        Picasso.get()
            .load(imageURL)
            .into(cardFieldImageView)
        cardFieldImageView.visibility = View.VISIBLE
    }

    fun setError(errorText: String) {
        if (errorText != cardFieldTextInputLayout.error.toString()) {
            cardFieldImageView.visibility = View.GONE
            cardFieldProgressBar.visibility = View.GONE
            cardFieldTextInputLayout.isErrorEnabled = true
            cardFieldTextInputLayout.error = errorText
        }
    }

    fun hideError() {
        cardFieldTextInputLayout.isErrorEnabled = false
        cardFieldTextInputLayout.error = null
        cardFieldImageView.visibility = View.VISIBLE
    }

    fun hideAllToolTips() {
        cardFieldTextInputLayout.isErrorEnabled = false
        cardFieldTextInputLayout.error = null
        cardFieldProgressBar.visibility = View.GONE
        cardFieldImageView.visibility = View.GONE
    }

    fun getPaymentProductFieldValue(): String {
        return cardFieldTextInputEditText.text.toString()
    }

    fun setPaymentProductFieldValue(value: String) {
        cardFieldTextInputEditText.setText(value)
    }

    fun removePaymentCardMaskTextWatcher() {
        paymentCardMaskTextWatcher?.let {
            cardFieldTextInputEditText.removeTextChangedListener(it)
        }
        paymentCardMaskTextWatcher = null
    }

    private fun setEditTextInputType(preferredInputType: DisplayHintsProductFields.PreferredInputType) {
        val inputType = when (preferredInputType) {
            INTEGER_KEYBOARD -> InputType.TYPE_CLASS_NUMBER
            STRING_KEYBOARD -> InputType.TYPE_CLASS_TEXT
            PHONE_NUMBER_KEYBOARD -> InputType.TYPE_CLASS_PHONE
            EMAIL_ADDRESS_KEYBOARD -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            DATE_PICKER -> InputType.TYPE_DATETIME_VARIATION_DATE
            else -> InputType.TYPE_CLASS_TEXT
        }
        cardFieldTextInputEditText.inputType = inputType
    }

    private fun addTooltipWhenAvailable() {
        if (paymentProductField.displayHints.tooltip != null) {
            cardFieldImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_info))
            cardFieldImageView.visibility = View.VISIBLE
            cardFieldImageView.setOnClickListener {
                cardFieldAfterTextChangedListener?.onToolTipClicked(paymentProductField)
            }
        }
    }

    /**
     * Add a mask to your field. If no mask is available, a maximum number of characters is set.
     */
    private fun addMaskTextWatcherWhenAvailable() {
        paymentCardMaskTextWatcher?.let {
            cardFieldTextInputEditText.removeTextChangedListener(it)
        }

        if (paymentProductField.displayHints.mask != null) {
            paymentCardMaskTextWatcher = PaymentCardMaskTextWatcher(cardFieldTextInputEditText, paymentProductField)
            cardFieldTextInputEditText.addTextChangedListener(paymentCardMaskTextWatcher)
            cardFieldTextInputEditText.setText(cardFieldTextInputEditText.text.toString())
        } else {
            cardFieldTextInputEditText.filters += InputFilter.LengthFilter(
                paymentProductField.dataRestrictions.validator.length.maxLength
            )
        }
    }

    /**
     * Listens for text changes. Is used to validate this field.
     */
    private fun setAfterTextChangedListener() {
        if (!isAfterTextChangedListenerInitialized) {
            isAfterTextChangedListenerInitialized = true
            cardFieldTextInputEditText.doAfterTextChanged { editable ->
                cardFieldAfterTextChangedListener?.afterTextChanged(paymentProductField, editable.toString())
            }
        }
    }
}

/**
 * Interface that listens for specific text changes.
 * This interface must be implemented in an activity/fragment so that you can validate fields and process iin changes.
 */
interface CardFieldAfterTextChangedListener {
    fun afterTextChanged(paymentProductField: PaymentProductField, value: String)
    fun issuerIdentificationNumberChanged(currentCardNumber: String)
    fun onToolTipClicked(paymentProductField: PaymentProductField)
}
