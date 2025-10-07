package com.amirali.myplugin.mcpinspectorlite.services

import com.amirali.myplugin.mcpinspectorlite.client.McpProcessManager
import com.amirali.myplugin.mcpinspectorlite.client.McpResourceExtractor
import com.amirali.myplugin.mcpinspectorlite.models.McpConnectionState
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class McpConnectionManagerTest {

    private lateinit var connectionManager: McpConnectionManager
    private lateinit var mockProcessManager: McpProcessManager
    private lateinit var mockResourceExtractor: McpResourceExtractor

    @BeforeEach
    fun setup() {
        mockProcessManager = mockk(relaxed = true)
        mockResourceExtractor = mockk(relaxed = true)

        connectionManager = McpConnectionManager()

        val processManagerField = McpConnectionManager::class.java.getDeclaredField("processManager")
        processManagerField.isAccessible = true
        processManagerField.set(connectionManager, mockProcessManager)

        val extractorField = McpConnectionManager::class.java.getDeclaredField("resourceExtractor")
        extractorField.isAccessible = true
        extractorField.set(connectionManager, mockResourceExtractor)
    }

    @AfterEach
    fun tearDown() {
        runBlocking { connectionManager.disconnect() }
        unmockkAll()
    }

    @Test
    fun `initial state is Disconnected`() {
        assertEquals(McpConnectionState.Disconnected, connectionManager.connectionState.value)
        assertTrue(connectionManager.getUiTools().isEmpty())
        assertNull(connectionManager.getClient())
    }

    @Test
    fun `disconnect cleans up resources`() = runBlocking {
        connectionManager.disconnect()

        assertEquals(McpConnectionState.Disconnected, connectionManager.connectionState.value)
        assertTrue(connectionManager.getUiTools().isEmpty())
        assertNull(connectionManager.getClient())
        verify { mockProcessManager.stopProcess() }
    }
}