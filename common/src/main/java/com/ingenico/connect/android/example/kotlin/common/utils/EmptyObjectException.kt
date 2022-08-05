/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common.utils

/**
 * Map empty response data to exception
 */
object EmptyObjectException : Exception("the response of the request is null with the current client session configuration. The session may be expired.")
