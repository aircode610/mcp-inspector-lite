package com.amirali.myplugin.mcpinspectorlite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    var tools by remember { mutableStateOf(listOf<com.amirali.myplugin.mcpinspectorlite.services.McpConnectionService.UiTool>()) }
    var invocationResults by remember { mutableStateOf(mapOf<String, String>()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top bar: Connect / Disconnect
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
                            tools = mcpService.getUiTools()
                            isConnected = true
                        } else {
                            mcpService.disconnect()
                            tools = emptyList()
                            invocationResults = emptyMap()
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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🔧 ${tool.name}", style = MaterialTheme.typography.bodyLarge)
                            tool.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

                            // State for parameters input
                            val paramStates = remember(tool.name) {
                                tool.parameters.associate { param ->
                                    param.name to mutableStateOf("")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Input fields for each parameter
                            tool.parameters.forEach { param ->
                                val state = paramStates[param.name]!!
                                OutlinedTextField(
                                    value = state.value,
                                    onValueChange = { state.value = it },
                                    label = { Text("${param.name} (${param.type ?: "string"})") },
                                    placeholder = { Text(param.description ?: "") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = when (param.type?.lowercase()) {
                                            "number", "int", "float", "double" -> KeyboardType.Number
                                            else -> KeyboardType.Text
                                        }
                                    )
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Invoke button
                            Button(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val args = paramStates.mapValues { it.value.value as Any }
                                    val result = mcpService.invokeTool(tool.name, args)
                                    invocationResults = invocationResults + (tool.name to result)
                                }
                            }) {
                                Text("Invoke")
                            }

                            // Show result if available
                            invocationResults[tool.name]?.let { result ->
                                Spacer(Modifier.height(4.dp))
                                Text("Result: $result", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
