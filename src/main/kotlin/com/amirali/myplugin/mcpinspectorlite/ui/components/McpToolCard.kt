package com.amirali.myplugin.mcpinspectorlite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amirali.myplugin.mcpinspectorlite.models.UiTool

/**
 * Card component displaying a single MCP tool
 */
@Composable
fun McpToolCard(
    tool: UiTool,
    result: String?,
    onInvoke: (Map<String, String>) -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State for parameter inputs
    val parameterStates = remember(tool.name) {
        tool.parameters.associate { param ->
            param.name to mutableStateOf("")
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Tool header
            Text(
                "🔧 ${tool.name}",
                style = MaterialTheme.typography.bodyLarge
            )

            tool.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (tool.parameters.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))

                // Parameter inputs
                tool.parameters.forEach { param ->
                    McpParameterInput(
                        parameter = param,
                        value = parameterStates[param.name]!!.value,
                        onValueChange = { parameterStates[param.name]!!.value = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val params = parameterStates.mapValues { it.value.value }
                        onInvoke(params)
                    }
                ) {
                    Text("Invoke")
                }

                if (result != null) {
                    OutlinedButton(onClick = onClearResult) {
                        Text("Clear")
                    }
                }
            }

            // Result display
            result?.let {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Result:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}