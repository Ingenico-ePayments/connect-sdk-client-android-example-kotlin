/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.utils.extentions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach

fun ViewGroup.deepForEach(function: View.() -> Unit) {
    this.forEach { child ->
        child.function()
        if (child is ViewGroup) {
            child.deepForEach(function)
        }
    }
}
