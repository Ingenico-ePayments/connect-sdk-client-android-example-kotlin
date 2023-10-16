/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.ingenico.connect.android.example.kotlin.xml.R
import com.ingenico.connect.android.example.kotlin.xml.databinding.FragmentPaymentCardBinding
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.deepForEach
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.hideKeyboard
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.CARD_HOLDER
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.CARD_NUMBER
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.EXPIRY_DATE
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.SECURITY_NUMBER
import com.ingenico.connect.android.example.kotlin.common.utils.FormValidationResult
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ingenico.connect.android.example.kotlin.common.PaymentCardUiState
import com.ingenico.connect.android.example.kotlin.common.PaymentCardViewModel
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.ingenico.connect.android.example.kotlin.common.PaymentCardValidationErrorMessageMapper
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.formatter.StringFormatter
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.EncryptedPaymentRequest
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.iin.IinStatus
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.AccountOnFile
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.PaymentProductField
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.Tooltip
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.validation.ValidationErrorMessage
import com.squareup.picasso.Picasso

class PaymentCardFragment : Fragment() {

    private var _binding: FragmentPaymentCardBinding? = null
    private val binding get() = _binding!!

    private val paymentCardViewModel: PaymentCardViewModel by viewModels()
    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    private val implementedPaymentProductFields = mutableMapOf<String, PaymentCardField>()

    private var accountOnFilePaymentProductId: String? = null

