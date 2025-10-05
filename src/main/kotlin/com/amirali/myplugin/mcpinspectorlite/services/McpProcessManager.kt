package com.amirali.myplugin.mcpinspectorlite.services

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages the MCP server subprocess lifecycle
 * Not a service - used as a component by McpConnectionManager
 */
class McpProcessManager {
    private val LOG = Logger.getInstance(McpProcessManager::class.java)
    private var process: Process? = null
    private val errorScope = CoroutineScope(Dispatchers.IO)

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
            .start() // Don't redirect error stream - handle it separately

        // Monitor stderr in background
        errorScope.launch {
            monitorErrorStream(process!!.errorStream)
        }

        return ProcessStreams(
            input = process!!.inputStream,
            output = process!!.outputStream
        )
    }

    /**
     * Monitor error stream for debugging
     */
    private fun monitorErrorStream(errorStream: InputStream) {
        try {
            BufferedReader(InputStreamReader(errorStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    // Only log non-SLF4J errors to reduce noise
                    if (line?.contains("SLF4J") == false) {
                        LOG.warn("MCP Server Error: $line")
                    }
                }
            }
        } catch (e: Exception) {
            // Stream closed, process ended
            LOG.debug("Error stream monitoring ended: ${e.message}")
        }
    }

    /**
     * Stop and cleanup the process
     */
    fun stopProcess() {
        process?.let {
            try {
                it.destroy()
                // Wait a bit for graceful shutdown
                if (!it.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                }
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

    /**
     * Check if process is alive and healthy
     */
    fun isHealthy(): Boolean {
        val proc = process ?: return false
        return proc.isAlive
    }

    data class ProcessStreams(
        val input: InputStream,
        val output: OutputStream
    )
}