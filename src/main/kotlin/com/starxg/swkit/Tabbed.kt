package com.starxg.swkit

import java.util.*

interface Tabbed {
    fun addTab(tab: Tab)
    fun removeTab(tab: Tab)
    fun getTabs(): List<Tab>

}