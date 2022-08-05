/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.extensions

import androidx.compose.ui.text.input.KeyboardType

fun String.isValidInput(keyboardType: KeyboardType): Boolean {
    if (this == "") return true
    val regex = when (keyboardType) {
        KeyboardType.Text -> "^[ -~]*\$"
        KeyboardType.Number -> "[0-9]+"
        else -> "^[ -~]*\$"
    }
    return (regex.toRegex().matches(this))
}

fun String.isTabEvent(): Boolean {
    return this == "\t"
}
