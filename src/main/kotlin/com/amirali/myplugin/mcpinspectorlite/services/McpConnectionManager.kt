package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.client.McpProcessManager
import com.amirali.myplugin.mcpinspectorlite.client.McpResourceExtractor
import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState
import com.amirali.myplugin.mcpinspectorlite.models.ToolMapper
import com.amirali.myplugin.mcpinspectorlite.models.UiTool
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

/**
 * Application-level service managing the MCP client connection lifecycle
 */
@Service(Service.Level.APP)
class McpConnectionManager {
    private val logger = Logger.getInstance(McpConnectionManager::class.java)

    private val processManager = McpProcessManager()
    private val resourceExtractor = McpResourceExtractor()

    private var client: Client? = null
    private val tools = mutableListOf<Tool>()

    private val _connectionState = MutableStateFlow<McpConnectionState>(McpConnectionState.Disconnected)
    val connectionState: StateFlow<McpConnectionState> = _connectionState.asStateFlow()

    companion object {
        private const val CONNECTION_TIMEOUT_MS = 10_000L

        fun getInstance(): McpConnectionManager = service()
    }

    /**
     * Connect to the MCP server with timeout
     */
    suspend fun connect() {
        if (client != null) {
            logger.warn("Already connected")
            return
        }

        _connectionState.value = McpConnectionState.Connecting

        try {
            withTimeout(CONNECTION_TIMEOUT_MS) {
                connectInternal()
            }
        } catch (e: TimeoutCancellationException) {
            logger.error("Connection timeout after ${CONNECTION_TIMEOUT_MS}ms")
            _connectionState.value = McpConnectionState.Error("Connection timeout - server not responding")
            disconnect()
        } catch (e: Exception) {
            logger.error("Failed to connect to MCP server", e)
            _connectionState.value = McpConnectionState.Error(e.message ?: "Unknown error")
            disconnect()
        }
    }

    /**
     * Internal connection logic
     */
    private suspend fun connectInternal() {
        val serverFile = resourceExtractor.extractServerScript()
        val streams = processManager.startProcess(serverFile.absolutePath)

        delay(500)

        if (!processManager.isHealthy()) {
            throw IllegalStateException("Server process failed to start")
        }

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

        logger.info("Attempting to connect to MCP server...")
        newClient.connect(transport)
        client = newClient

        logger.info("Client connected, fetching tools...")
        val toolsResult = client?.listTools() ?: throw IllegalStateException("Client is null")
        tools.clear()
        tools.addAll(toolsResult.tools)

        _connectionState.value = McpConnectionState.Connected(toolsResult.tools.size)
        logger.info("Successfully connected to MCP server with ${tools.size} tools")
    }

    /**
     * Disconnect from the MCP server
     */
    suspend fun disconnect() {
        try {
            client?.close()
            logger.info("MCP client closed")
        } catch (e: Exception) {
            logger.warn("Error closing client", e)
        } finally {
            client = null
        }

        processManager.stopProcess()
        tools.clear()
        _connectionState.value = McpConnectionState.Disconnected
    }

    /**
     * Get the raw SDK tools for schema access
     */
    fun getTools(): List<Tool> = tools.toList()

    /**
     * Get UI-friendly tool representation
     */
    fun getUiTools(): List<UiTool> = tools.map { ToolMapper.toUiTool(it) }

    /**
     * Get the MCP client for advanced operations
     */
    fun getClient(): Client? = client
}