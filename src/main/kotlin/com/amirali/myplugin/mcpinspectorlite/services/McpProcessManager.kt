package com.amirali.myplugin.mcpinspectorlite.services

import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.io.OutputStream

/**
 * Manages the MCP server subprocess lifecycle
 * Not a service - used as a component by McpConnectionManager
 */
class McpProcessManager {
    private val LOG = Logger.getInstance(McpProcessManager::class.java)
    private var process: Process? = null

    /**
     * Start the MCP server process
     */
    fun startProcess(serverScriptPath: String): ProcessStreams {
        if (process != null) {
            throw IllegalStateException("Process already running")
        }

        val command = listOf(
            if (System.getProperty("os.name").lowercase().contains("win")) "python" else "python3",
            serverScriptPath
        )

        LOG.info("Starting MCP server: ${command.joinToString(" ")}")

        process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        return ProcessStreams(
            input = process!!.inputStream,
            output = process!!.outputStream
        )
    }

    /**
     * Stop and cleanup the process
     */
    fun stopProcess() {
        process?.let {
            try {
                it.destroy()
                LOG.info("MCP server process destroyed")
            } catch (e: Exception) {
                LOG.warn("Error destroying process", e)
            }
        }
        process = null
    }

    /**
     * Check if process is running
     */
    fun isRunning(): Boolean = process?.isAlive == true

    data class ProcessStreams(
        val input: InputStream,
        val output: OutputStream
    )
}