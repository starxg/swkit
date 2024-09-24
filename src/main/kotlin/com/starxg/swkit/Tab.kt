package com.starxg.swkit

import javax.swing.Icon
import javax.swing.JComponent

/**
 * 一个 Tab
 */
interface Tab {
    /**
     * 此方法会多次调用
     */
    fun getJComponent(): JComponent

    /**
     * 获取 Icon
     */
    fun getIcon(): Icon?

    /**
     * 获取标题
     */
    fun getTitle(): String
}