/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ingenico.connect.android.example.kotlin.xml.databinding.FragmentPaymentConfigurationBinding
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.getDataFromClipboard
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.hideKeyboard
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.ingenico.connect.android.example.kotlin.common.googlepay.GooglePayConfiguration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.AmountOfMoney
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.PaymentContext
import java.util.*
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.getCurrentLocale
import com.ingenico.connect.android.example.kotlin.common.*
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.deepForEach
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.configuration.SessionConfiguration

class PaymentConfigurationFragment : Fragment() {

    private var _binding: FragmentPaymentConfigurationBinding? = null
    private val binding get() = _binding!!

    private lateinit var configurationInputFields: List<Pair<TextInputLayout, TextInputEditText>>

    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPaymentConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetInputData()
        initLayout()
        prefillPaymentConfigurationFields()
        initInputFieldsDrawableEndClickListeners()
        observePaymentSharedViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        paymentSharedViewModel.activePaymentScreen.value = PaymentScreen.CONFIGURATION
    }

    private fun resetInputData() {
        paymentSharedViewModel.globalErrorMessage.value = null
        paymentSharedViewModel.paymentProductsStatus.value = null
    }

    private fun initLayout() {
         configurationInputFields = listOf(
                Pair(binding.tilPaymentConfigurationClientSessionId, binding.etPaymentConfigurationClientSessionId),
                Pair(binding.tilPaymentConfigurationCustomerId, binding.etPaymentConfigurationCustomerId),
                Pair(binding.tilPaymentConfigurationClientApiUrl, binding.etPaymentConfigurationClientApiUrl),
                Pair(binding.tilPaymentConfigurationAssetsUrl, binding.etPaymentConfigurationAssetsUrl),
                Pair(binding.tilPaymentConfigurationAmount, binding.etPaymentConfigurationAmount),
                Pair(binding.tilPaymentConfigurationCountryCode, binding.etPaymentConfigurationCountryCode),
                Pair(binding.tilPaymentConfigurationCurrencyCode, binding.etPaymentConfigurationCurrencyCode),
                Pair(binding.tilPaymentConfigurationMerchantId, binding.etPaymentConfigurationMerchantId),
                Pair(binding.tilPaymentConfigurationMerchantName, binding.etPaymentConfigurationMerchantName))

        binding.btnPaymentConfigurationProceedToCheckout.loadingButtonMaterialButton.setOnClickListener {
            view?.clearFocus()
            requireContext().hideKeyboard(it)
            validatePaymentConfiguration()
        }
        binding.btnPaymentConfigurationClientSessionJsonResponse.setOnClickListener {
            view?.clearFocus()
            requireContext().hideKeyboard(it)
            parseJsonDataFromClipboard()
        }

        // prefill country and currency
        val currentLocale = Locale.getDefault()
        binding.apply {
            etPaymentConfigurationCountryCode.setText(currentLocale.country)
            etPaymentConfigurationCurrencyCode.setText(Currency.getInstance(currentLocale)
                .toString())
            cbPaymentConfigurationGooglePay.setOnCheckedChangeListener { _, isChecked ->
                clPaymentConfigurationGooglePayInputContainer.visibility =
                    if (isChecked) View.VISIBLE else View.GONE
            }
        }

        initFieldsRemoveErrorAfterTextChange()

    }

    private fun initInputFieldsDrawableEndClickListeners() {
        binding.apply {
            tilPaymentConfigurationClientSessionId.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_client_session_id_helper_text))
            }

            tilPaymentConfigurationCustomerId.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog((getString(R.string.payment_configuration_customer_id_helper_text)))
            }

            tilPaymentConfigurationClientApiUrl.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_clientApiUrl_helper_text))
            }

            tilPaymentConfigurationAssetsUrl.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_assetsUrl_helper_text))
            }

            tilPaymentConfigurationAmount.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_amount_helper_text))
            }
            tilPaymentConfigurationCountryCode.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_country_code_helper_text))
            }
            tilPaymentConfigurationCurrencyCode.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_currency_code_helper_text))
            }
            tilPaymentConfigurationMerchantId.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_merchant_name_helper_text))
            }
            tilPaymentConfigurationMerchantName.setEndIconOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_merchant_name_helper_text))
            }
            ivPaymentConfigurationRecurringPaymentHelperIcon.setOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_recurring_payment_helper_text))
            }
            ivPaymentConfigurationGroupPaymentProductsHelperIcon.setOnClickListener {
                showInputFieldExplanationTextBottomSheetDialog(getString(R.string.payment_configuration_group_payment_products_helper_text))
            }
        }
    }

    private fun prefillPaymentConfigurationFields() {
        try {
                binding.apply {
                    etPaymentConfigurationClientSessionId.setText(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.clientSessionId)
                    etPaymentConfigurationCustomerId.setText(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.customerId)
                    etPaymentConfigurationClientApiUrl.setText(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.clientApiUrl)
                    etPaymentConfigurationAssetsUrl.setText(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl)
                    etPaymentConfigurationAmount.setText(ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.amount.toString())
                    etPaymentConfigurationCountryCode.setText(ConnectSDK.getPaymentConfiguration().paymentContext.countryCode)
                    etPaymentConfigurationCurrencyCode.setText(ConnectSDK.getPaymentConfiguration().paymentContext.amountOfMoney.currencyCode)
                    cbPaymentConfigurationGroupPaymentProducts.isChecked = ConnectSDK.getPaymentConfiguration().groupPaymentProducts
                    cbPaymentConfigurationGooglePay.isChecked = paymentSharedViewModel.googlePayConfiguration.configureGooglePay
                    etPaymentConfigurationMerchantId.setText(paymentSharedViewModel.googlePayConfiguration.merchantId)
                    etPaymentConfigurationMerchantName.setText(paymentSharedViewModel.googlePayConfiguration.merchantName)
                }
        } catch (exception: Exception) {
            // SDK is not initialized, fields are not prefilled
        }
    }

    private fun validatePaymentConfiguration() {
        var isFormValid = true
        configurationInputFields.forEach {
            if (it.second.text?.toString()?.isBlank() == true && it.first.id != binding.tilPaymentConfigurationMerchantId.id && it.first.id != binding.tilPaymentConfigurationMerchantName.id) {
                it.first.error = getString(R.string.payment_configuration_field_not_valid_error)
                isFormValid = false
            }

            if (binding.cbPaymentConfigurationGooglePay.isChecked) {
                if (it.first.id == binding.tilPaymentConfigurationMerchantId.id || it.first.id == binding.tilPaymentConfigurationMerchantName.id) {
                    if (it.second.text?.isBlank() == true) {
                        it.first.error = getString(R.string.payment_configuration_field_not_valid_error)
                        isFormValid = false
                    }
                }
            }
        }

        if (isFormValid){
            val amount = try {
                binding.etPaymentConfigurationAmount.text.toString().toLong()
            } catch (e: NumberFormatException) {
                0
            }

            if (binding.cbPaymentConfigurationGooglePay.isChecked){
                paymentSharedViewModel.googlePayConfiguration = GooglePayConfiguration(true,  binding.etPaymentConfigurationMerchantId.text.toString(),  binding.etPaymentConfigurationMerchantName.text.toString())
            }

            paymentSharedViewModel.configureConnectSDK(SessionConfiguration(
                binding.etPaymentConfigurationClientSessionId.text.toString(),
                binding.etPaymentConfigurationCustomerId.text.toString(),
                binding.etPaymentConfigurationClientApiUrl.text.toString(),
                binding.etPaymentConfigurationAssetsUrl.text.toString()
            ),
                PaymentContext(
                    AmountOfMoney(
                        amount, binding.etPaymentConfigurationCurrencyCode.text.toString()
                    ),
                    binding.etPaymentConfigurationCountryCode.text.toString(),
                    false,
                    context?.getCurrentLocale()
                ),
                binding.cbPaymentConfigurationGroupPaymentProducts.isChecked
            )
        }
    }
    private fun observePaymentSharedViewModel() {
        paymentSharedViewModel.paymentProductsStatus.observe(viewLifecycleOwner) { paymentProductStatus ->
            when (paymentProductStatus) {
                is Status.ApiError -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentConfigurationProceedToCheckout.hideLoadingIndicator()
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.apiError.errors.first().message
                }
                is Status.Loading -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = false }
                    binding.btnPaymentConfigurationProceedToCheckout.showLoadingIndicator()
                }
                is Status.Success -> {
                    findNavController().navigate(PaymentConfigurationFragmentDirections.navigateToPaymentProductFragment())
                }
                is Status.Failed -> {
                    binding.clPaymentConfigurationInputForm.deepForEach { isEnabled = true }
                    binding.btnPaymentConfigurationProceedToCheckout.hideLoadingIndicator()
                    paymentSharedViewModel.globalErrorMessage.value =
                        paymentProductStatus.throwable.message
                }
                Status.None -> {
                    // Init status; nothing to do here
                }
            }
        }
    }
    private fun parseJsonDataFromClipboard() {
        val jsonString = context?.getDataFromClipboard()
        try {
            Gson().fromJson(jsonString, SessionConfiguration::class.java).apply {
                binding.etPaymentConfigurationClientSessionId.setText(this.clientSessionId)
                binding.etPaymentConfigurationCustomerId.setText(this.customerId)
                binding.etPaymentConfigurationClientApiUrl.setText(this.clientApiUrl)
                binding.etPaymentConfigurationAssetsUrl.setText(this.assetUrl)
            }
        } catch (exception: Exception) {
            paymentSharedViewModel.globalErrorMessage.value = "Json data from clipboard can't be parsed."
        }
    }

    private fun initFieldsRemoveErrorAfterTextChange() {
        configurationInputFields.forEach { field ->
            field.second.doAfterTextChanged { editable ->
                if (editable?.isNotBlank() == true) {
                    field.first.isErrorEnabled = false
                    field.first.error = null
                    return@doAfterTextChanged
                }
            }
        }
    }

    private fun showInputFieldExplanationTextBottomSheetDialog(explanationText: String) {
        BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottomsheet_information)
            findViewById<TextView>(R.id.tvInformationText)?.apply {
                text = explanationText
            }
        }.show()
    }
}
