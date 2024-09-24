package com.starxg.swkit

import com.formdev.flatlaf.FlatIntelliJLaf
import com.formdev.flatlaf.FlatLaf
import java.awt.BorderLayout
import javax.swing.*
import kotlin.random.Random
import kotlin.test.Test


class TabbedPaneTest {

    @Test
    fun test() {
        FlatLaf.setup(FlatIntelliJLaf())


        SwingUtilities.invokeLater {
            var pane = TabbedPane()

            val frame = JFrame()
            println(frame.graphicsConfiguration.defaultTransform.scaleX)
            frame.title = "Test"


            val panel = JPanel(BorderLayout())
            val box = Box.createHorizontalBox()
            box.add(JButton("Add").apply {
                addActionListener {
                    pane.addTab(object : Tab {
                        private val title = "Tab ${Random.nextInt().toString().substring(0, 2)}"
                        private val panel = JLabel(title)
                        override fun getJComponent(): JComponent {
                            return this.panel
                        }

                        override fun getIcon(): Icon? {
                            return null
                        }

                        override fun getTitle(): String {
                            return title
                        }
                    })
                }
            })
            panel.add(box, BorderLayout.NORTH)
            panel.add(pane.getJComponent(), BorderLayout.CENTER)

            frame.add(panel)
            frame.setSize(500, 500)
            frame.setLocationRelativeTo(null)
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true


        }

        SwingUtilities.invokeLater {
            var pane = TabbedPane()

            val frame = JFrame()
            println(frame.graphicsConfiguration.defaultTransform.scaleX)
            frame.title = "Test"


            val panel = JPanel(BorderLayout())
            val box = Box.createHorizontalBox()
            box.add(JButton("Add").apply {
                addActionListener {
                    pane.addTab(object : Tab {
                        private val title = "Tab ${Random.nextInt().toString().substring(0, 2)}"
                        private val panel = JLabel(title)
                        override fun getJComponent(): JComponent {
                            return this.panel
                        }

                        override fun getIcon(): Icon? {
                            return null
                        }

                        override fun getTitle(): String {
                            return title
                        }
                    })
                }
            })
            panel.add(box, BorderLayout.NORTH)
            panel.add(pane.getJComponent(), BorderLayout.CENTER)

            frame.add(panel)
            frame.setSize(500, 500)
            frame.setLocationRelativeTo(null)
            frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            frame.isVisible = true


        }
        Thread.currentThread().join()
    }
}