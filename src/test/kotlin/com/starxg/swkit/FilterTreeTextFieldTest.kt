package com.starxg.swkit

import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import java.awt.BorderLayout
import javax.swing.*
import kotlin.test.Test

class FilterTreeTextFieldTest {
    @Test
    fun test() {
        FlatLaf.setup(FlatIntelliJLaf())
        SwingUtilities.invokeLater {
            val frame = JFrame()
            frame.title = "Test"
            val panel = JPanel(BorderLayout())
            val filterTreeTextField = FilterTreeTextField()
            val tree = JTree(filterTreeTextField.getFilterableTreeModel())
            filterTreeTextField.tree = tree
            panel.add(filterTreeTextField, BorderLayout.NORTH)
            panel.add(tree, BorderLayout.CENTER)

            frame.add(panel)
            frame.setSize(500, 500)
            frame.setLocationRelativeTo(null)
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true


        }
        Thread.currentThread().join()
    }
}