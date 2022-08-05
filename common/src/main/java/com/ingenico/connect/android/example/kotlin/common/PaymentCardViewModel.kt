/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.SECURITY_NUMBER
import com.ingenico.connect.android.example.kotlin.common.utils.FormValidationResult
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.EncryptedPaymentRequest
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.PaymentRequest
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.iin.IinDetailsResponse
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.iin.IinStatus
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.*
import com.ingenico.connect.gateway.sdk.client.android.sdk.network.ApiErrorResponse

/**
 * ViewModel for retrieving (grouped) payment products. Also validates all card fields
 */
class PaymentCardViewModel(application: Application) : AndroidViewModel(application) {

    val encryptedPaymentRequestStatus = MutableLiveData<Status>()
    private val paymentRequest = PaymentRequest()

    val paymentProductFieldsUiState = MutableLiveData<PaymentCardUiState>(PaymentCardUiState.None)

    // When property 'liveFormValidating' is true, card fields are validated after each input change
    private var liveFormValidating = false
    val formValidationResult = MutableLiveData<FormValidationResult>()

    /**
     * Based on the selected payment product, the correct data is retrieved from the SDK and parameters are set correctly.
     */
    fun getPaymentProduct(
        selectedPaymentProduct: Any?,
    ) {
        when (selectedPaymentProduct) {
            is BasicPaymentProductGroup -> {
                getPaymentProductGroup(selectedPaymentProduct.id)
            }
            is BasicPaymentProduct -> {
                getPaymentProductDetails(
                    selectedPaymentProduct.id,
                    null,
                    true
                )
            }
            is AccountOnFile -> {
                getPaymentProductDetails(
                    selectedPaymentProduct.paymentProductId,
                    selectedPaymentProduct.id.toString(),
                    true
                )
            }
        }
    }

