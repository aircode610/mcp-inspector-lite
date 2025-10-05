package com.amirali.myplugin.mcpinspectorlite.services

import com.intellij.openapi.diagnostic.Logger
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Diagnostic utilities for troubleshooting MCP connection issues
 */
object McpDiagnostics {
    private val LOG = Logger.getInstance(McpDiagnostics::class.java)

    data class DiagnosticResult(
        val pythonAvailable: Boolean,
        val pythonVersion: String?,
        val mcpPackageInstalled: Boolean,
        val errorMessage: String?
    )

    /**
     * Run diagnostic checks for MCP server requirements
     */
    fun runDiagnostics(): DiagnosticResult {
        val pythonCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "python"
        } else {
            "python3"
        }

        // Check Python availability and version
        val (pythonAvailable, pythonVersion) = checkPython(pythonCommand)

        if (!pythonAvailable) {
            return DiagnosticResult(
                pythonAvailable = false,
                pythonVersion = null,
                mcpPackageInstalled = false,
                errorMessage = "Python is not installed or not in PATH"
            )
        }

        // Check if MCP package is installed
        val mcpInstalled = checkMcpPackage(pythonCommand)

        val errorMessage = when {
            !mcpInstalled -> "MCP package not installed. Run: pip install mcp"
            else -> null
        }

        return DiagnosticResult(
            pythonAvailable = pythonAvailable,
            pythonVersion = pythonVersion,
            mcpPackageInstalled = mcpInstalled,
            errorMessage = errorMessage
        )
    }

    private fun checkPython(pythonCommand: String): Pair<Boolean, String?> {
        return try {
            val process = ProcessBuilder(pythonCommand, "--version")
                .redirectErrorStream(true)
                .start()

            val output = BufferedReader(InputStreamReader(process.inputStream))
                .readText()
                .trim()

            val exitCode = process.waitFor(5, TimeUnit.SECONDS)

            if (exitCode && process.exitValue() == 0) {
                LOG.info("Python check: $output")
                Pair(true, output)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            LOG.warn("Failed to check Python: ${e.message}")
            Pair(false, null)
        }
    }

    private fun checkMcpPackage(pythonCommand: String): Boolean {
        return try {
            val process = ProcessBuilder(
                pythonCommand,
                "-c",
                "import mcp.server.fastmcp"
            )
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor(5, TimeUnit.SECONDS)

            if (exitCode && process.exitValue() == 0) {
                LOG.info("MCP package is installed")
                true
            } else {
                val error = BufferedReader(InputStreamReader(process.inputStream))
                    .readText()
                LOG.warn("MCP package check failed: $error")
                false
            }
        } catch (e: Exception) {
            LOG.warn("Failed to check MCP package: ${e.message}")
            false
        }
    }

    /**
     * Get formatted diagnostic message for UI
     */
    fun getDiagnosticMessage(result: DiagnosticResult): String {
        return buildString {
            appendLine("🔍 MCP Connection Diagnostics:")
            appendLine()
            append("Python: ")
            if (result.pythonAvailable) {
                appendLine("✅ ${result.pythonVersion}")
            } else {
                appendLine("❌ Not found")
            }

            append("MCP Package: ")
            if (result.mcpPackageInstalled) {
                appendLine("✅ Installed")
            } else {
                appendLine("❌ Not installed")
            }

            if (result.errorMessage != null) {
                appendLine()
                appendLine("❌ ${result.errorMessage}")
            }

            if (!result.mcpPackageInstalled) {
                appendLine()
                appendLine("To install MCP:")
                appendLine("pip install mcp")
                appendLine("or")
                appendLine("pip install fastmcp")
            }
        }
    }
}