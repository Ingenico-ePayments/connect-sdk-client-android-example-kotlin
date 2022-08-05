/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.utils.extentions

import android.content.Context
import android.os.Build
import java.util.*

fun Context.getCurrentLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        this.resources.configuration.locales[0]
    } else {
        this.resources.configuration.locale
    }
}
