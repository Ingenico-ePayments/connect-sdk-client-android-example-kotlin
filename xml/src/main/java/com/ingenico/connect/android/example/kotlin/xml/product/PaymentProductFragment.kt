/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ingenico.connect.android.example.kotlin.xml.R
import com.ingenico.connect.android.example.kotlin.xml.databinding.FragmentPaymentProductBinding
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.*

class PaymentProductFragment : Fragment() {

    private var _binding: FragmentPaymentProductBinding? = null
    private val binding get() = _binding!!

    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    private val paymentProductAdapter by lazy {
        PaymentProductAdapter(ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl).apply {
            onBasicPaymentItemClicked = ::onPaymentProductClicked
            onAccountOnFileClicked = ::onSavedPaymentProductClicked
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPaymentProductItems.adapter = paymentProductAdapter
        observePaymentSharedViewModel()
    }

    override fun onResume() {
        super.onResume()
        paymentSharedViewModel.activePaymentScreen.value = PaymentScreen.PRODUCT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observePaymentSharedViewModel() {
        paymentSharedViewModel.paymentProductsStatus.observe(viewLifecycleOwner) { paymentProductsStatus ->
            if (paymentProductsStatus is Status.Success) {
                val paymentProducts = mutableListOf<Any>()
                val basicPaymentItems = paymentProductsStatus.data as BasicPaymentItems
                if (basicPaymentItems.accountsOnFile.isNotEmpty()) {
                    paymentProducts.add(getString(R.string.gc_app_paymentProductSelection_accountsOnFileTitle))
                    paymentProducts.addAll(basicPaymentItems.accountsOnFile)
                    paymentProducts.add(getString(R.string.gc_app_paymentProductSelection_paymentProductsTitle))
                }
                paymentProducts.addAll(basicPaymentItems.basicPaymentItems)
                paymentProductAdapter.paymentProducts = paymentProducts.toList()
            }
        }
    }

    private fun onPaymentProductClicked(basicPaymentItem: BasicPaymentItem) {
        paymentSharedViewModel.selectedPaymentProduct = basicPaymentItem
        when (basicPaymentItem) {
            is BasicPaymentProduct -> {
                when {
                    basicPaymentItem.paymentProductGroup == PAYMENT_PRODUCT_GROUP_CARDS -> {
                        findNavController().navigate(PaymentProductFragmentDirections.navigateToPaymentCardFragment())
                    }
                    basicPaymentItem.id.equals(PAYMENT_PRODUCT_ID_GOOGLE_PAY) -> {
                        findNavController().navigate(PaymentProductFragmentDirections.navigateToGooglePayFragment())
                    }
                    else -> {
                        showPaymentProductNotImplementedBottomSheetDialog()
                    }
                }
            }
            is BasicPaymentProductGroup -> {
                if (basicPaymentItem.id == PAYMENT_PRODUCT_GROUP_CARDS) {
                    findNavController().navigate(PaymentProductFragmentDirections.navigateToPaymentCardFragment())
                } else {
                    showPaymentProductNotImplementedBottomSheetDialog()
                }
            }
            else -> {
                showPaymentProductNotImplementedBottomSheetDialog()
            }
        }
    }

    private fun onSavedPaymentProductClicked(accountOnFile: AccountOnFile) {
            paymentSharedViewModel.selectedPaymentProduct = accountOnFile
            findNavController().navigate(PaymentProductFragmentDirections.navigateToPaymentCardFragment())
    }

    private fun showPaymentProductNotImplementedBottomSheetDialog() {
        BottomSheetDialog(requireContext()).apply {
            setContentView(R.layout.bottomsheet_information)
            findViewById<TextView>(R.id.tvInformationText)?.apply {
                text = getString(R.string.gc_general_errors_productUnavailable)
            }
        }.show()
    }

    private companion object {
        // the correct payment product is initialized based on this identifier.
        // In this example only Cards and Google Pay implemented.
        const val PAYMENT_PRODUCT_GROUP_CARDS = "cards"
        const val PAYMENT_PRODUCT_ID_GOOGLE_PAY = "320"
    }
}
