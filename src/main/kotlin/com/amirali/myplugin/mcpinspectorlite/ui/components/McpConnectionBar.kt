package com.amirali.myplugin.mcpinspectorlite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState

/**
 * Connection control bar component
 */
@Composable
fun McpConnectionBar(
    connectionState: McpConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                "MCP Server: Demo",
                style = MaterialTheme.typography.titleMedium
            )

            // Connection status
            Text(
                text = when (connectionState) {
                    is McpConnectionState.Disconnected -> "Not connected"
                    is McpConnectionState.Connecting -> "Connecting..."
                    is McpConnectionState.Connected -> "Connected (${connectionState.toolCount} tools)"
                    is McpConnectionState.Error -> "Error: ${connectionState.message}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (connectionState) {
                    is McpConnectionState.Connected -> MaterialTheme.colorScheme.primary
                    is McpConnectionState.Error -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }

        Button(
            onClick = {
                when (connectionState) {
                    is McpConnectionState.Connected -> onDisconnect()
                    else -> onConnect()
                }
            },
            enabled = connectionState !is McpConnectionState.Connecting
        ) {
            Text(
                when (connectionState) {
                    is McpConnectionState.Connected -> "Disconnect"
                    is McpConnectionState.Connecting -> "Connecting..."
                    else -> "Connect"
                }
            )
        }
    }
}