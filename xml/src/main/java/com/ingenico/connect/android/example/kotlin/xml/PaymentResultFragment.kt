/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ingenico.connect.android.example.kotlin.xml.databinding.FragmentPaymentResultBinding
import com.ingenico.connect.android.example.kotlin.xml.utils.extentions.copyDataToClipboard
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel

class PaymentResultFragment : Fragment() {

    private var _binding: FragmentPaymentResultBinding? = null
    private val binding get() = _binding!!

    private val args: PaymentResultFragmentArgs by navArgs()
    private val paymentSharedViewModel: PaymentSharedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPaymentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    override fun onResume() {
        super.onResume()
        paymentSharedViewModel.activePaymentScreen.value = PaymentScreen.RESULT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initLayout() {
        binding.tvPaymentResultEncryptedFieldsData.text = args.encryptedFieldsData

        binding.tvPaymentResultShowEncryptedFieldsData.setOnClickListener {
            binding.clPaymentResultEncryptedFieldsDataContainer.visibility = View.VISIBLE
        }

        binding.btnPaymentResultCopyEncryptedFieldsDataToClipboard.setOnClickListener {
            context?.copyDataToClipboard(CLIPBOARD_PAYMENT_ENCRYPTED_FIELDS_DATA_LABEL, args.encryptedFieldsData)
        }

        binding.btnPaymentResultClose.setOnClickListener {
            findNavController().navigate(PaymentResultFragmentDirections.navigateToPaymentConfigurationFragment())
        }
    }

    private companion object {

        const val CLIPBOARD_PAYMENT_ENCRYPTED_FIELDS_DATA_LABEL = "Encrypted fields data"
    }
}
