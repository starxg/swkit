package com.starxg.swkit

import java.util.*
import javax.swing.JComponent

interface TabbedListener : EventListener {

    /**
     * 当拖拽在外部释放时
     */
    fun onOutSideReleased(tab: Tab) {}

    /**
     * 当 Tab 被删除时
     *
     * @param manual 如果是 true 那么用户手动关闭，否则可能是由于拖拽到外部 [onOutSideReleased] 被删除
     */
    fun onTabRemoved(tab: Tab, manual: Boolean) {}

    /**
     * 当 Tab 被选中时，这个并不表示唯一，因为当 Tabbed 被分割成多个的时候就会有很多 tab 被选中
     */
    fun onTabSelected(tab: Tab) {}
}


interface Tabbed {
    fun addTab(tab: Tab)
    fun removeTab(tab: Tab)
    fun getJComponent(): JComponent
    fun getTabs(): List<Tab>

    fun addListener(listener: TabbedListener)
    fun removeListener(listener: TabbedListener)
}