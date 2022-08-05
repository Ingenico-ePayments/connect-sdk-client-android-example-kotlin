/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.configuration

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingenico.connect.android.example.kotlin.compose.components.IsValid
import com.ingenico.connect.android.example.kotlin.compose.components.OutlinedTextFieldWithError
import com.ingenico.connect.android.example.kotlin.compose.components.TextFieldState
import com.ingenico.connect.android.example.kotlin.compose.extensions.convertToString

@Composable
fun ConfigurationTextField(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState = remember { ConfigurationTextFieldState() },
    onTrailingIconClicked: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextFieldWithError(
        value = textFieldState.text,
        onValueChange = {
            textFieldState.text = it
            textFieldState.displayError =
                textFieldState.liveValidating && textFieldState.isValid is IsValid.No
        },
        modifier = modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!textFieldState.liveValidating && focusState.isFocused) {
                    textFieldState.displayError = false
                }
            },
        enabled = textFieldState.enabled,
        label = {
            Text(text = textFieldState.label.convertToString())
        },
        trailingIcon = {
            IconButton(onClick = { onTrailingIconClicked() }) {
                Icon(imageVector = Icons.Outlined.Info, null)
            }
        },
        error = {
            if (textFieldState.displayError) {
                Text(
                    text = textFieldState.getError()?.convertToString() ?: "",
                    color = MaterialTheme.colors.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
        },
        isError = textFieldState.displayError,
        keyboardOptions = textFieldState.keyboardOptions,
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            },
            onDone = {
                focusManager.clearFocus()
            }
        )
    )
}
