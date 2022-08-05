/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ingenico.connect.android.example.kotlin.compose.R

@Composable
fun LabelledCheckbox(checkBoxField: CheckBoxField, onCheckedChange: ((Boolean) -> Unit)? = null, onTrailingIconClicked: (() -> Unit?)? = null) {
    if (checkBoxField.visible.value) {
        Row(Modifier
            .fillMaxWidth()) {
            Checkbox(
                checked = checkBoxField.isChecked.value,
                onCheckedChange = {
                    checkBoxField.isChecked.value = it
                    if (onCheckedChange != null) {
                        onCheckedChange(it)
                    }
                },
                enabled = checkBoxField.enabled.value
            )
                Text(
                    text = stringResource(id = checkBoxField.label), modifier = Modifier
                        .padding(start = 4.dp)
                        .align(Alignment.CenterVertically)
                )
                if (checkBoxField.bottomSheetContent.text != null) {
                    Box(Modifier.fillMaxWidth()) {
                    IconButton(onClick = { onTrailingIconClicked?.let { it() } }, modifier = Modifier.align(
                        Alignment.CenterEnd)) {
                        Icon(imageVector = Icons.Outlined.Info, null, tint = colorResource(R.color.darkGray))
                    }
                }
            }
        }
    }
}

data class CheckBoxField(
    @StringRes val label: Int,
    val bottomSheetContent: BottomSheetContent = BottomSheetContent(text = null)
) {
    val isChecked = mutableStateOf(false)
    val enabled = mutableStateOf(true)
    val visible = mutableStateOf(true)
}
