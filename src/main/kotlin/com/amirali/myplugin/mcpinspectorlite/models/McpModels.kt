package com.amirali.myplugin.mcpinspectorlite.models

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.InputStream
import java.io.OutputStream

/**
 * UI representation of an MCP tool parameter
 */
data class UiToolParameter(
    val name: String,
    val type: String?,
    val description: String?,
    val required: Boolean
)

/**
 * UI representation of an MCP tool
 */
data class UiTool(
    val name: String,
    val description: String?,
    val parameters: List<UiToolParameter>
)

/**
 * Connection state for the MCP server
 */
sealed class McpConnectionState {
    data object Disconnected : McpConnectionState()
    data object Connecting : McpConnectionState()
    data class Connected(val toolCount: Int) : McpConnectionState()
    data class Error(val message: String) : McpConnectionState()
}

/**
 * Result of a tool invocation
 */
sealed class ToolInvocationResult {
    data class Success(val output: String) : ToolInvocationResult()
    data class Error(val message: String) : ToolInvocationResult()
}

/**
 * Mapper to convert SDK Tool to UI representation
 */
object ToolMapper {
    fun toUiTool(tool: Tool): UiTool {
        val props = tool.inputSchema.properties
        val requiredList = tool.inputSchema.required ?: emptyList()

        val parameters = props.entries.map { (paramName, paramValue) ->
            val obj = paramValue.jsonObject
            UiToolParameter(
                name = paramName,
                type = obj["type"]?.jsonPrimitive?.contentOrNull,
                description = obj["description"]?.jsonPrimitive?.contentOrNull,
                required = requiredList.contains(paramName)
            )
        }

        return UiTool(
            name = tool.name,
            description = tool.description,
            parameters = parameters
        )
    }
}

data class ProcessStreams(
    val input: InputStream,
    val output: OutputStream
)

data class DiagnosticResult(
    val pythonAvailable: Boolean,
    val pythonVersion: String?,
    val mcpPackageInstalled: Boolean,
    val errorMessage: String?
)