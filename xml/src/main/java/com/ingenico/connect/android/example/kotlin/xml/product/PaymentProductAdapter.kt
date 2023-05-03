/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ingenico.connect.android.example.kotlin.xml.databinding.ListitemPaymentProductBinding
import com.ingenico.connect.gateway.sdk.client.android.sdk.formatter.StringFormatter
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.AccountOnFile
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentItem
import com.squareup.picasso.Picasso

class PaymentProductAdapter(
    var baseAssetsUrl: String
) : RecyclerView.Adapter<PaymentProductAdapter.PaymentProductViewHolder>() {

    var paymentProducts: List<Any> = emptyList()
        set(value) {
            field = value
            notifyItemRangeChanged(0, paymentProducts.size)
        }

    var onBasicPaymentItemClicked: ((BasicPaymentItem) -> Unit)? = null
    var onAccountOnFileClicked: ((AccountOnFile) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentProductViewHolder {
        val itemBinding = ListitemPaymentProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentProductViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: PaymentProductViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        when {
            paymentProducts[position] is BasicPaymentItem -> {
                holder.bindBasicPaymentItem(paymentProducts[position] as BasicPaymentItem)
            }
            paymentProducts[position] is AccountOnFile -> {
                holder.bindAccountOnFile(paymentProducts[position] as AccountOnFile)
            }
            paymentProducts[position] is String -> {
                holder.bindHeader(paymentProducts[position] as String)
            }
        }
    }

    override fun getItemCount(): Int {
        return paymentProducts.size
    }

    inner class PaymentProductViewHolder(
        private val binding: ListitemPaymentProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindBasicPaymentItem(basicPaymentItem: BasicPaymentItem) {
            binding.root.run {
                Picasso.get()
                    .load(baseAssetsUrl.plus(basicPaymentItem.displayHints.logoUrl))
                    .into(binding.ivPaymentProductLogo)
                binding.tvPaymentProductLabel.text = basicPaymentItem.displayHints.label
                binding.paymentProductItem.setOnClickListener { onBasicPaymentItemClicked?.invoke(basicPaymentItem) }
            }
        }

        fun bindAccountOnFile(accountOnFile: AccountOnFile) {
            itemView.run {
                Picasso.get()
                    .load(baseAssetsUrl.plus(accountOnFile.displayHints.logo))
                    .into(binding.ivPaymentProductLogo)

                accountOnFile.displayHints.labelTemplate[0].mask?.let { mask ->
                    val formattedValue = StringFormatter().applyMask(mask.replace("9", "*"), accountOnFile.label)
                    binding.tvPaymentProductLabel.text = formattedValue
                }
                binding.paymentProductItem.setOnClickListener { onAccountOnFileClicked?.invoke(accountOnFile) }
            }
        }

        fun bindHeader(headerLabel: String) {
            itemView.run {
                binding.apply {
                    paymentProductItem.visibility = View.GONE
                    tvPaymentProductHeader.visibility = View.VISIBLE
                    tvPaymentProductHeader.text = headerLabel
                }
            }
        }
    }
}
