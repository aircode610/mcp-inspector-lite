package com.amirali.myplugin.mcpinspectorlite.client

import com.amirali.myplugin.mcpinspectorlite.models.ProcessStreams
import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages the MCP server subprocess lifecycle
 */
class McpProcessManager {
    private val logger = Logger.getInstance(McpProcessManager::class.java)

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

        logger.info("Starting MCP server: ${command.joinToString(" ")}")

        process = ProcessBuilder(command)
            .start()

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
                    if (line?.contains("SLF4J") == false) {
                        logger.warn("MCP Server Error: $line")
                    }
                }
            }
        } catch (e: Exception) {
            logger.debug("Error stream monitoring ended: ${e.message}")
        }
    }

    /**
     * Stop and cleanup the process
     */
    fun stopProcess() {
        process?.let {
            try {
                it.destroy()
                if (!it.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    it.destroyForcibly()
                }
                logger.info("MCP server process destroyed")
            } catch (e: Exception) {
                logger.warn("Error destroying process", e)
            }
        }
        process = null
    }

    /**
     * Check if process is alive and healthy
     */
    fun isHealthy(): Boolean {
        val proc = process ?: return false
        return proc.isAlive
    }
}