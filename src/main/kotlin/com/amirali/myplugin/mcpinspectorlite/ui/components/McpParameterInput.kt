package com.amirali.myplugin.mcpinspectorlite.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.amirali.myplugin.mcpinspectorlite.models.UiToolParameter

/**
 * Input field component for a single tool parameter
 */
@Composable
fun McpParameterInput(
    parameter: UiToolParameter,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            val requiredMarker = if (parameter.required) "*" else ""
            Text("${parameter.name}$requiredMarker (${parameter.type ?: "string"})")
        },
        placeholder = {
            parameter.description?.let { Text(it) }
        },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = when (parameter.type?.lowercase()) {
                "number", "int", "integer", "float", "double" -> KeyboardType.Number
                else -> KeyboardType.Text
            }
        ),
        isError = parameter.required && value.isBlank()
    )
}