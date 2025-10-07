package com.amirali.myplugin.mcpinspectorlite.services

import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class McpProcessManagerTest {

    private val processManager = McpProcessManager()

    @AfterEach
    fun tearDown() {
        processManager.stopProcess()
        unmockkAll()
    }

    @Test
    fun `isHealthy returns false when no process running`() {
        assertFalse(processManager.isHealthy())
    }

    @Test
    fun `starts process successfully`() {
        val mockProcess = mockk<Process>(relaxed = true) {
            every { isAlive } returns true
            every { inputStream } returns ByteArrayInputStream(byteArrayOf())
            every { outputStream } returns ByteArrayOutputStream()
            every { errorStream } returns ByteArrayInputStream(byteArrayOf())
        }

        mockkConstructor(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess

        val streams = processManager.startProcess("/path/to/script.py")

        assertNotNull(streams.input)
        assertNotNull(streams.output)
        assertTrue(processManager.isHealthy())
    }

    @Test
    fun `throws exception when starting process twice`() {
        val mockProcess = mockk<Process>(relaxed = true) {
            every { isAlive } returns true
            every { inputStream } returns ByteArrayInputStream(byteArrayOf())
            every { outputStream } returns ByteArrayOutputStream()
            every { errorStream } returns ByteArrayInputStream(byteArrayOf())
        }

        mockkConstructor(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess

        processManager.startProcess("/path/to/script.py")

        assertThrows(IllegalStateException::class.java) {
            processManager.startProcess("/path/to/script.py")
        }
    }

    @Test
    fun `stops process gracefully`() {
        val mockProcess = mockk<Process>(relaxed = true) {
            every { isAlive } returns true
            every { inputStream } returns ByteArrayInputStream(byteArrayOf())
            every { outputStream } returns ByteArrayOutputStream()
            every { errorStream } returns ByteArrayInputStream(byteArrayOf())
            every { waitFor(any(), any()) } returns true
        }

        mockkConstructor(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess

        processManager.startProcess("/path/to/script.py")
        processManager.stopProcess()

        verify { mockProcess.destroy() }
        assertFalse(processManager.isHealthy())
    }

    @Test
    fun `force kills process if graceful shutdown fails`() {
        val mockProcess = mockk<Process>(relaxed = true) {
            every { isAlive } returns true
            every { inputStream } returns ByteArrayInputStream(byteArrayOf())
            every { outputStream } returns ByteArrayOutputStream()
            every { errorStream } returns ByteArrayInputStream(byteArrayOf())
            every { waitFor(any(), any()) } returns false
        }

        mockkConstructor(ProcessBuilder::class)
        every { anyConstructed<ProcessBuilder>().start() } returns mockProcess

        processManager.startProcess("/path/to/script.py")
        processManager.stopProcess()

        verify { mockProcess.destroy() }
        verify { mockProcess.destroyForcibly() }
    }
}