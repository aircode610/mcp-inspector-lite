package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.models.ToolInvocationResult
import com.amirali.myplugin.mcpinspectorlite.models.UiTool
import com.amirali.myplugin.mcpinspectorlite.util.SimpleParameterParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject

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
        parameters: Map<String, String>,
        tool: UiTool
    ): ToolInvocationResult {
        val client = connectionManager.getClient()
            ?: return ToolInvocationResult.Error("Not connected to MCP server")

        return try {
            val sdkTool = connectionManager.getTools().find { it.name == toolName }

            val requiredFields = sdkTool?.inputSchema?.required ?: emptyList()
            val validationErrors = SimpleParameterParser.validateRequired(parameters, requiredFields)

            if (validationErrors.isNotEmpty()) {
                return ToolInvocationResult.Error(
                    "Validation failed:\n${validationErrors.joinToString("\n")}"
                )
            }

            val schema = sdkTool?.inputSchema?.properties?.let { props ->
                buildJsonObject {
                    put("properties", JsonObject(props))
                }
            }

            val typedParams = SimpleParameterParser.parseParameters(parameters, schema)

            logger.info("Invoking tool: $toolName with parameters: $typedParams")

            val result = client.callTool(
                name = toolName,
                arguments = typedParams
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

    companion object {
        fun getInstance(): McpToolExecutor = service()
    }
}