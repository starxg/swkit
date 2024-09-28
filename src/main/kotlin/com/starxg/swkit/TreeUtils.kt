package com.starxg.swkit

import javax.swing.JTree
import javax.swing.tree.TreeNode

object TreeUtils {
    /**
     * 获取子节点
     */
    fun children(
        parent: TreeNode,
        including: Boolean = true
    ): List<TreeNode> {

        val nodes = mutableListOf<TreeNode>()
        val parents = mutableListOf(parent)

        while (parents.isNotEmpty()) {
            val p = parents.removeFirst()
            p.children().toList().onEach {
                nodes.add(it)
                if (including) {
                    parents.add(it)
                }
            }
        }

        return nodes
    }

    fun saveExpansionState(tree: JTree): String {
        val rows = mutableListOf<Int>()
        for (i in 0 until tree.rowCount) {
            if (tree.isExpanded(i)) {
                rows.add(i)
            }
        }
        return rows.joinToString(",")
    }

    fun loadExpansionState(tree: JTree, state: String) {
        if (state.isEmpty()) {
            return
        }

        state.split(",")
            .mapNotNull { it.toIntOrNull() }
            .forEach {
                tree.expandRow(it)
            }
    }


    fun expandAll(tree: JTree) {
        var j = tree.rowCount
        var i = 0
        while (i < j) {
            tree.expandRow(i)
            i += 1
            j = tree.rowCount
        }
    }
}