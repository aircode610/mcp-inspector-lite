package com.amirali.myplugin.mcpinspectorlite.client

import com.intellij.openapi.diagnostic.Logger
import java.io.File

/**
 * Handles extraction of bundled resources to temporary files
 */
class McpResourceExtractor {
    private val logger = Logger.getInstance(McpResourceExtractor::class.java)

    /**
     * Extract the MCP server script from resources to a temp file
     */
    fun extractServerScript(): File {
        val resourceStream = javaClass.classLoader.getResourceAsStream("mcp/server.py")
            ?: throw IllegalStateException("server.py not found in resources")

        val tempFile = File.createTempFile("mcp-server", ".py")
        tempFile.deleteOnExit()

        resourceStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        logger.info("Extracted MCP server to: ${tempFile.absolutePath}")
        return tempFile
    }
}