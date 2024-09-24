package com.starxg.swkit

import com.formdev.flatlaf.extras.components.FlatTabbedPane
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.event.EventListenerList
import kotlin.math.abs

internal enum class Position {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    MIDDLE,
    UNKNOWN
}


interface JTabbedPaneCustomizer {
    fun customize(tabbedPane: JTabbedPane)
}


class TabbedPane : Tabbed {

    /**
     * 预选颜色
     */
    var preselectBackgroundColor = Color(0, 91, 187, 40)

    /**
     * 预选透明度
     */
    var preselectBackgroundOpacity = 0.8f

    /**
     * 预选敏感度，中间越好选
     */
    var preselectBackgroundSensitivity = 3.5f

    /**
     * 是否允许跨实例
     */
    var cross = true

    /**
     * 是否允许跨窗口，如果允许跨窗口那么 [cross] 也要为 true。
     */
    var crossWindows = true

    /**
     * 定制化
     */
    var tabbedPaneCustomizer = object : JTabbedPaneCustomizer {
        override fun customize(tabbedPane: JTabbedPane) {
            tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
        }
    }


    private val listeners = EventListenerList()
    private val tabbedListeners get() = listeners.getListeners(TabbedListener::class.java)
    private val tabs = mutableListOf<Tab>()
    private val rootSplitter = Splitter()
    private val root = JPanel(BorderLayout()).apply {
        add(rootSplitter, BorderLayout.CENTER)
    }


    override fun addTab(tab: Tab) {
        val component = tab.getJComponent()
        if (tabs.any { it.getJComponent() == component }) {
            throw IllegalStateException("Component already added.")
        }
        addTab(findNearestNode(rootSplitter), tab)
    }

    internal fun addTab(splitterTabbedPane: SplitterTabbedPane, tab: Tab) {
        splitterTabbedPane.addTab(tab.getTitle(), tab.getJComponent())
        this.tabs.add(tab)
    }

    override fun removeTab(tab: Tab) {
        tabs.remove(tab)
    }

    override fun getJComponent(): JComponent {
        return root
    }

    override fun getTabs(): List<Tab> {
        return tabs
    }

    override fun addListener(listener: TabbedListener) {
        listeners.add(TabbedListener::class.java, listener)
    }

    override fun removeListener(listener: TabbedListener) {
        listeners.remove(TabbedListener::class.java, listener)
    }


    private fun findNearestNode(splitter: Splitter): SplitterTabbedPane {
        if (splitter.isLeftTabbedPane) {
            return splitter.leftTabbedPane
        } else if (splitter.isRightTabbedPane) {
            return splitter.rightTabbedPane
        }

        if (splitter.leftComponent == null && splitter.rightComponent == null) {
            rootSplitter.setLeftComponent(SplitterTabbedPane())
            return rootSplitter.leftTabbedPane
        }

        if (splitter.isLeftSplitter) {
            return findNearestNode(splitter.leftComponent as Splitter)
        }

        if (splitter.isRightSplitter) {
            return findNearestNode(splitter.rightComponent as Splitter)
        }

        throw UnsupportedOperationException()
    }

