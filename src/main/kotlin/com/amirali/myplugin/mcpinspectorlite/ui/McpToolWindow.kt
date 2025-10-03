package com.amirali.myplugin.mcpinspectorlite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposePanel
import com.amirali.myplugin.mcpinspectorlite.services.McpConnectionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class McpToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val composePanel = ComposePanel().apply {
            setContent {
                MaterialTheme(
                    colorScheme = darkColorScheme() // desktop-friendly scheme
                ) {
                    McpToolWindowUI(project)
                }
            }
        }

        val content = ContentFactory.getInstance().createContent(composePanel, project.name, false)
        toolWindow.contentManager.addContent(content)
    }
}

@Composable
fun McpToolWindowUI(project: Project) {
    val mcpService = remember { McpConnectionService.getInstance(project) }
    val scope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var tools by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top bar with MCP name + connect/disconnect
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("MCP Server: Demo", style = MaterialTheme.typography.titleMedium)

            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        if (!isConnected) {
                            mcpService.connect()
                            tools = mcpService.listTools()
                            isConnected = true
                        } else {
                            mcpService.disconnect()
                            tools = emptyList()
                            isConnected = false
                        }
                    } catch (e: Exception) {
                        tools = emptyList()
                        isConnected = false
                    }
                }
            }) {
                Text(if (isConnected) "Disconnect" else "Connect")
            }
        }

        Spacer(Modifier.height(24.dp))

        // Tool list
        if (!isConnected) {
            Text("Not connected", style = MaterialTheme.typography.bodyMedium)
        } else if (tools.isEmpty()) {
            Text("No tools available", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(tools) { tool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔧 $tool", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
