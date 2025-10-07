package com.amirali.myplugin.mcpinspectorlite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposePanel
import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState
import com.amirali.myplugin.mcpinspectorlite.ui.components.McpConnectionBar
import com.amirali.myplugin.mcpinspectorlite.ui.components.McpToolCard
import com.amirali.myplugin.mcpinspectorlite.ui.viewmodel.McpToolWindowViewModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the MCP Tool Window
 */
class McpToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val composePanel = ComposePanel().apply {
            setContent {
                MaterialTheme(
                    colorScheme = lightColorScheme()
                ) {
                    McpToolWindowUI()
                }
            }
        }

        val content = ContentFactory.getInstance()
            .createContent(composePanel, project.name, false)
        toolWindow.contentManager.addContent(content)
    }
}

/**
 * Main UI composable for the MCP Tool Window
 * Uses application-level ViewModel (shared across all project windows)
 */
@Composable
fun McpToolWindowUI() {
    val viewModel = remember { McpToolWindowViewModel.getInstance() }

    val connectionState by viewModel.connectionState.collectAsState()
    val tools by viewModel.tools.collectAsState()
    val results by viewModel.invocationResults.collectAsState()
    val diagnosticMessage by viewModel.diagnosticMessage.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Connection bar
        McpConnectionBar(
            connectionState = connectionState,
            onConnect = { viewModel.connect() },
            onDisconnect = { viewModel.disconnect() }
        )

        Spacer(Modifier.height(16.dp))

        // Show diagnostic message if connection failed
        if (connectionState is McpConnectionState.Error && diagnosticMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        diagnosticMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.runDiagnostics() }
                    ) {
                        Text("Run Diagnostics Again")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Tools list
        when {
            tools.isEmpty() -> {
                Text(
                    "Connect to see available tools",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tools.size) { index ->
                        val tool = tools[index]
                        McpToolCard(
                            tool = tool,
                            result = results[tool.name],
                            onInvoke = { params ->
                                viewModel.invokeTool(tool.name, params)
                            },
                            onClearResult = { viewModel.clearResult(tool.name) }
                        )
                    }
                }
            }
        }
    }
}