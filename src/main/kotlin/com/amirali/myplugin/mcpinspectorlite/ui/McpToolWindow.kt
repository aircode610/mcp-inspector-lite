package com.amirali.myplugin.mcpinspectorlite.ui

import com.amirali.myplugin.mcpinspectorlite.services.McpConnectionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class McpToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()
        panel.layout = javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS)

        // Title label with project name
        val titleLabel = JLabel("MCP Tools for Project: ${project.name}")
        panel.add(titleLabel)

        // List to show tools
        val listModel = DefaultListModel<String>()
        val toolList = JBList(listModel)
        val scrollPane = JBScrollPane(toolList)
        panel.add(scrollPane)

        // Add content to tool window
        val content = ContentFactory.getInstance().createContent(panel, project.name, false)
        toolWindow.contentManager.addContent(content)

        // Load tools from MCP service in background
        CoroutineScope(Dispatchers.IO).launch {
            val mcpService = McpConnectionService.getInstance(project)
            val tools = mcpService.listTools()

            // Update UI on EDT
            javax.swing.SwingUtilities.invokeLater {
                tools.forEach { listModel.addElement(it) }
                if (tools.isEmpty()) {
                    listModel.addElement("No tools available")
                }
            }
        }
    }
}
