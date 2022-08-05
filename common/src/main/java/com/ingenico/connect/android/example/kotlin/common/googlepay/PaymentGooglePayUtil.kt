/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common.googlepay

import android.app.Activity
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.specificdata.PaymentProduct320SpecificData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contains helper methods for dealing with the Payments API.
 * @see (https://developers.google.com/pay/api/android/guides/tutorial)
 *
 */
class PaymentGooglePayUtil(
    private val activity: Activity,
    private val merchantId: String,
    private val merchantName: String,
    private val paymentProduct320SpecificData: PaymentProduct320SpecificData
) {

    /**
     * Creates an instance of [PaymentsClient] for use in an [Activity]
     *
     */
    val paymentsClient: PaymentsClient
        get() {
            val walletOptions = Wallet.WalletOptions.Builder()
                .setEnvironment(PAYMENTS_ENVIRONMENT)
                .build()
            return Wallet.getPaymentsClient(activity, walletOptions)
        }

    /**
     * Create a Google Pay API base request object with properties used in all requests.
     *
     * @return Google Pay API base request object.
     * @throws JSONException
     */
    private val baseRequest = JSONObject().apply {
        put("apiVersion", GOOGLE_PAY_API_VERSION)
        put("apiVersionMinor", GOOGLE_PAY_API_VERSION_MINOR)
    }

    /**
     * Information about the merchant requesting payment information
     *
     * @return Information about the merchant.
     * @throws JSONException
     */
    private val merchantInfo: JSONObject
        @Throws(JSONException::class)
        get() = JSONObject().put("merchantName", merchantName)

    /**
     * Gateway Integration: Identify your gateway and your app's gateway merchant identifier.
     *
     *
     * The Google Pay API response will return an encrypted payment method capable of being charged
     * by a supported gateway after payer authorization.
     *
     *
     *
     * @return Payment data tokenization for the CARD payment method.
     * @throws JSONException
     */
    private fun gatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", paymentProduct320SpecificData.gateway)
                put("gatewayMerchantId", merchantId)
            })
        }
    }

    /**
     * Card networks supported by your app and your gateway.
     *
     *
     *
     * @return Allowed card networks
     * @see (https://developers.google.com/pay/api/android/reference/object.CardParameters)
     */
    private val allowedCardNetworks = JSONArray(paymentProduct320SpecificData.networks)

    /**
     * Card authentication methods supported by your app and your gateway.
     *
     *
     *
     * @return Allowed card authentication methods.
     */
    private val allowedCardAuthMethods = JSONArray(SUPPORTED_METHODS)

    /**
     * Describe your app's support for the CARD payment method.
     *
     *
     * The provided properties are applicable to both an IsReadyToPayRequest and a
     * PaymentDataRequest.
     *
     * @return A CARD PaymentMethod object describing accepted cards.
     * @throws JSONException
     */
    private fun baseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {

            val parameters = JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", false)
            }

            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    /**
     * Describe the expected returned payment data for the CARD payment method
     *
     * @return A CARD PaymentMethod describing accepted cards and optional fields.
     * @throws JSONException
     * @see (https://developers.google.com/pay/api/android/reference/object.PaymentMethod)
     */
    private fun cardPaymentMethod(): JSONObject {
        val cardPaymentMethod = baseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", gatewayTokenizationSpecification())

        return cardPaymentMethod
    }

    /**
     * An object describing accepted forms of payment by your app, used to determine a viewer's
     * readiness to pay.
     *
     * @return API version and payment methods supported by the app.
     */
    fun isReadyToPayRequest(): JSONObject? {
        return try {
            val isReadyToPayRequest = JSONObject(baseRequest.toString())
            isReadyToPayRequest.put(
                "allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod())
            )

            isReadyToPayRequest

        } catch (e: JSONException) {
            null
        }
    }

    /**
     * Provide Google Pay API with a payment amount, currency, and amount status.
     *
     * @return information about the requested payment.
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun getTransactionInfo(price: Long, countryCode: String, currencyCode: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", (price / 100f).toString())
            put("totalPriceStatus", "FINAL")
            put("countryCode", countryCode)
            put("currencyCode", currencyCode)
        }
    }

    /**
     * An object describing information requested in a Google Pay payment sheet
     *
     * @return Payment data expected by your app.
     */
    fun getPaymentDataRequest(price: Long, countryCode: String, currencyCode: String): JSONObject? {
        return try {
            JSONObject(baseRequest.toString()).apply {
                put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod()))
                put("transactionInfo", getTransactionInfo(price, countryCode, currencyCode))
                put("merchantInfo", merchantInfo)
            }
        } catch (e: JSONException) {
            null
        }
    }

    private companion object {
        /**
         * The Google Pay API may return cards on file on Google.com (PAN_ONLY) and/or a device token on
         * an Android device authenticated with a 3-D Secure cryptogram (CRYPTOGRAM_3DS).
         *
         * @value #SUPPORTED_METHODS
         */
        val SUPPORTED_METHODS = listOf(
            "PAN_ONLY",
            "CRYPTOGRAM_3DS"
        )

        /**
         * Changing this to ENVIRONMENT_PRODUCTION will make the API return chargeable card information.
         * Please refer to the documentation to read about the required steps needed to enable
         * ENVIRONMENT_PRODUCTION.
         *
         * @value #PAYMENTS_ENVIRONMENT
         */
        const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

        const val GOOGLE_PAY_API_VERSION = 2
        const val GOOGLE_PAY_API_VERSION_MINOR = 0
    }
}