    /**
     * 分割器
     */
    internal inner class Splitter : DynamicSingleSplitPane() {

        internal val isLeftSplitter get() = getLeftComponent() is Splitter
        internal val isRightSplitter get() = getRightComponent() is Splitter
        internal val isRightTabbedPane get() = getRightComponent() is JTabbedPane
        internal val isLeftTabbedPane get() = getLeftComponent() is JTabbedPane

        private val leftSplitter get() = getLeftComponent() as Splitter
        private val rightSplitter get() = getRightComponent() as Splitter
        private val isRightBox get() = getRightComponent() is Box.Filler || getRightComponent() == null

        val leftTabbedPane: SplitterTabbedPane
            get() {
                if (isLeftSplitter) {
                    return leftSplitter.leftTabbedPane
                }
                return getLeftComponent() as SplitterTabbedPane
            }
        val rightTabbedPane: SplitterTabbedPane
            get() {
                if (isRightSplitter) {
                    return rightSplitter.rightTabbedPane
                }
                return getRightComponent() as SplitterTabbedPane
            }

        init {
            this.setLeftComponent(SplitterTabbedPane())
            this.setRightComponent(null)
        }

        private fun isInParentLeft(c: JComponent): Boolean {
            return splitterParent().getLeftComponent() == c
        }

        private fun isInParentRight(c: JComponent): Boolean {
            return splitterParent().getRightComponent() == c
        }

        private fun splitterParent(): Splitter {
            if (parent is Splitter) {
                return parent as Splitter
            }
            return rootSplitter
        }

        internal fun tabbedPaneEmpty() {
            val p = splitterParent()
            val r = getRightComponent()
            if (this == rootSplitter) {
                p.setRightComponent(null)
                p.setLeftComponent(null)
                if (r is Splitter) {
                    p.setOrientation(r.orientation)
                    p.setRightComponent(r.rightComponent)
                    p.setLeftComponent(r.leftComponent)
                } else {
                    p.setLeftComponent(r)
                }
            } else {
                // 如果右边是一个 Splitter
                if (isRightSplitter) {
                    if (isInParentRight(this)) {
                        p.setRightComponent(rightSplitter)
                    } else {
                        p.setLeftComponent(rightSplitter)
                    }
                } else { // 不是 Splitter 那就是空，把 r 提升到父亲的右边。
                    p.setRightComponent(r)
                }
            }

            // 修正左边数据
            rootSplitter.correctLeft()

        }

        /**
         * 纠正左边数据。这个方法的目的是为了让左边的数据满足最短路径，始终让 SplitterTabbedPane 离 Root 更近
         */
        private fun correctLeft() {
            val p = splitterParent()
            if (isLeftSplitter) {
                // 如果左边是Splitter，右边是Box。那么这一层Splitter没有意义，直接提升一级，离Root更近。
                if (isRightBox) {
                    // 如果父就是自己，并且左边是Splitter，右边是 Null，那么Left需要平面化
                    if (p == this) {
                        val l = leftSplitter.leftComponent
                        val r = leftSplitter.rightComponent
                        p.setOrientation(leftSplitter.orientation)
                        p.setLeftComponent(l)
                        p.setRightComponent(r)
                    } else {
                        if (isInParentRight(this)) {
                            p.setRightComponent(leftSplitter)
                        } else {
                            p.setLeftComponent(leftSplitter)
                        }
                    }
                    // 持续修正
                    rootSplitter.correctLeft()
                    return
                }

            } else if (isLeftTabbedPane) {
                // 只修复父亲在左边的
                if (isInParentLeft(this) && isRightBox) {
                    p.setLeftComponent(leftComponent)
                }
            }

            if (isLeftSplitter) {
                leftSplitter.correctLeft()
            }

            if (isRightSplitter) {
                rightSplitter.correctLeft()
            }
        }

        private fun splitRight(position: Position, tab: Tab) {
            if (isRightBox) {
                if (position == Position.RIGHT || position == Position.LEFT) {
                    setOrientation(HORIZONTAL_SPLIT)
                } else {
                    setOrientation(VERTICAL_SPLIT)
                }
                val splitter = Splitter()
                setRightComponent(splitter)
                addTab(splitter.leftTabbedPane, tab)
            } else {
                if (isLeftTabbedPane) {
                    val splitter = Splitter()
                    val oldLeft = getLeftComponent()
                    splitter.setLeftComponent(oldLeft)
                    splitter.split(position, tab)
                    setLeftComponent(splitter)
                } else if (isRightSplitter) {
                    leftSplitter.splitRight(position, tab)
                }
            }

            this.setResizeWeight(0.5)
            this.setDividerSize(4)
            this.doLayout()
        }

        private fun splitLeft(position: Position, tab: Tab) {
            val oldLeft = leftTabbedPane
            // 如果右边是一个空的，那么拆分左边，右边变成一个 Splitter
            if (isRightBox) {
                if (position == Position.TOP || position == Position.BOTTOM) {
                    setOrientation(VERTICAL_SPLIT)
                } else {
                    setOrientation(HORIZONTAL_SPLIT)
                }
                // 因为是往左边添加，那么之前的Left就是新Splitter的左边
                // 而 Splitter 就变的成了右边
                val splitter = Splitter()
                splitter.setLeftComponent(oldLeft)
                val tabbedPane = SplitterTabbedPane()
                addTab(tabbedPane, tab)
                setLeftComponent(tabbedPane)
                setRightComponent(splitter)
            } else {
                if (isLeftSplitter) {
                    leftSplitter.splitLeft(position, tab)
                } else {
                    // 如果右边不是一个 Box ，那么左边需要再用一个 Splitter 包括，右边不动
                    val splitter = Splitter()
                    splitter.setLeftComponent(oldLeft)
                    // 递归交换，此时新的 Splitter 右边是一个 Box
                    splitter.splitLeft(position, tab)
                    setLeftComponent(splitter)
                }

            }

            this.setResizeWeight(0.5)
            this.setDividerSize(4)
        }

        internal fun split(position: Position, tab: Tab) {
            if (position == Position.UNKNOWN) {
                return
            }

            if (position == Position.RIGHT || position == Position.BOTTOM) {
                splitRight(position, tab)
            } else {
                splitLeft(position, tab)
            }
        }

    }

