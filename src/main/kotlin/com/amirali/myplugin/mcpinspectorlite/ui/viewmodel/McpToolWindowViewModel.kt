package com.amirali.myplugin.mcpinspectorlite.ui.viewmodel

import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState
import com.amirali.myplugin.mcpinspectorlite.models.ToolInvocationResult
import com.amirali.myplugin.mcpinspectorlite.models.UiTool
import com.amirali.myplugin.mcpinspectorlite.services.McpConnectionManager
import com.amirali.myplugin.mcpinspectorlite.services.McpToolExecutor
import com.amirali.myplugin.mcpinspectorlite.services.McpDiagnostics
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Application-level ViewModel for the MCP Tool Window UI
 */
@Service(Service.Level.APP)
class McpToolWindowViewModel {

    private val connectionManager = McpConnectionManager.getInstance()
    private val toolExecutor = McpToolExecutor.getInstance()

    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    private val _tools = MutableStateFlow<List<UiTool>>(emptyList())
    val tools: StateFlow<List<UiTool>> = _tools.asStateFlow()

    private val _invocationResults = MutableStateFlow<Map<String, String>>(emptyMap())
    val invocationResults: StateFlow<Map<String, String>> = _invocationResults.asStateFlow()

    private val _diagnosticMessage = MutableStateFlow<String?>(null)
    val diagnosticMessage: StateFlow<String?> = _diagnosticMessage.asStateFlow()

    val connectionState: StateFlow<McpConnectionState> = connectionManager.connectionState

    companion object {
        fun getInstance(): McpToolWindowViewModel = service()
    }

    /**
     * Connect to the MCP server
     */
    fun connect() {
        viewModelScope.launch {
            _diagnosticMessage.value = null
            connectionManager.connect()
            _tools.value = connectionManager.getUiTools()

            val state = connectionManager.connectionState.value
            if (state is McpConnectionState.Error) {
                runDiagnostics()
            }
        }
    }

    /**
     * Disconnect from the MCP server
     */
    fun disconnect() {
        viewModelScope.launch {
            connectionManager.disconnect()
            _tools.value = emptyList()
            _invocationResults.value = emptyMap()
            _diagnosticMessage.value = null
        }
    }

    /**
     * Run diagnostics to help troubleshoot connection issues
     */
    fun runDiagnostics() {
        viewModelScope.launch {
            val result = McpDiagnostics.runDiagnostics()
            _diagnosticMessage.value = McpDiagnostics.getDiagnosticMessage(result)
        }
    }

    /**
     * Invoke a tool with parameters
     */
    fun invokeTool(toolName: String, parameters: Map<String, String>) {
        viewModelScope.launch {
            val tool = _tools.value.find { it.name == toolName }
            if (tool == null) {
                _invocationResults.value += (toolName to "Error: Tool not found")
                return@launch
            }

            when (val result = toolExecutor.invokeTool(toolName, parameters, tool)) {
                is ToolInvocationResult.Success -> {
                    _invocationResults.value += (toolName to result.output)
                }
                is ToolInvocationResult.Error -> {
                    _invocationResults.value += (toolName to "Error: ${result.message}")
                }
            }
        }
    }

    /**
     * Clear result for a specific tool
     */
    fun clearResult(toolName: String) {
        _invocationResults.value -= toolName
    }
}