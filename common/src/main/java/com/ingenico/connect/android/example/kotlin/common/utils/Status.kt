/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.common.utils

import com.ingenico.connect.gateway.sdk.client.android.sdk.network.ApiErrorResponse

/**
 * This class is  an example how to store the result of async call
 */
sealed class Status {
    data class Success(val data: Any?) : Status()
    data class Failed(val throwable: Throwable) : Status()
    data class ApiError(val apiError: ApiErrorResponse) : Status()
    object Loading : Status()
    object None: Status()
}
