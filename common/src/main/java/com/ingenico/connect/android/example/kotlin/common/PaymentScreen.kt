/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common

/**
 * Enum class containing all payment screens.
 * This can be used to navigate and display a navigation icon
 */
enum class PaymentScreen(val route: String) {
    CONFIGURATION("configuration"),
    PRODUCT("product"),
    CARD("card"),
    RESULT("result")
}
