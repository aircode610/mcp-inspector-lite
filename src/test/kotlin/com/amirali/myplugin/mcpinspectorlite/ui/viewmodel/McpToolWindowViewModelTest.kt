package com.amirali.myplugin.mcpinspectorlite.ui.viewmodel

import com.amirali.myplugin.mcpinspectorlite.models.*
import com.amirali.myplugin.mcpinspectorlite.services.McpConnectionManager
import com.amirali.myplugin.mcpinspectorlite.services.McpToolExecutor
import io.mockk.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class McpToolWindowViewModelTest {

    private lateinit var viewModel: McpToolWindowViewModel
    private val mockConnectionManager = mockk<McpConnectionManager>(relaxed = true)
    private val mockToolExecutor = mockk<McpToolExecutor>(relaxed = true)
    private val connectionStateFlow = MutableStateFlow<McpConnectionState>(McpConnectionState.Disconnected)

    @BeforeEach
    fun setup() {
        mockkObject(McpConnectionManager)
        mockkObject(McpToolExecutor)

        every { McpConnectionManager.getInstance() } returns mockConnectionManager
        every { McpToolExecutor.getInstance() } returns mockToolExecutor
        every { mockConnectionManager.connectionState } returns connectionStateFlow
        every { mockConnectionManager.getUiTools() } returns emptyList()

        viewModel = McpToolWindowViewModel()
    }

    @Test
    fun `initial state is empty`() {
        assertTrue(viewModel.tools.value.isEmpty())
        assertTrue(viewModel.invocationResults.value.isEmpty())
    }

    @Test
    fun `connect triggers connection manager`() = runBlocking {
        coEvery { mockConnectionManager.connect() } just Runs

        viewModel.connect()

        coVerify { mockConnectionManager.connect() }
    }

    @Test
    fun `disconnect clears state`() = runBlocking {
        coEvery { mockConnectionManager.disconnect() } just Runs

        viewModel.disconnect()
        delay(100)

        assertTrue(viewModel.tools.value.isEmpty())
        assertTrue(viewModel.invocationResults.value.isEmpty())
    }

    @Test
    fun `invokeTool stores success result`() = runBlocking {
        val testTool = UiTool(
            name = "tool1",
            description = "Test tool",
            parameters = listOf(UiToolParameter("p", "string", null, false))
        )
        viewModel.tools.value.toMutableList().add(testTool)
        every { mockConnectionManager.getUiTools() } returns listOf(testTool)
        viewModel.connect()
        delay(100)

        coEvery {
            mockToolExecutor.invokeTool("tool1", mapOf("p" to "value"), testTool)
        } returns ToolInvocationResult.Success("Output")

        viewModel.invokeTool("tool1", mapOf("p" to "value"))
        delay(100)

        assertEquals("Output", viewModel.invocationResults.value["tool1"])
    }

    @Test
    fun `invokeTool stores error result`() = runBlocking {
        val testTool = UiTool(
            name = "tool1",
            description = "Test tool",
            parameters = emptyList()
        )
        every { mockConnectionManager.getUiTools() } returns listOf(testTool)
        viewModel.connect()
        delay(100)

        coEvery {
            mockToolExecutor.invokeTool("tool1", emptyMap(), testTool)
        } returns ToolInvocationResult.Error("Failed")

        viewModel.invokeTool("tool1", emptyMap())
        delay(100)

        assertEquals("Error: Failed", viewModel.invocationResults.value["tool1"])
    }
}