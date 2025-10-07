package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.models.ToolInvocationResult
import com.amirali.myplugin.mcpinspectorlite.models.UiTool
import io.mockk.*
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

        val testTool = UiTool(
            name = "test_tool",
            description = "Test tool",
            parameters = emptyList()
        )

        val result = executor.invokeTool("test_tool", emptyMap(), testTool)

        assertTrue(result is ToolInvocationResult.Error)
        assertEquals("Not connected to MCP server", (result as ToolInvocationResult.Error).message)
    }
}