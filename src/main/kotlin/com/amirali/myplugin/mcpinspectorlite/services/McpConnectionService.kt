package com.amirali.myplugin.mcpinspectorlite.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File

@Service(Service.Level.PROJECT)
class McpConnectionService(private val project: Project) {

    private val LOG = Logger.getInstance(McpConnectionService::class.java)

    private val client: Client
    private val tools = mutableListOf<String>()

    init {
        LOG.info("Initializing MCP Plugin Service")
        client = Client(clientInfo = Implementation(name = "myplugin-mcp-client", version = "1.0.0"))

        // Start MCP server as a subprocess (Python script)
        runBlocking {
            try {
                val serverFile = extractServerFromResources()
                LOG.info("Launching MCP server from temp file: ${serverFile.absolutePath}")

                val command = listOf(
                    if (System.getProperty("os.name").lowercase().contains("win")) "python" else "python3",
                    serverFile.absolutePath
                )

                val process = ProcessBuilder(command).start()

                val transport = StdioClientTransport(
                    input = process.inputStream.asSource().buffered(),
                    output = process.outputStream.asSink().buffered()
                )

                client.connect(transport)

                // Fetch tools from server
                val toolsResult = client.listTools()
                tools.addAll(toolsResult.tools.map { it.name })
                LOG.info("Connected to MCP server with tools: ${tools.joinToString(", ")}")

            } catch (e: Exception) {
                LOG.error("Failed to connect to MCP server", e)
            }
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

    /**
     * List available tools
     */
    fun listTools(): List<String> = tools

    /**
     * Invoke a tool on the server
     */
    fun invokeTool(name: String, parameters: Map<String, Any>): String = runBlocking {
        val result = client.callTool(
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