    // Use this listener for all card fields to receive updates when a text field changes.
    // It is not necessary to create a separate listener for each field.
    private val cardFieldAfterTextChangedListener = object : CardFieldAfterTextChangedListener {
        override fun afterTextChanged(paymentProductField: PaymentProductField, value: String) {
            paymentCardViewModel.fieldChanged(paymentProductField, value)
        }

        override fun issuerIdentificationNumberChanged(currentCardNumber: String) {
            if (accountOnFilePaymentProductId == null) {
                paymentCardViewModel.getPaymentProductId(currentCardNumber)
            }
        }

        override fun onToolTipClicked(paymentProductField: PaymentProductField) {
            view?.clearFocus()
            showCardFieldTooltipBottomSheetDialog(paymentProductField.displayHints.tooltip)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shouldShowFormLoadingIndicator(true)
        // bind a productFieldId to an cardField
        implementedPaymentProductFields.putAll(
            mapOf(
                CARD_NUMBER to binding.paymentCardFieldCardNumber,
                EXPIRY_DATE to binding.paymentCardFieldCardExpiryDate,
                SECURITY_NUMBER to binding.paymentCardFieldSecurityCode,
                CARD_HOLDER to binding.paymentCardFieldCardholderName
            )
        )

        if (paymentSharedViewModel.selectedPaymentProduct is AccountOnFile){
            accountOnFilePaymentProductId =
                (paymentSharedViewModel.selectedPaymentProduct as AccountOnFile).paymentProductId
        }

        paymentCardViewModel.getPaymentProduct(
            paymentSharedViewModel.selectedPaymentProduct
        )

        initLayout()
        observePaymentProductFieldsUiState()
        observeFormValidationResult()
        observeEncryptedPaymentRequestStatus()
    }

    override fun onResume() {
        super.onResume()
        paymentSharedViewModel.activePaymentScreen.value = PaymentScreen.CARD
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initLayout() {
        binding.btnPaymentCardPayProduct.loadingButtonMaterialButton.setOnClickListener {
            view?.clearFocus()
            requireContext().hideKeyboard(it)
            paymentCardViewModel.onPayClicked()
        }

        if (accountOnFilePaymentProductId != null) {
            binding.cbPaymentCardSaveCard.visibility = View.GONE
        } else {
            binding.cbPaymentCardSaveCard.setOnCheckedChangeListener { _, isChecked ->
                paymentCardViewModel.saveCardForLater(isChecked)
            }
        }

        // init listener for issuer identification number changes
        binding.paymentCardFieldCardNumber.setIssuerIdentificationNumberListener()
    }

    private fun observePaymentProductFieldsUiState() {
        paymentCardViewModel.paymentProductFieldsUiState.observe(viewLifecycleOwner) { paymentCardUiState ->
            when (paymentCardUiState) {
                is PaymentCardUiState.ApiError -> {
                    shouldShowFormLoadingIndicator(false)
                    paymentSharedViewModel.globalErrorMessage.value = paymentCardUiState.apiError.errors.first().message
                }
                is PaymentCardUiState.IinFailed -> {
                    when (paymentCardUiState.throwable.message) {
                        IinStatus.UNKNOWN.name -> binding.paymentCardFieldCardNumber.setError(
                            getString(R.string.gc_general_paymentProductFields_validationErrors_iin_label)
                        )
                        IinStatus.EXISTING_BUT_NOT_ALLOWED.name -> binding.paymentCardFieldCardNumber.setError(
                            getString(R.string.gc_general_paymentProductFields_validationErrors_allowedInContext_label)
                        )
                        else -> {
                            binding.paymentCardFieldCardNumber.hideAllToolTips()
                            paymentSharedViewModel.globalErrorMessage.value = paymentCardUiState.throwable.message
                        }
                    }
                }
                is PaymentCardUiState.Loading -> {
                    shouldShowFormLoadingIndicator(true)
                }
                is PaymentCardUiState.Success -> {
                    shouldShowFormLoadingIndicator(false)
                    updateCardFields(paymentCardUiState.paymentFields)

                    paymentCardUiState.logoUrl?.let { logoUrl ->
                        binding.paymentCardFieldCardNumber.setImage(
                            ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl.plus(
                                logoUrl
                            )
                        )
                    }

                    paymentCardUiState.accountOnFile?.let { accountOnFile ->
                        updateCardFieldByAccountOnFilePaymentProduct(accountOnFile)
                        binding.cbPaymentCardSaveCard.visibility = View.GONE
                    }
                }
                is PaymentCardUiState.Failed -> {
                    shouldShowFormLoadingIndicator(false)
                    paymentSharedViewModel.globalErrorMessage.value = paymentCardUiState.throwable.message
                }
                PaymentCardUiState.None -> {
                    // Init status; nothing to do here
                }
            }
        }
    }

    private fun observeFormValidationResult() {
        paymentCardViewModel.formValidationResult.observe(viewLifecycleOwner) { formValidationResult ->
            when (formValidationResult) {
                is FormValidationResult.Invalid -> {
                    binding.btnPaymentCardPayProduct.isButtonEnabled = false
                }
                is FormValidationResult.InvalidWithValidationErrorMessage -> {
                    binding.btnPaymentCardPayProduct.isButtonEnabled = false
                    setFieldErrors(formValidationResult.exceptions)
                }
                is FormValidationResult.Valid -> {
                    binding.btnPaymentCardPayProduct.isButtonEnabled = true
                    setFieldErrors(emptyList())
                }
                FormValidationResult.NotValidated -> {
                    // No option, when form is invalid it will always come in the
                    // FormValidationResult.InvalidWithValidationErrorMessage case
                }
            }
        }
    }

    private fun observeEncryptedPaymentRequestStatus() {
        paymentCardViewModel.encryptedPaymentRequestStatus.observe(
            viewLifecycleOwner
        ) { EncryptedPaymentRequestStatus ->
            when (EncryptedPaymentRequestStatus) {
                is Status.ApiError -> {
                    paymentSharedViewModel.globalErrorMessage.value =
                        EncryptedPaymentRequestStatus.apiError.errors.first().message
                    binding.clPaymentCardInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentCardPayProduct.hideLoadingIndicator()
                }
                is Status.Loading -> {
                    binding.clPaymentCardInputForm.deepForEach { isEnabled = false }
                    binding.btnPaymentCardPayProduct.showLoadingIndicator()
                }
                is Status.Success -> {
                    val encryptedFieldsData =
                        (EncryptedPaymentRequestStatus.data as EncryptedPaymentRequest).encryptedFields
                    findNavController().navigate(
                        PaymentCardFragmentDirections.navigateToPaymentResultFragment(encryptedFieldsData)
                    )
                }
                is Status.Failed -> {
                    paymentSharedViewModel.globalErrorMessage.value = EncryptedPaymentRequestStatus.throwable.message
                    binding.clPaymentCardInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentCardPayProduct.hideLoadingIndicator()
                }
                is Status.None -> {
                    // Init status; nothing to do here
                }
            }
        }
    }

    private fun shouldShowFormLoadingIndicator(showLoadingIndicator: Boolean) {
        if (showLoadingIndicator) {
            binding.clPaymentCardInputForm.visibility = View.GONE
            binding.pbPaymentCardLoadingIndicator.visibility = View.VISIBLE
        } else {
            binding.pbPaymentCardLoadingIndicator.visibility = View.GONE
            binding.clPaymentCardInputForm.visibility = View.VISIBLE
        }
    }

    private fun setFieldErrors(fieldErrors: List<ValidationErrorMessage>) {
        implementedPaymentProductFields.forEach { paymentProductsField ->
            paymentProductsField.value.hideError()
            fieldErrors.forEach { validationErrorMessage ->
                if (paymentProductsField.key == validationErrorMessage.paymentProductFieldId) {
                    paymentProductsField.value.setError(
                        PaymentCardValidationErrorMessageMapper.mapValidationErrorMessageToString(
                            requireContext(),
                            validationErrorMessage
                        )
                    )
                    return
                }
            }
        }
    }

    private fun updateCardFields(paymentProductFields: List<PaymentProductField>) {
        paymentProductFields.forEach { paymentProductField ->
            updateCardFieldById(paymentProductField)
        }
    }

    private fun updateCardFieldById(paymentProductField: PaymentProductField) {
        implementedPaymentProductFields.forEach { implementedPaymentProductsField ->
            if (implementedPaymentProductsField.key == paymentProductField.id) {
                paymentCardViewModel.updateValueInPaymentRequest(
                    paymentProductField,
                    implementedPaymentProductsField.value.getPaymentProductFieldValue()
                )
                implementedPaymentProductsField.value.setPaymentProductField(
                    paymentProductField,
                    cardFieldAfterTextChangedListener
                )
            }
        }
    }

    private fun updateCardFieldByAccountOnFilePaymentProduct(accountOnFile: AccountOnFile) {
        implementedPaymentProductFields.forEach { implementedPaymentProductField ->
            accountOnFile.attributes.firstOrNull { it.key == implementedPaymentProductField.key }
                ?.let { attribute ->
                    if (!attribute.isEditingAllowed) {
                        implementedPaymentProductField.value.removePaymentCardMaskTextWatcher()
                        implementedPaymentProductField.value.deepForEach { isEnabled = false }
                    }

                    when (attribute.key) {
                        CARD_NUMBER -> {
                            val formattedValue = StringFormatter().applyMask(
                                implementedPaymentProductField.value.paymentProductField.displayHints.mask.replace(
                                    "9",
                                    "*"
                                ),
                                accountOnFile.label
                            )

                            implementedPaymentProductField.value.setPaymentProductFieldValue(
                                formattedValue
                            )
                        }
                        EXPIRY_DATE -> {
                            implementedPaymentProductField.value.setPaymentProductFieldValue(
                                attribute.value.replaceRange(2, 2, "/")
                            )
                        }
                        else -> {
                            implementedPaymentProductField.value.setPaymentProductFieldValue(
                                attribute.value
                            )
                        }
                    }
                }
        }
    }

    private fun showCardFieldTooltipBottomSheetDialog(tooltip: Tooltip) {
        BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottomsheet_information)
            findViewById<TextView>(R.id.tvInformationText)?.apply {
                text = tooltip.label
            }
            val informationImage = findViewById<ImageView>(R.id.ivInformationImage)?.apply {
                visibility = View.VISIBLE
            }
            Picasso.get()
                .load(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl.plus(tooltip.imageURL))
                .into(informationImage)

        }.show()
    }
}
