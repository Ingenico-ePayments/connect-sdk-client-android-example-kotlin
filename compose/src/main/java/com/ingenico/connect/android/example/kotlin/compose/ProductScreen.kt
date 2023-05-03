/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import com.ingenico.connect.android.example.kotlin.common.utils.Status
import com.ingenico.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.ingenico.connect.android.example.kotlin.compose.components.FailedText
import com.ingenico.connect.android.example.kotlin.compose.components.ProgressIndicator
import com.ingenico.connect.gateway.sdk.client.android.ConnectSDK
import com.ingenico.connect.gateway.sdk.client.android.sdk.formatter.StringFormatter
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.AccountOnFile
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentItems
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentProduct
import com.ingenico.connect.gateway.sdk.client.android.sdk.model.paymentproduct.BasicPaymentProductGroup

const val PAYMENT_PRODUCT_GROUP_CARDS = "cards"
const val PAYMENT_PRODUCT_ID_GOOGLE_PAY = "320"

@Composable
fun ProductScreen(
    navController: NavHostController,
    paymentSharedViewModel: PaymentSharedViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit,
    launchGooglePay: () -> Unit
) {

    val paymentProductStatus by paymentSharedViewModel.paymentProductsStatus.observeAsState(Status.None)

    ProductContent(
        paymentProductStatus = paymentProductStatus,
        assetsBaseUrl = ConnectSDK.getConnectSdkConfiguration().sessionConfiguration.assetUrl,
        onItemClicked = { selectedPaymentProduct ->
            paymentSharedViewModel.selectedPaymentProduct = selectedPaymentProduct
            navigateToScreen(navController, selectedPaymentProduct, showBottomSheet = {
                showBottomSheet(it)
            }, launchGooglePay = { launchGooglePay() })
        })
}

@Composable
fun ProductContent(
    paymentProductStatus: Status,
    assetsBaseUrl: String,
    onItemClicked: (Any) -> Unit
) {
    when (paymentProductStatus) {
        is Status.ApiError -> {
            FailedText()
        }
        is Status.Loading -> {
            ProgressIndicator()
        }
        is Status.Success -> {
            PaymentProductItems(
                basicPaymentItems = paymentProductStatus.data as BasicPaymentItems,
                assetsBaseUrl = assetsBaseUrl,
                onItemClicked = { onItemClicked(it) }
            )
        }
        is Status.None -> {
            // Init status; nothing to do here
        }
        is Status.Failed -> {
            FailedText()
        }
    }
}

@Composable
private fun PaymentProductItems(
    basicPaymentItems: BasicPaymentItems,
    assetsBaseUrl: String,
    onItemClicked: (Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (basicPaymentItems.accountsOnFile.isNotEmpty()) {
            item {
                SectionHeader(
                    text = stringResource(id = R.string.gc_app_paymentProductSelection_accountsOnFileTitle),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(basicPaymentItems.accountsOnFile) { accountOnFile ->
                PaymentProductItem(
                    imageUrl = assetsBaseUrl + accountOnFile.displayHints.logo,
                    label = StringFormatter().applyMask(
                        accountOnFile.displayHints.labelTemplate[0].mask.replace(
                            "9",
                            "*"
                        ), accountOnFile.label
                    ),
                    onItemClicked = {
                        onItemClicked(accountOnFile)
                    }
                )
            }

            item {
                SectionHeader(
                    text = stringResource(id = R.string.gc_app_paymentProductSelection_paymentProductsTitle),
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
            }
        }

        items(basicPaymentItems.basicPaymentItems) { basicPaymentItem ->
            PaymentProductItem(
                imageUrl = assetsBaseUrl + basicPaymentItem.displayHints.logoUrl,
                label = basicPaymentItem.displayHints.label,
                onItemClicked = {
                    onItemClicked(basicPaymentItem)
                })
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.h6,
        modifier = modifier
    )
}

@Composable
private fun PaymentProductItem(imageUrl: String, label: String, onItemClicked: () -> Unit) {
    Surface(
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clickable { onItemClicked() }) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
            Image(
                painter = rememberImagePainter(imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .width(50.dp)
                    .height(25.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(alignment = CenterVertically)
            )
        }
    }
}

private fun navigateToScreen(
    navController: NavHostController,
    selectedPaymentProduct: Any,
    showBottomSheet: (BottomSheetContent) -> Unit,
    launchGooglePay: () -> Unit
) {
    when (selectedPaymentProduct) {

        is AccountOnFile -> {
            navController.navigate(PaymentScreen.CARD.route)
        }

        is BasicPaymentProduct -> {
            when {
                selectedPaymentProduct.paymentProductGroup == PAYMENT_PRODUCT_GROUP_CARDS -> {
                    navController.navigate(PaymentScreen.CARD.route)
                }
                selectedPaymentProduct.id.equals(PAYMENT_PRODUCT_ID_GOOGLE_PAY) -> {
                    launchGooglePay()
                }
                else -> {
                    showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
                }
            }
        }
        is BasicPaymentProductGroup -> {
            if (selectedPaymentProduct.id == PAYMENT_PRODUCT_GROUP_CARDS) {
                navController.navigate(PaymentScreen.CARD.route)

            } else {
                showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
            }
        }
        else -> {
            showBottomSheet(BottomSheetContent(R.string.gc_general_errors_productUnavailable))
        }
    }
}
