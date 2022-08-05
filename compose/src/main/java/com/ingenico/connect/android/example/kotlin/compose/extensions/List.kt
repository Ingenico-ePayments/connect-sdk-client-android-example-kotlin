/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.extensions

fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}
