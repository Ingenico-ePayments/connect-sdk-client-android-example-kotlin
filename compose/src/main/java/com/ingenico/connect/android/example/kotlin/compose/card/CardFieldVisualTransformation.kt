/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.compose.card

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ingenico.connect.gateway.sdk.client.android.sdk.formatter.StringFormatter

/**
 * VisualTransformation that applies the masking to a cardField when necessary.
 *  example masking are:
 *  cardNumber: 1234 1234 1234 1234 {{9999 9999 9999 9999}}
 *  expiryDate: 11/11 {{99/99}}
 */
class CardFieldVisualTransformation(private val mask: String?) : VisualTransformation {

    val maskChar: String =
        mask?.firstOrNull { !excludedMaskChar.contains(it.toString()) }.toString()

    private fun creditCardOffsetTranslator(cardValue: String): OffsetMapping {
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return (cardValue.count { it.toString() == maskChar }) + offset
            }

            override fun transformedToOriginal(offset: Int): Int {
                val position = offset - (cardValue.count { it.toString() == maskChar })
                return if (position < 0) 0 else position
            }
        }
        return offsetMapping
    }

    override fun filter(text: AnnotatedString): TransformedText {
        return mask?.let {
            val output = StringFormatter().applyMask(it, text.text)
            TransformedText(AnnotatedString(output), creditCardOffsetTranslator(output))
        } ?: TransformedText(text, OffsetMapping.Identity)
    }

    private companion object {
        val excludedMaskChar = listOf("{", "}", "9")
    }
}
