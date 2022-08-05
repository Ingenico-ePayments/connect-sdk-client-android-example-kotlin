/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ingenico.connect.android.example.kotlin.compose.card.CardScreen
import com.ingenico.connect.android.example.kotlin.compose.components.BottomSheetContent
import com.ingenico.connect.android.example.kotlin.compose.components.DefaultBottomSheet
import com.ingenico.connect.android.example.kotlin.compose.components.Header
import com.ingenico.connect.android.example.kotlin.compose.configuration.ConfigurationScreen
import com.ingenico.connect.android.example.kotlin.compose.configuration.ConfigurationViewModel
import com.ingenico.connect.android.example.kotlin.compose.theme.ComposeTheme
import com.google.accompanist.insets.systemBarsPadding
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ingenico.connect.android.example.kotlin.common.PaymentScreen
import com.ingenico.connect.android.example.kotlin.common.PaymentSharedViewModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun ComposeApp(paymentSharedViewModel: PaymentSharedViewModel, launchGooglePay: () -> Unit) {
    val navController = rememberAnimatedNavController()
    val coroutineScope = rememberCoroutineScope()

    val configurationViewModel: ConfigurationViewModel = viewModel()

    val encryptedFieldsData = paymentSharedViewModel.googlePayData.observeAsState("")
    if (encryptedFieldsData.value.isNotBlank()){
        navController.navigate("${PaymentScreen.RESULT.route}/${encryptedFieldsData.value}")
    }

    val focusManager = LocalFocusManager.current
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var informationBottomSheetContent by remember { mutableStateOf(BottomSheetContent(R.string.payment_configuration_merchant_name_helper_text)) }

    ComposeTheme {
        ModalBottomSheetLayout(
            sheetState = bottomSheetState,
            sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            sheetContent = {
                DefaultBottomSheet(informationBottomSheetContent)
            },
            content = {
                Scaffold(
                    topBar = {
                        Header(navController = navController)
                    },
                    snackbarHost = {
                        SnackbarHost(
                            hostState = it,
                            modifier = Modifier.systemBarsPadding(),
                            snackbar = { snackbarData -> Snackbar(snackbarData = snackbarData) })
                    },
                ) {
                    AnimatedNavigation(
                        navController = navController,
                        paymentSharedViewModel = paymentSharedViewModel,
                        configurationViewModel = configurationViewModel,
                        showBottomSheet = {
                            coroutineScope.launch {
                                informationBottomSheetContent = it
                                focusManager.clearFocus()
                                bottomSheetState.show()
                            }
                        },
                        launchGooglePay = { launchGooglePay() })
                }
            }
        )
    }
}

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
private fun AnimatedNavigation(
    navController: NavHostController,
    paymentSharedViewModel: PaymentSharedViewModel,
    configurationViewModel: ConfigurationViewModel,
    showBottomSheet: (BottomSheetContent) -> Unit,
    launchGooglePay: () -> Unit
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = PaymentScreen.CONFIGURATION.route
    ) {
        composable(
            PaymentScreen.CONFIGURATION.route,
            enterTransition = {
                when (initialState.destination.route) {
                    PaymentScreen.PRODUCT.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    PaymentScreen.PRODUCT.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            }
        ) {
            ConfigurationScreen(
                navController = navController,
                paymentSharedViewModel = paymentSharedViewModel,
                configurationViewModel = configurationViewModel,
                showBottomSheet = { showBottomSheet(it) })
        }
        composable(
            PaymentScreen.PRODUCT.route,
            enterTransition = {
                when (initialState.destination.route) {
                    PaymentScreen.CONFIGURATION.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    PaymentScreen.CARD.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    PaymentScreen.CONFIGURATION.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    PaymentScreen.CARD.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            }
        ) {
            ProductScreen(
                navController = navController,
                paymentSharedViewModel = paymentSharedViewModel,
                showBottomSheet = { showBottomSheet(it) },
                launchGooglePay = { launchGooglePay() })
        }
        composable(
            PaymentScreen.CARD.route,
            enterTransition = {
                when (initialState.destination.route) {
                    PaymentScreen.PRODUCT.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    PaymentScreen.RESULT.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    PaymentScreen.PRODUCT.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    PaymentScreen.RESULT.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            }
        ) {
            CardScreen(
                navController = navController,
                paymentSharedViewModel = paymentSharedViewModel,
                showBottomSheet = { showBottomSheet(it) })
        }
        composable(
            "${PaymentScreen.RESULT.route}/{encryptedFieldsData}",
            enterTransition = {
                when (initialState.destination.route) {
                    PaymentScreen.CARD.route ->
                        slideIntoContainer(
                            AnimatedContentScope.SlideDirection.Left,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            },
            exitTransition = {
                when (targetState.destination.route) {
                    PaymentScreen.CARD.route ->
                        slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Right,
                            animationSpec = tween(700)
                        )
                    else -> null
                }
            }
        ) { backStackEntry ->
            ResultScreen(
                navController = navController,
                encryptedFieldsData = backStackEntry.arguments?.getString("encryptedFieldsData")
            )
        }
    }
}
