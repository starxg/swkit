package com.starxg.swkit

import com.formdev.flatlaf.extras.components.FlatTextField
import java.util.*
import java.util.function.BiPredicate
import javax.swing.JTree
import javax.swing.event.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

interface FilterListener : EventListener {
    fun onSearch(text: String)
}

class FilterTreeTextField : FlatTextField {
    private val filterableTreeModel = FilterableTreeModel()
    private val model: TreeModel
    private val state = object {
        var expansionState = ""
        var selectionPaths = arrayOf<TreePath>()
        var filterText = ""
    }

    var filtering = false
    var tree: JTree? = null
    var ignoreCase = false

    private val filters = mutableListOf<BiPredicate<Any, String>>(object : BiPredicate<Any, String> {
        override fun test(child: Any, text: String): Boolean {
            if (child.toString().contains(text, ignoreCase)) {
                return true
            } else if (child is DefaultMutableTreeNode && child.userObject != null) {
                if (child.userObject.toString().contains(text, ignoreCase)) {
                    return true
                }
            }
            return false
        }


    })

    private val filterListener = object : TreeModelListener {
        override fun treeNodesChanged(e: TreeModelEvent) {
            treeStructureChanged(e)
        }

        override fun treeNodesInserted(e: TreeModelEvent) {
            treeStructureChanged(e)
        }

        override fun treeNodesRemoved(e: TreeModelEvent) {
            treeStructureChanged(e)
        }

        override fun treeStructureChanged(e: TreeModelEvent) {
            filter(state.filterText)
        }

    }

    init {
        this.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                changedUpdate(e)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                filter(text)
            }
        })
    }

    constructor() {
        this.model = getDefaultTreeModel()
        this.model.addTreeModelListener(filterListener)
    }

    constructor(model: TreeModel) {
        this.model = model
        this.model.addTreeModelListener(filterListener)
    }

    fun addFilterListener(listener: FilterListener) {
        listenerList.add(FilterListener::class.java, listener)
    }

    fun removeFilterListener(listener: FilterListener) {
        listenerList.remove(FilterListener::class.java, listener)
    }

    fun filter(text: String) {

        // 搜索事件
        listenerList.getListeners(FilterListener::class.java).forEach { it.onSearch(text) }

        state.filterText = text
        filterableTreeModel.children.clear()


        // 记录状态
        if (!filtering) {
            tree?.let {
                state.expansionState = TreeUtils.saveExpansionState(it)
                state.selectionPaths = it.selectionPaths ?: emptyArray()
            }
        }

        filtering = false

        // 当取消搜索的时候恢复状态
        if (text.isBlank()) {
            filterableTreeModel.fireTreeStructureChanged()
            tree?.let {
                TreeUtils.loadExpansionState(it, state.expansionState)
                it.selectionPaths = state.selectionPaths
            }
            return
        }

        filtering = true

        // 过滤
        val children = filterChildren(model.root, filterableTreeModel.children)
        if (children.isNotEmpty()) {
            filterableTreeModel.children.getOrPut(model.root)
            { mutableListOf() }.addAll(children)
        }

        // fire
        filterableTreeModel.fireTreeStructureChanged()


        // expand all
        tree?.let {
            TreeUtils.expandAll(it)
        }
    }

    fun filter() {
        filter(state.filterText)
    }

    fun addFilter(predicate: BiPredicate<Any, String>) {
        filters.add(predicate)
    }

    fun removeFilter(predicate: BiPredicate<Any, String>) {
        filters.remove(predicate)
    }

    fun clearFilters() {
        filters.clear()
    }

    private fun filterChildren(node: Any, treeChildren: MutableMap<Any, MutableList<Any>>): MutableList<Any> {
        val children = ArrayList<Any>()
        for (i in 0 until model.getChildCount(node)) {
            val child = model.getChild(node, i)
            if (model.isLeaf(child)) {
                for (filter in filters) {
                    if (filter.test(child, state.filterText)) {
                        children.add(child)
                        break
                    }
                }
            } else {
                val cc = filterChildren(child, treeChildren)
                if (cc.isNotEmpty()) {
                    treeChildren.getOrPut(child) { mutableListOf() }.addAll(cc)
                    children.add(child)
                }
            }
        }
        return children
    }

    fun getFilterableTreeModel(): TreeModel {
        return filterableTreeModel
    }

    private fun getDefaultTreeModel(): TreeModel {
        val root = DefaultMutableTreeNode("JTree")

        var parent = DefaultMutableTreeNode("colors")
        root.add(parent)
        parent.add(DefaultMutableTreeNode("blue"))
        parent.add(DefaultMutableTreeNode("violet"))
        parent.add(DefaultMutableTreeNode("red"))
        parent.add(DefaultMutableTreeNode("yellow"))

        parent = DefaultMutableTreeNode("sports")
        root.add(parent)
        parent.add(DefaultMutableTreeNode("basketball"))
        parent.add(DefaultMutableTreeNode("soccer"))
        parent.add(DefaultMutableTreeNode("football"))
        parent.add(DefaultMutableTreeNode("hockey"))

        parent = DefaultMutableTreeNode("food")
        root.add(parent)
        parent.add(DefaultMutableTreeNode("hot dogs"))
        parent.add(DefaultMutableTreeNode("pizza"))
        parent.add(DefaultMutableTreeNode("ravioli"))
        parent.add(DefaultMutableTreeNode("bananas"))
        return DefaultTreeModel(root)
    }


    private inner class FilterableTreeModel : TreeModel {
        val children = linkedMapOf<Any, MutableList<Any>>()
        private val listeners = EventListenerList()

        override fun getRoot(): Any {
            return model.root
        }

        override fun getChild(parent: Any, index: Int): Any? {
            return if (filtering) children[parent]?.get(index) else model.getChild(parent, index)
        }

        override fun getChildCount(parent: Any): Int {
            return if (filtering) children[parent]?.size ?: 0 else model.getChildCount(parent)
        }

        override fun isLeaf(node: Any): Boolean {
            return if (filtering) children[node]?.isEmpty() ?: true else model.isLeaf(node)
        }

        override fun valueForPathChanged(path: TreePath, newValue: Any) {
            model.valueForPathChanged(path, newValue)
        }

        override fun getIndexOfChild(parent: Any, child: Any): Int {
            return if (filtering) children[parent]?.let {
                return it.indexOf(child)
            } ?: -1 else model.getIndexOfChild(parent, child)
        }

        override fun addTreeModelListener(l: TreeModelListener) {
            listeners.add(TreeModelListener::class.java, l)
        }

        override fun removeTreeModelListener(l: TreeModelListener) {
            listeners.remove(TreeModelListener::class.java, l)
        }

        fun fireTreeStructureChanged() {
            listeners.getListeners(TreeModelListener::class.java).forEach {
                it.treeStructureChanged(
                    TreeModelEvent(
                        this, TreePath(root),
                        null, null
                    )
                )
            }
        }

    }
}