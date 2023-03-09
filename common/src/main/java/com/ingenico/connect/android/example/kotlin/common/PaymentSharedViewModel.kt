/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.ingenico.connect.android.example.kotlin.common.googlepay.GooglePayConfiguration
import com.ingenico.connect.android.example.kotlin.common.utils.Constants.APPLICATION_IDENTIFIER
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK.getClientApi
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK.initialize
import com.ingenico.connect.gateway.sdk.client.android.sdk.configuration.ConnectSDKConfiguration
import com.ingenico.connect.gateway.sdk.client.android.sdk.configuration.PaymentConfiguration
import com.ingenico.connect.gateway.sdk.client.android.sdk.configuration.SessionConfiguration
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.PaymentContext
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentItems
import com.ingenico.connect.gateway.sdk.client.android.sdk.network.ApiErrorResponse

/**
 * Shared ViewModel for sharing objects with multiple fragments.
 * As a result, the session and paymentContext object is created only once and can be used by multiple fragments.
 */
class PaymentSharedViewModel(application: Application) : AndroidViewModel(application) {

    var googlePayConfiguration: GooglePayConfiguration = GooglePayConfiguration(false, "", "")

    val globalErrorMessage = MutableLiveData("")
    val paymentProductsStatus = MutableLiveData<Status>()
    var selectedPaymentProduct: Any? = null

    // Only used in XML example
    val activePaymentScreen = MutableLiveData(PaymentScreen.CONFIGURATION)

    // Only used in Compose example
    val googlePayData = MutableLiveData<String>()

    /**
     * After filling in and validating all configuration fields,
     * the Ingenico connect SDK can be initialized and the result is saved in the shared viewModel.
     */
    fun configureConnectSDK(
        sessionConfiguration: SessionConfiguration,
        paymentContext: PaymentContext,
        groupPaymentProducts: Boolean,
    ) {
        val connectSDKConfiguration = ConnectSDKConfiguration.Builder(
            sessionConfiguration,
            getApplication<Application>().applicationContext
        )
            .enableNetworkLogs(true)
            .preLoadImages(true)
            .applicationId(APPLICATION_IDENTIFIER)
            .build()

        val paymentConfiguration = PaymentConfiguration.Builder(paymentContext)
            .groupPaymentProducts(groupPaymentProducts)
            .build()

        initialize(connectSDKConfiguration, paymentConfiguration)

        getPaymentProducts()
    }

    /**
     * Gets all Payment products for a provided payment context
     */
    private fun getPaymentProducts() {
        paymentProductsStatus.postValue(Status.Loading)
        getClientApi().getPaymentItems({ paymentItems: BasicPaymentItems ->
            paymentProductsStatus.postValue(Status.Success(paymentItems))
        },
            { apiError: ApiErrorResponse ->
                paymentProductsStatus.postValue(Status.ApiError(apiError))
            },
            { failure: Throwable ->
                paymentProductsStatus.postValue(Status.Failed(failure))
            }
        )
    }
}