    /**
     * 分割器内的面板
     */
    internal inner class SplitterTabbedPane : FlatTabbedPane() {


        internal val splitter: Splitter
            get() {
                var p = parent
                while (p != null && p !is Splitter) {
                    p = p.parent
                }
                if (p == null) throw IllegalStateException()
                return p as Splitter
            }


        init {
            val drag = Drag(this, this@TabbedPane)
            addMouseListener(drag)
            addMouseMotionListener(drag)
            putClientProperty("JTabbedPane.tabClosable", true)
            setTabCloseCallback { _, u ->
                // 提前获取
                val tab = getTabAt(u)
                removeTabAt(u)
                // 发出被删除事件
                tabbedListeners.forEach { it.onTabRemoved(tab, false) }
            }
        }

        init {
            addChangeListener {
                listeners.getListeners(TabbedListener::class.java).forEach {
                    it.onTabSelected(getTabAt(selectedIndex))
                }
            }
        }

        init {
            tabbedPaneCustomizer.customize(this)
        }


        override fun removeTabAt(index: Int) {
            removeTabAt(index, true)
        }


        internal fun removeTabAt(index: Int, autoRemove: Boolean = true) {
            removeTab(getTabAt(index))
            super.removeTabAt(index)
            if (tabCount < 1 && autoRemove)
                splitter.tabbedPaneEmpty()
        }

        internal fun getTabAt(index: Int): Tab {
            val component = getComponentAt(index)
            for (tab in tabs) {
                if (tab.getJComponent() == component) {
                    return tab
                }
            }
            throw IndexOutOfBoundsException()
        }

    }

