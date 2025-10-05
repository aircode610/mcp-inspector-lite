package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState
import com.amirali.myplugin.mcpinspectorlite.models.ToolMapper
import com.amirali.myplugin.mcpinspectorlite.models.UiTool
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * Application-level service managing the MCP client connection lifecycle
 * Singleton across all projects
 */
@Service(Service.Level.APP)
class McpConnectionManager {
    private val LOG = Logger.getInstance(McpConnectionManager::class.java)

    private val processManager = McpProcessManager()
    private val resourceExtractor = McpResourceExtractor()

    private var client: Client? = null
    private val _tools = mutableListOf<Tool>()

    private val _connectionState = MutableStateFlow<McpConnectionState>(McpConnectionState.Disconnected)
    val connectionState: StateFlow<McpConnectionState> = _connectionState.asStateFlow()

    /**
     * Connect to the MCP server
     */
    suspend fun connect() {
        if (client != null) {
            LOG.warn("Already connected")
            return
        }

        _connectionState.value = McpConnectionState.Connecting

        try {
            val serverFile = resourceExtractor.extractServerScript()
            val streams = processManager.startProcess(serverFile.absolutePath)

            val transport = StdioClientTransport(
                input = streams.input.asSource().buffered(),
                output = streams.output.asSink().buffered()
            )

            val newClient = Client(
                clientInfo = Implementation(
                    name = "myplugin-mcp-client",
                    version = "1.0.0"
                )
            )
            newClient.connect(transport)
            client = newClient

            // Fetch available tools
            val toolsResult = client!!.listTools()
            _tools.clear()
            _tools.addAll(toolsResult.tools)

            _connectionState.value = McpConnectionState.Connected(toolsResult.tools.size)
            LOG.info("Connected to MCP server with ${_tools.size} tools")

        } catch (e: Exception) {
            LOG.error("Failed to connect to MCP server", e)
            _connectionState.value = McpConnectionState.Error(e.message ?: "Unknown error")
            disconnect()
        }
    }

    /**
     * Disconnect from the MCP server
     */
    suspend fun disconnect() {
        try {
            client?.close()
            LOG.info("MCP client closed")
        } catch (e: Exception) {
            LOG.warn("Error closing client", e)
        } finally {
            client = null
        }

        processManager.stopProcess()
        _tools.clear()
        _connectionState.value = McpConnectionState.Disconnected
    }

    /**
     * Get all available tools
     */
    fun getTools(): List<Tool> = _tools.toList()

    /**
     * Get UI-friendly tool representation
     */
    fun getUiTools(): List<UiTool> = _tools.map { ToolMapper.toUiTool(it) }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean = client != null && processManager.isRunning()

    /**
     * Get the MCP client for advanced operations
     */
    fun getClient(): Client? = client

    companion object {
        fun getInstance(): McpConnectionManager = service()
    }
}