package com.amirali.myplugin.mcpinspectorlite

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JLabel
import javax.swing.JPanel


class TestToolWindow : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()
        panel.add(JLabel("🎉 Hello MCP Inspector Lite! 🎉"))

        val content = ContentFactory.getInstance().createContent(panel, "MCP ToolWindow", false)
        toolWindow.contentManager.addContent(content)
    }
}