    /**
     * 拖拽功能
     */
    internal class Drag(private val splitterTabbedPane: SplitterTabbedPane, private val tabbedPane: TabbedPane) :
        MouseAdapter(), KeyEventDispatcher {
        private var tabIndex = -1
        private var tab: Tab? = null
        private var pressedPoint = Point()
        private var dragging = false
        private var drag: JWindow? = null
        private var preselectBackground: PreselectBackground? = null
        private val focusManager = FocusManager.getCurrentKeyboardFocusManager()

        override fun mousePressed(e: MouseEvent) {
            tabIndex = splitterTabbedPane.indexAtLocation(e.x, e.y)
            if (tabIndex == -1) return
            pressedPoint = e.point
        }

        override fun mouseReleased(e: MouseEvent) {
            dragEnd()
        }

        override fun mouseDragged(e: MouseEvent) {
            if (!dragging) {
                initDrag(e)
                if (!dragging) return
            }

            drag?.let {
                val dragPoint = Point(e.locationOnScreen)
                dragPoint.x -= it.width / 2
                dragPoint.y -= it.height / 2
                it.location = dragPoint
                it.isVisible = true
            }

            val tabbedPane = findSplitterTabbedPane(e.locationOnScreen)
            if (tabbedPane == null) {
                preselectBackground?.dispose()
                return
            }

            val background = initPreselectBackground(SwingUtilities.getWindowAncestor(tabbedPane))

            val point = Point(e.locationOnScreen)
            SwingUtilities.convertPointFromScreen(point, tabbedPane)
            background.show(point, tabbedPane)

            if (!background.isVisible()) {
                background.setVisible(true)
            }

            drag?.isVisible = true

        }

        private fun initPreselectBackground(owner: Window): PreselectBackground {
            val preselectBackground = this.preselectBackground
            if (preselectBackground != null) {
                if (preselectBackground.owner == owner) {
                    return preselectBackground
                }
            }

            this.preselectBackground?.dispose()
            this.preselectBackground = PreselectBackground(
                owner,
                color = tabbedPane.preselectBackgroundColor,
                opacity = tabbedPane.preselectBackgroundOpacity,
                sensitivity = tabbedPane.preselectBackgroundSensitivity
            )
            return this.preselectBackground!!
        }

        private fun initDrag(e: MouseEvent) {
            if (tabIndex == -1) return

            if (abs(pressedPoint.x - e.x) > 5 || abs(pressedPoint.y - e.y) > 5) {
                val owner = JWindow(SwingUtilities.getWindowAncestor(tabbedPane.root))
                val image = createTabImage(tabIndex)
                owner.add(JLabel(ImageIcon(image)))
                owner.size = Dimension(image.width, image.height)
                owner.isVisible = false
                this.drag = owner

                // 拖拽中
                dragging = true

                // 获取到 Tab
                tab = splitterTabbedPane.getTabAt(tabIndex)

                // 从原来的地方删除
                splitterTabbedPane.removeTabAt(tabIndex, false)

                // 监听 ESC
                focusManager.addKeyEventDispatcher(this)

            }
        }

        private fun findSplitterTabbedPane(screenPoint: Point): SplitterTabbedPane? {
            val current = SwingUtilities.getWindowAncestor(tabbedPane.root)
            for (window in Window.getWindows()
                // 如果不允许跨窗口那么就是只有 current
                .filter { if (tabbedPane.crossWindows) true else it == current }
                .sortedBy { if (it == current) 0 else 1 }) {

                val point = Point(screenPoint)
                SwingUtilities.convertPointFromScreen(point, window)

                val root = if (window is RootPaneContainer) (window as RootPaneContainer).rootPane else window
                // 先根据坐标查找到最深的子
                val c = SwingUtilities.getDeepestComponentAt(root, point.x, point.y)
                // 然后再一层层找出合适的Pane
                val p = findSplitterTabbedPane(c, screenPoint)

                // 如果不在当前root内，那就是跨实例了
                if (!tabbedPane.cross && !isInCurrentRoot(p)) {
                    // 跳过
                    continue
                }

                if (p != null) {
                    return p
                }
            }
            return null
        }

        private fun isInCurrentRoot(c: Component?): Boolean {
            var p = c
            while (p != null) {
                if (p == tabbedPane.root) {
                    return true
                }
                p = p.parent
            }
            return false
        }

        private fun findSplitterTabbedPane(c: Component?, screenPoint: Point): SplitterTabbedPane? {
            var p: Component? = c

            while (p != null) {
                if (p is SplitterTabbedPane) {
                    val point = Point(screenPoint)
                    SwingUtilities.convertPointFromScreen(point, p)
                    if ((point.x >= 0) && (point.x < p.width) && (point.y >= p.tabHeight) && (point.y < p.height)) {
                        return p
                    }
                }
                p = p.parent
            }

            return null
        }

        private fun dragEnd(cancel: Boolean = false) {
            if (!dragging) {
                return
            }

            // 获取到目标等信息
            var position = Position.UNKNOWN
            val tab = this.tab
            val tabIndex = this.tabIndex
            var targetTabbedPane: SplitterTabbedPane? = null
            val background = this.preselectBackground
            if (background != null) {
                position = background.position
                if (position != Position.UNKNOWN) {
                    targetTabbedPane = background.tabbedPane
                }
            }

            focusManager.removeKeyEventDispatcher(this)

            this.preselectBackground?.dispose()
            this.drag?.dispose()

            this.tab = null
            this.preselectBackground = null
            this.drag = null
            this.tabIndex = -1
            this.pressedPoint = Point()
            this.dragging = false

            if (tab == null) {
                return
            }

            // 如果是取消，那么恢复原位
            if (cancel) {
                tabbedPane.tabs.add(tab)
                splitterTabbedPane.insertTab(
                    tab.getTitle(), tab.getIcon(),
                    tab.getJComponent(), String(), tabIndex
                )
                return
            }

            if (position == Position.UNKNOWN || targetTabbedPane !is SplitterTabbedPane) {
                // 发出被删除事件
                tabbedPane.tabbedListeners.forEach { it.onTabRemoved(tab, false) }

                if (position == Position.UNKNOWN) {
                    if (splitterTabbedPane.tabCount == 0) {
                        splitterTabbedPane.splitter.tabbedPaneEmpty()
                        // 外部释放
                        tabbedPane.tabbedListeners.forEach { it.onOutSideReleased(tab) }
                    }
                }
                return
            }

            // 这种就是最后一个拖拽出来，然后右添加回去了
            if (targetTabbedPane == splitterTabbedPane && targetTabbedPane.tabCount == 0) {
                tabbedPane.addTab(splitterTabbedPane, tab)
            } else {
                // 中间就是直接添加
                if (position == Position.MIDDLE) {
                    tabbedPane.addTab(targetTabbedPane, tab)
                } else {
                    targetTabbedPane.splitter.split(position, tab)
                }
                if (splitterTabbedPane.tabCount == 0) {
                    splitterTabbedPane.splitter.tabbedPaneEmpty()
                }
            }
        }

        private fun createTabImage(index: Int): BufferedImage {
            val tabBounds = splitterTabbedPane.getBoundsAt(index)
            val image = BufferedImage(tabBounds.width, tabBounds.height, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.translate(-tabBounds.x, -tabBounds.y)
            splitterTabbedPane.paint(g2)
            g2.dispose()
            return image
        }

        override fun dispatchKeyEvent(e: KeyEvent): Boolean {
            if (e.id == KeyEvent.KEY_PRESSED) {
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    preselectBackground?.hide()
                    dragEnd(true)
                    return true
                }
            }
            return false
        }
    }
}


