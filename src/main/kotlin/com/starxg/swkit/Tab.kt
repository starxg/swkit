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

    /**
     * 当拖拽在外部释放时
     */
    fun onOutSideReleased() {}

    /**
     * 当 Tab 被删除时
     *
     * @param manual 如果是 true 那么用户手动关闭，否则可能是由于拖拽到外部 [onOutSideReleased] 被删除
     */
    fun onTabRemoved(manual: Boolean) {}

    /**
     * 当 Tab 被选中时，这个并不表示唯一，因为当 Tabbed 被分割成多个的时候就会有很多 tab 被选中
     */
    fun onTabSelected() {}
}