    /**
     * A payment product group has a collection of payment products that can be grouped together on a payment product selection page,
     * and a set of fields to render on the payment product details page that allow to determine which payment product of the group the consumer wants to select.
     * We currently support one payment product group named 'cards'.
     */
    private fun getPaymentProductGroup(paymentGroupId: String) {
        paymentProductFieldsUiState.postValue(PaymentCardUiState.Loading)
        ConnectSDK.getClientApi()
            .getPaymentProductGroup(paymentGroupId, { paymentProductGroup: PaymentProductGroup ->
                paymentProductFieldsUiState.postValue(
                    PaymentCardUiState.Success(
                        paymentProductGroup.paymentProductFields,
                        null,
                        null
                    )
                )
            },
                { apiError: ApiErrorResponse ->
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.ApiError(
                            apiError
                        )
                    )
                },
                { failure: Throwable ->
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.Failed(
                            failure
                        )
                    )
                }
            )
    }

    /**
     * After at least 6 digits have been entered(issuerIdentificationNumber) in the card number field, the corresponding card type is searched.
     * The returned 'paymentProductId' can be used to provide visual feedback to the user
     * by showing the appropriate payment product logo and specific card type details.
     */
    fun getPaymentProductId(issuerIdentificationNumber: String) {
        ConnectSDK.getClientApi()
            .getIINDetails(issuerIdentificationNumber, { iinDetailsResponse: IinDetailsResponse ->
                when (iinDetailsResponse.status) {
                    IinStatus.UNKNOWN -> paymentProductFieldsUiState.postValue(PaymentCardUiState.IinFailed(Exception(IinStatus.UNKNOWN.name)))
                    IinStatus.NOT_ENOUGH_DIGITS -> paymentProductFieldsUiState.postValue(PaymentCardUiState.IinFailed(Exception(IinStatus.NOT_ENOUGH_DIGITS.name)))
                    IinStatus.EXISTING_BUT_NOT_ALLOWED -> paymentProductFieldsUiState.postValue(PaymentCardUiState.IinFailed(Exception(IinStatus.EXISTING_BUT_NOT_ALLOWED.name)))
                    IinStatus.SUPPORTED -> getPaymentProductDetails(iinDetailsResponse.paymentProductId, null, false)
                    else -> paymentProductFieldsUiState.postValue(PaymentCardUiState.IinFailed(Exception(IinStatus.UNKNOWN.name)))
                }
            },
                {
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.IinFailed(
                            Exception(IinStatus.UNKNOWN.name)
                        )
                    )
                },
                { failure: Throwable ->
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.Failed(
                            failure
                        )
                    )
                }
            )
    }

    /**
     * Returns all details of a specific payment product. Such as a logo, hints, masking and validation rules.
     *
     * You can use this method if you have first retrieved a payment productId with the 'getPaymentProductId' function
     * or if you have already selected a specific payment product.
     */
    private fun getPaymentProductDetails(
        paymentProductId: String,
        accountOnFileId: String?,
        showLoadingIndicator: Boolean
    ) {
        if (showLoadingIndicator) {
            paymentProductFieldsUiState.postValue(PaymentCardUiState.Loading)
        }
        ConnectSDK.getClientApi()
            .getPaymentProduct(paymentProductId, { paymentProduct: PaymentProduct ->
                paymentProductFieldsUiState.postValue(
                    PaymentCardUiState.Success(
                        paymentProduct.paymentProductFields,
                        paymentProduct.displayHints.logoUrl,
                        accountOnFileId?.let { paymentProduct.getAccountOnFileById(it) }
                    )
                )

                paymentRequest.paymentProduct = paymentProduct
                if (!accountOnFileId.isNullOrBlank() && paymentProduct.getAccountOnFileById(
                        accountOnFileId) != null
                ) {
                    paymentRequest.accountOnFile =
                        paymentProduct.getAccountOnFileById(accountOnFileId)
                }

                if (liveFormValidating) validateAllFields()
            },
                { apiError: ApiErrorResponse ->
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.ApiError(
                            apiError
                        )
                    )
                },
                { failure: Throwable ->
                    paymentProductFieldsUiState.postValue(
                        PaymentCardUiState.Failed(
                            failure
                        )
                    )
                }
            )
    }

    /**
     * When all fields are filled in this function validates all fields and when valid a payment is encrypted.
     * Returns an encryptedPaymentRequest in which all data of the fields is stored and merged into one encrypted key.
     */
    fun onPayClicked() {
        liveFormValidating = true
        if (validateAllFields()) {
            ConnectSDK.encryptPaymentRequest(
                paymentRequest,
                { encryptedPaymentRequest: EncryptedPaymentRequest ->
                    encryptedPaymentRequestStatus.postValue(Status.Success(encryptedPaymentRequest))
                },
                { failure: Throwable ->
                    encryptedPaymentRequestStatus.postValue(Status.Failed(failure)
                    )
                }
            )
        }
    }

    /**
     * When a field is changed validate it.
     */
    fun fieldChanged(paymentProductField: PaymentProductField, value: String) {
        updateValueInPaymentRequest(paymentProductField.id, value)
        if (liveFormValidating) {
           validateAllFields()
        } else {
            shouldEnablePayButton()
        }
    }

    /**
     * When option for saving your card changes update payment request
     */
    fun saveCardForLater(saveCard: Boolean) {
        paymentRequest.tokenize = saveCard
    }

    /**
     * When a field is changed, it must also be updated in the payment request object
     * so that the correct input is used when validating or preparing a payment.
     */
    fun updateValueInPaymentRequest(paymentProductFieldId: String, value: String) {
        if (paymentRequest.accountOnFile != null) {
            if (paymentRequest.accountOnFile.attributes.firstOrNull { it.key == paymentProductFieldId }?.isEditingAllowed == true || paymentProductFieldId == SECURITY_NUMBER) {
                paymentRequest.setValue(paymentProductFieldId, value)
            }
        } else {
            paymentRequest.setValue(paymentProductFieldId, value)
        }
    }

    fun shouldEnablePayButton() {
        if (!liveFormValidating) {
            var allRequiredFieldsNotEmpty = true
            if (paymentRequest.paymentProduct != null && paymentProductFieldsUiState.value is PaymentCardUiState.Success) {
                paymentRequest.values.forEach { paymentRequestValues ->
                    if (paymentRequestValues.value.isNullOrBlank() &&
                        paymentRequest.paymentProduct.getPaymentProductFieldById(
                            paymentRequestValues.key
                        ).dataRestrictions.isRequired
                    ) {
                        allRequiredFieldsNotEmpty = false
                        return@forEach
                    }
                }
            } else {
                allRequiredFieldsNotEmpty = false
            }

            if (allRequiredFieldsNotEmpty) {
                formValidationResult.value = FormValidationResult.Valid
            } else {
                formValidationResult.value = FormValidationResult.Invalid(null)
            }
        }
    }

    private fun validateAllFields(): Boolean {
        val validationErrors = paymentRequest.validate()
        return if (validationErrors.isNullOrEmpty()) {
            formValidationResult.postValue(FormValidationResult.Valid)
            true
        } else {
            formValidationResult.postValue(FormValidationResult.InvalidWithValidationErrorMessage(validationErrors))
            false
        }
    }
}

sealed class PaymentCardUiState {
    object Loading : PaymentCardUiState()
    object None: PaymentCardUiState()
    class ApiError(val apiError: ApiErrorResponse) : PaymentCardUiState()
    class Failed(val throwable: Throwable) : PaymentCardUiState()
    class IinFailed(val throwable: Throwable) : PaymentCardUiState()
    class Success(
        val paymentFields: List<PaymentProductField>,
        val logoUrl: String?,
        val accountOnFile: AccountOnFile?
    ) : PaymentCardUiState()
}