/**
 * 如果只有一个元素的时候，会隐藏 Diver
 */
internal open class DynamicSingleSplitPane : JSplitPane() {
    override fun setLeftComponent(comp: Component?) {
        super.setLeftComponent(comp)
        resetDividerSize()
    }

    override fun setRightComponent(comp: Component?) {
        super.setRightComponent(comp)
        resetDividerSize()
    }

    private fun resetDividerSize() {

        // reset
        setDividerSize(getDefaultDividerSize())

        if (getLeftComponent() == null || getRightComponent() == null) {
            setDividerSize(0)
        }
    }

    private fun getDefaultDividerSize(): Int {
        return try {
            UIManager.getInt("SplitPane.dividerSize")
        } catch (e: Exception) {
            4
        }
    }
}


internal class PreselectBackground(
    val owner: Window,
    opacity: Float = 0.8f,
    color: Color = Color(0, 91, 187, 40),
    private val sensitivity: Float = 3.5f,
) {
    private val window = JWindow(owner).apply {
        this.isVisible = false
        this.background = color
        this.opacity = opacity
    }
    private val rectangle = Rectangle()
    var position = Position.UNKNOWN
    var tabbedPane: TabbedPane.SplitterTabbedPane? = null

    /**
     * @param point component point
     */
    fun show(point: Point, tabbedPane: TabbedPane.SplitterTabbedPane) {
        val rectangle = getRectangle(point, tabbedPane)
        this.tabbedPane = tabbedPane
        window.setSize(rectangle.width, rectangle.height)
        window.location = Point(rectangle.x, rectangle.y)
    }

    fun setVisible(visible: Boolean) {
        window.isVisible = visible
    }

    fun isVisible(): Boolean {
        return window.isVisible
    }

    private fun getRectangle(point: Point, tabbedPane: FlatTabbedPane): Rectangle {
        position = getPosition(point, tabbedPane)

        val tabEmpty = tabbedPane.tabCount < 1
        val tabHeight = tabbedPane.tabHeight
        val locationOnScreen = tabbedPane.locationOnScreen
        val width = tabbedPane.width
        val height = tabbedPane.height

        when (position) {
            Position.MIDDLE -> {
                rectangle.width = width
                rectangle.height = height - if (tabEmpty) 0 else tabHeight
                rectangle.x = locationOnScreen.x
                rectangle.y = locationOnScreen.y + if (tabEmpty) 0 else tabHeight
            }

            Position.TOP -> {
                rectangle.width = width
                rectangle.height = (height - tabHeight) / 2
                rectangle.x = locationOnScreen.x
                rectangle.y = locationOnScreen.y + tabHeight
            }

            Position.LEFT -> {
                rectangle.width = width / 2
                rectangle.height = height - tabHeight
                rectangle.x = locationOnScreen.x
                rectangle.y = locationOnScreen.y + tabHeight
            }

            Position.BOTTOM -> {
                rectangle.width = width
                rectangle.height = (height - tabHeight) / 2
                rectangle.x = locationOnScreen.x
                rectangle.y = locationOnScreen.y + rectangle.height + tabHeight
            }

            Position.RIGHT -> {
                rectangle.width = width / 2
                rectangle.height = height - tabHeight
                rectangle.x = locationOnScreen.x + (width / 2)
                rectangle.y = locationOnScreen.y + tabHeight
            }

            else -> rectangle
        }
        return rectangle
    }

    private fun getPosition(point: Point, tabbedPane: FlatTabbedPane): Position {

        // 如果已经没有 Tab 了，那就只能添加到中间
        if (tabbedPane.tabCount < 1) {
            return Position.MIDDLE
        }

        val proportion = sensitivity
        val width = tabbedPane.width * 1.0
        val height = tabbedPane.height * 1.0
        val blockWidth = width / proportion
        val blockHeight = height / proportion
        val xMoreHalf = blockWidth * (proportion - 1)
        val yMoreHalf = blockHeight * (proportion - 1)

        // 在左边
        val xPosition = if (point.x < blockWidth) {
            Position.LEFT
        } else if (point.x >= xMoreHalf) { // 在右边
            Position.RIGHT
        } else { // 在中间
            Position.MIDDLE
        }

        // 在上边
        val yPosition = if (point.y < blockHeight) {
            Position.TOP
        } else if (point.y >= yMoreHalf) { // 在下边
            Position.BOTTOM
        } else { // 在中间
            Position.MIDDLE
        }

        if (yPosition == Position.MIDDLE) {
            return xPosition
        } else if (xPosition == Position.MIDDLE) {
            return yPosition
        } else { // 出现二义性的时候去谁占的比例小
            if (xPosition == Position.LEFT && yPosition == Position.TOP) {
                return if (point.x / width < point.y / height) {
                    xPosition
                } else {
                    yPosition
                }
            } else if (xPosition == Position.LEFT) {
                return if (point.x / width < (1 - point.y / height)) {
                    xPosition
                } else {
                    yPosition
                }
            } else if (yPosition == Position.TOP) {
                return if ((1 - point.x / width) < point.y / height) {
                    xPosition
                } else {
                    yPosition
                }
            } else {
                return if ((1 - point.x / width) < (1 - point.y / height)) {
                    xPosition
                } else {
                    yPosition
                }
            }
        }

    }

    fun dispose() {
        window.dispose()
    }

    fun hide() {
        window.isVisible = false
        position = Position.UNKNOWN
    }


}