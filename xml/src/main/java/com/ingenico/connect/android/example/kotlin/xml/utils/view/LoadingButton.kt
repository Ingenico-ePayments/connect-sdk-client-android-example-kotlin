/*
 * Copyright (c) 2022. Global Collect Services B.V
 */

package com.ingenico.connect.android.example.kotlin.xml.utils.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.ingenico.connect.android.example.kotlin.xml.R
import com.google.android.material.button.MaterialButton

/**
 * Custom view class for displaying a button with loading indicator
 */
class LoadingButton(context: Context, attributeSet: AttributeSet) : ConstraintLayout(context, attributeSet) {

    var loadingButtonMaterialButton: MaterialButton
    private var loadingButtonLoadingIndicator: ProgressBar

    private var buttonText: String? = null
    var isButtonEnabled: Boolean = true
        set(value) {
            field = value
            loadingButtonMaterialButton.isEnabled = value
        }

    init {
        inflate(context, R.layout.view_loading_button, this)
        loadingButtonMaterialButton = findViewById(R.id.loadingButtonMaterialButton)
        loadingButtonLoadingIndicator = findViewById(R.id.loadingButtonLoadingIndicator)

        // Set attributes parameters from file
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.LoadingButton, 0, 0).apply {
            try {
                buttonText = getString(R.styleable.LoadingButton_android_text)
                isButtonEnabled = getBoolean(R.styleable.LoadingButton_android_enabled, true)
                loadingButtonMaterialButton.text = buttonText
                loadingButtonMaterialButton.isEnabled = isButtonEnabled
            } catch (exception: Exception) {
                Log.e(javaClass.name, exception.toString())
            } finally {
                recycle()
            }
        }
    }

    fun showLoadingIndicator() {
        loadingButtonMaterialButton.text = null
        isButtonEnabled = false
        loadingButtonLoadingIndicator.visibility = View.VISIBLE
    }

    fun hideLoadingIndicator() {
        loadingButtonLoadingIndicator.visibility = View.GONE
        loadingButtonMaterialButton.text = buttonText
        isButtonEnabled = true
    }
}
