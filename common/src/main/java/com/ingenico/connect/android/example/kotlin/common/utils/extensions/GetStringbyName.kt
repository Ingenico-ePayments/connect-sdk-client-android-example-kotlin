/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common.utils.extensions

import android.content.Context

fun Context.getStringByName(stringName: String): String? {
    val identifier = this.resources.getIdentifier(stringName, "string", this.packageName)
    return if (identifier != 0) {
        this.getString(identifier)
    } else {
        null
    }
}
