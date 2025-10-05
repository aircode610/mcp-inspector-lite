package com.amirali.myplugin.mcpinspectorlite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Service(Service.Level.PROJECT)
class McpConnectionService(private val project: Project) {

    private val LOG = Logger.getInstance(McpConnectionService::class.java)

    private var client: Client? = null
    private var process: Process? = null
    private val tools = mutableListOf<Tool>()

    /**
     * Connect to the MCP server (spawns subprocess + establishes transport)
     */
    fun connect() = runBlocking {
        if (client != null) {
            LOG.warn("Already connected to MCP server")
            return@runBlocking
        }

        try {
            val serverFile = extractServerFromResources()
            LOG.info("Launching MCP server from temp file: ${serverFile.absolutePath}")

            val command = listOf(
                if (System.getProperty("os.name").lowercase().contains("win")) "python" else "python3",
                serverFile.absolutePath
            )

            process = ProcessBuilder(command).redirectErrorStream(true).start()

            val transport = StdioClientTransport(
                input = process!!.inputStream.asSource().buffered(),
                output = process!!.outputStream.asSink().buffered()
            )

            val newClient = Client(
                clientInfo = Implementation(name = "myplugin-mcp-client", version = "1.0.0")
            )
            newClient.connect(transport)

            client = newClient

            // Fetch tools
            val toolsResult = client!!.listTools()
            tools.clear()
            tools.addAll(toolsResult.tools)
            LOG.info("Connected to MCP server with tools: ${tools.joinToString(", ")}")

        } catch (e: Exception) {
            LOG.error("Failed to connect to MCP server", e)
            disconnect() // cleanup if failed
        }
    }

    /**
     * Disconnect from MCP server (close client + kill process)
     */
    fun disconnect() {
        runBlocking {
            try {
                client?.close()
                LOG.info("MCP client closed")
            } catch (e: Exception) {
                LOG.warn("Error closing MCP client", e)
            } finally {
                client = null
            }

            try {
                process?.destroy()
                LOG.info("MCP server process destroyed")
            } catch (e: Exception) {
                LOG.warn("Error killing MCP server process", e)
            } finally {
                process = null
            }

            tools.clear()
        }
    }

    /**
     * Extract script from resources to a temporary file
     */
    private fun extractServerFromResources(): File {
        val resourceStream = this::class.java.classLoader.getResourceAsStream("mcp/server.py")
            ?: throw IllegalStateException("server.py not found in resources")

        val tempFile = File.createTempFile("mcp-server", ".py")
        tempFile.deleteOnExit()

        resourceStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    data class UiToolParameter(
        val name: String,
        val type: String?,
        val description: String?,
        val required: Boolean
    )

    data class UiTool(
        val name: String,
        val description: String?,
        val parameters: List<UiToolParameter>
    )

    fun getUiTools(): List<UiTool> {
        return tools.map { tool ->
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

            UiTool(
                name = tool.name,
                description = tool.description,
                parameters = parameters
            )
        }
    }



    fun listTools(): List<Tool> = tools

    fun invokeTool(name: String, parameters: Map<String, Any>): String = runBlocking {
        val result = client?.callTool(
            name = name,
            arguments = parameters
        )

        result?.content?.joinToString("\n") {
            (it as? TextContent)?.text ?: ""
        } ?: ""
    }

    companion object {
        fun getInstance(project: Project): McpConnectionService = project.service()
    }
}
