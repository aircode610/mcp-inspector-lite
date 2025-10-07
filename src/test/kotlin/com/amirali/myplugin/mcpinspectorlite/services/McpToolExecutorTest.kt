package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.models.ToolInvocationResult
import io.mockk.*
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class McpToolExecutorTest {

    private lateinit var executor: McpToolExecutor
    private val mockConnectionManager = mockk<McpConnectionManager>()
    private val mockClient = mockk<Client>()

    @BeforeEach
    fun setup() {
        mockkObject(McpConnectionManager)
        every { McpConnectionManager.getInstance() } returns mockConnectionManager
        executor = McpToolExecutor()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `returns error when not connected`() = runBlocking {
        every { mockConnectionManager.getClient() } returns null

        val result = executor.invokeTool("test_tool", emptyMap())

        assertTrue(result is ToolInvocationResult.Error)
        assertEquals("Not connected to MCP server", (result as ToolInvocationResult.Error).message)
    }

    @Test
    fun `successfully invokes tool with parameters`() = runBlocking {
        every { mockConnectionManager.getClient() } returns mockClient

        val mockResult = mockk<CallToolResult> {
            every { content } returns listOf(mockk<TextContent> {
                every { text } returns "Tool executed successfully"
            })
        }

        coEvery { mockClient.callTool(any<String>(), any<Map<String, Any>>()) } returns mockResult

        val result = executor.invokeTool("test_tool", mapOf("param" to "value"))

        assertTrue(result is ToolInvocationResult.Success)
        assertEquals("Tool executed successfully", (result as ToolInvocationResult.Success).output)
    }

    @Test
    fun `parseParameter parses correctly`() {
        assertEquals(42, executor.parseParameter("42", "int"))
        assertEquals(3.14, executor.parseParameter("3.14", "double"))
        assertEquals(false, executor.parseParameter("false", "bool"))
    }

    @Test
    fun `parseParameter handles invalid input gracefully`() {
        val result = executor.parseParameter("not_a_number", "int")

        assertEquals("not_a_number", result)
        assertTrue(result is String)
    }

    @Test
    fun `parseParameter defaults to string for unknown types`() {
        val result = executor.parseParameter("test_value", "unknown")

        assertEquals("test_value", result)
        assertTrue(result is String)
    }
}