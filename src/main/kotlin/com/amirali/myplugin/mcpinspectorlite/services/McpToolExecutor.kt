package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.models.ToolInvocationResult
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.modelcontextprotocol.kotlin.sdk.TextContent

/**
 * Application-level service for tool execution and result processing
 */
@Service(Service.Level.APP)
class McpToolExecutor {
    private val logger = Logger.getInstance(McpToolExecutor::class.java)

    private val connectionManager by lazy {
        McpConnectionManager.getInstance()
    }

    /**
     * Invoke a tool with the given parameters
     */
    suspend fun invokeTool(
        toolName: String,
        parameters: Map<String, Any>
    ): ToolInvocationResult {
        val client = connectionManager.getClient()
            ?: return ToolInvocationResult.Error("Not connected to MCP server")

        return try {
            logger.info("Invoking tool: $toolName with parameters: $parameters")

            val result = client.callTool(
                name = toolName,
                arguments = parameters
            )

            val output = result!!.content.joinToString("\n") {
                (it as? TextContent)?.text ?: ""
            }

            logger.info("Tool $toolName executed successfully")
            ToolInvocationResult.Success(output)

        } catch (e: Exception) {
            logger.error("Failed to invoke tool $toolName", e)
            ToolInvocationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Parse string parameter to appropriate type
     */
    fun parseParameter(value: String, type: String?): Any {
        return when (type?.lowercase()) {
            "int", "integer" -> value.toIntOrNull() ?: value
            "float", "double", "number" -> value.toDoubleOrNull() ?: value
            "boolean", "bool" -> value.toBoolean()
            else -> value
        }
    }

    companion object {
        fun getInstance(): McpToolExecutor = service()
    }
}