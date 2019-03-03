package io.moyuru.timetablelayout.layoutmanager

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import androidx.core.util.forEach
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import io.moyuru.timetablelayout.BuildConfig
import io.moyuru.timetablelayout.adapterPosition
import io.moyuru.timetablelayout.getOrPut
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class TimetableLayoutManager(
  private val columnWidth: Int,
  private val heightPerMinute: Int,
  private val periodLookUp: (position: Int) -> PeriodInfo
) : RecyclerView.LayoutManager() {

  companion object {
    private const val NO_TIME = -1
  }

  class PeriodInfo(val startUnixMillis: Long, val endUnixMillis: Long, val columnNumber: Int)

  private data class Period(
    val startUnixMin: Int,
    val endUnixMin: Int,
    val columnNumber: Int,
    val adapterPosition: Int,
    val positionInColumn: Int
  ) {
    val durationMin = endUnixMin - startUnixMin
  }

  private class Anchor {
    val top = SparseIntArray()
    val bottom = SparseIntArray()
    var leftColumn = NO_POSITION
    var rightColumn = NO_POSITION

    fun reset() {
      top.clear()
      bottom.clear()
      leftColumn = NO_POSITION
      rightColumn = NO_POSITION
    }
  }

  private enum class Direction {
    LEFT, TOP, RIGHT, BOTTOM
  }

  private data class SaveState(val position: Int, val left: Int, val top: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
      parcel.readInt(),
      parcel.readInt(),
      parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeInt(position)
      parcel.writeInt(left)
      parcel.writeInt(top)
    }

    override fun describeContents(): Int {
      return 0
    }

    companion object CREATOR : Parcelable.Creator<SaveState> {
      override fun createFromParcel(parcel: Parcel): SaveState {
        return SaveState(parcel)
      }

      override fun newArray(size: Int): Array<SaveState?> {
        return arrayOfNulls(size)
      }
    }
  }

  private val parentLeft get() = paddingLeft
  private val parentTop get() = paddingTop
  private val parentRight get() = width - paddingRight
  private val parentBottom get() = height - paddingBottom

  private val periods = ArrayList<Period>()
  private val columns = SparseArray<ArrayList<Period>>()
  private val anchor = Anchor()

  private var firstStartUnixMin = NO_TIME
  private var lastEndUnixMin = NO_TIME

  private var pendingScrollPosition = NO_POSITION
  private var saveState: SaveState? = null

  override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
    return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    saveState = (state as? SaveState)
  }

  override fun onSaveInstanceState(): Parcelable? {
    if (childCount == 0) return null

    val view = findFirstVisibleView() ?: return null
    return SaveState(
      view.adapterPosition,
      getDecoratedLeft(view),
      getDecoratedTop(view)
    )
  }

  override fun onLayoutChildren(recycler: Recycler, state: State) {
    if (itemCount == 0) {
      detachAndScrapAttachedViews(recycler)
      periods.clear()
      columns.clear()
      anchor.reset()
      saveState = null
      return
    }

    calculateColumns()

    if (pendingScrollPosition != NO_POSITION) {
      anchor.reset()
      detachAndScrapAttachedViews(recycler)
      periods.getOrNull(pendingScrollPosition)?.let { period ->
        anchor.leftColumn = period.columnNumber
        fillHorizontalChunk(period.columnNumber, parentLeft, parentTop, period, true, recycler)
        fixBottomLayoutGap(recycler)
      }
      return
    }

    val firstVisibleView = findFirstVisibleView()
    val restoredOffsetX = saveState?.left ?: firstVisibleView?.let(this::getDecoratedLeft)
    val restoredOffsetY = saveState?.top ?: firstVisibleView?.let(this::getDecoratedTop)
    val restoredPeriod = (saveState?.position ?: anchor.top.get(anchor.leftColumn, -1)).let(periods::getOrNull)

    anchor.reset()
    detachAndScrapAttachedViews(recycler)

    if (restoredOffsetX != null && restoredOffsetY != null && restoredPeriod != null) {
      anchor.leftColumn = restoredPeriod.columnNumber
      fillHorizontalChunk(restoredPeriod.columnNumber, restoredOffsetX, restoredOffsetY, restoredPeriod, true, recycler)
    } else {
      anchor.leftColumn = columns.keyAt(0)
      val columnCount = columns.size()
      val offsetY = parentTop
      var offsetX = parentLeft
      for (columnNumber in 0 until columnCount) {
        offsetX += addColumn(columns[columnNumber].first(), offsetX, offsetY, true, recycler)
        anchor.rightColumn = columnNumber
        if (offsetX > parentRight) break
      }
    }
  }

  override fun onLayoutCompleted(state: State?) {
    pendingScrollPosition = NO_POSITION
    saveState = null
  }

  override fun scrollToPosition(position: Int) {
    if (position < 0 || position >= itemCount) return
    pendingScrollPosition = position
    requestLayout()
  }

  override fun canScrollVertically() = true

  override fun canScrollHorizontally() = true

  override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
    if (dy == 0) return 0

    val actualDy = calculateVerticallyScrollAmount(dy)
    if (actualDy == 0) return 0
    offsetChildrenVertical(-actualDy)
    if (actualDy > 0) {
      // recycle
      recycleTop(recycler)

      // append
      anchor.bottom.forEach { columnNum, position ->
        val view = findViewByPosition(position) ?: return@forEach
        val bottom = getDecoratedBottom(view)
        if (bottom < parentBottom) {
          val left = getDecoratedLeft(view)
          val period = periods.getOrNull(position) ?: return@forEach
          val nextPeriod = columns.get(columnNum).getOrNull(period.positionInColumn + 1) ?: return@forEach
          addPeriodsToColumn(nextPeriod, left, bottom, true, recycler)
        }
      }
    } else {
      // recycle
      recycleBottom(recycler)

      // prepend
      anchor.top.forEach { columnNum, position ->
        val view = findViewByPosition(position) ?: return@forEach
        val top = getDecoratedTop(view)
        if (top > parentTop) {
          val left = getDecoratedLeft(view)
          val period = periods.getOrNull(position) ?: return@forEach
          val nextPeriod = columns.get(columnNum).getOrNull(period.positionInColumn - 1) ?: return@forEach
          addPeriodsToColumn(nextPeriod, left, top, false, recycler)
        }
      }
    }
    return actualDy
  }

  override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: State): Int {
    if (dx == 0) return 0

    val rightView = findRightView() ?: return 0
    val leftView = findLeftView() ?: return 0
    val actualDx = calculateHorizontallyScrollAmount(dx, getDecoratedLeft(leftView), getDecoratedRight(rightView))
    if (actualDx == 0) return 0
    offsetChildrenHorizontal(-actualDx)
    if (actualDx > 0) {
      // recycle
      if (getDecoratedRight(leftView) < parentLeft) recycleLeft(recycler)

      // append
      val right = getDecoratedRight(rightView)
      if (right < parentRight) {
        val topView = findTopView() ?: return 0
        val top = getDecoratedTop(topView)
        val topPeriod = periods[topView.adapterPosition]
        val nextColumn = anchor.rightColumn.getNextColumn()
        fillHorizontalChunk(nextColumn, right, top, topPeriod, true, recycler)
      }
    } else {
      // recycle
      if (getDecoratedLeft(rightView) > parentRight) recycleRight(recycler)

      // prepend
      val left = getDecoratedLeft(leftView)
      if (left > parentLeft) {
        val topView = findTopView() ?: return 0
        val top = getDecoratedTop(topView)
        val topPeriod = periods[topView.adapterPosition]
        val previousColumn = anchor.leftColumn.getPreviousColumn()
        fillHorizontalChunk(previousColumn, left, top, topPeriod, false, recycler)
      }
    }

    return dx
  }

  private fun calculateVerticallyScrollAmount(dy: Int): Int {
    return if (dy > 0) {
      val bottomView = findBottomView() ?: return 0
      val period = periods.getOrNull(bottomView.adapterPosition) ?: return 0
      val bottom = getDecoratedBottom(bottomView)
      if (period.endUnixMin == lastEndUnixMin) if (bottom == parentBottom) 0 else min(dy, bottom - parentBottom)
      else dy
    } else {
      val topView = findTopView() ?: return 0
      val period = periods.getOrNull(topView.adapterPosition) ?: return 0
      val top = getDecoratedTop(topView)
      if (period.startUnixMin == firstStartUnixMin) if (top == parentTop) 0 else max(dy, top - parentTop)
      else dy
    }
  }

  private fun calculateHorizontallyScrollAmount(dx: Int, left: Int, right: Int): Int {
    if (!anchor.leftColumn.isFirstColumn() || !anchor.rightColumn.isLastColumn()) return dx

    return if (dx > 0) {
      if (anchor.rightColumn.isLastColumn())
        if (right <= parentRight) 0 else min(dx, right - parentRight)
      else dx
    } else {
      if (anchor.leftColumn.isFirstColumn())
        if (left >= parentLeft) 0 else max(dx, left - parentLeft)
      else dx
    }
  }

  private fun addPeriod(
    period: Period,
    direction: Direction,
    offsetX: Int,
    offsetY: Int,
    recycler: Recycler
  ): Pair<Int, Int> {
    val view = recycler.getViewForPosition(period.adapterPosition)
    addView(view)
    measureChild(view, period)
    val width = getDecoratedMeasuredWidth(view)
    val height = getDecoratedMeasuredHeight(view)
    val left = if (direction == Direction.LEFT) offsetX - width else offsetX
    val top = if (direction == Direction.TOP) offsetY - height else offsetY
    val right = left + width
    val bottom = top + height
    layoutDecorated(view, left, top, right, bottom)
    return width to height
  }

  private fun addPeriodsToColumn(
    startPeriod: Period,
    offsetX: Int,
    startY: Int,
    isAppend: Boolean,
    recycler: Recycler
  ): Int {
    val column = columns.get(startPeriod.columnNumber) ?: return 0
    val direction = if (isAppend) Direction.BOTTOM else Direction.TOP
    var offsetY = startY
    val range = if (isAppend) startPeriod.positionInColumn until column.size else startPeriod.positionInColumn downTo 0
    for (i in range) {
      val period = column[i]
      val (_, height) = addPeriod(period, direction, offsetX, offsetY, recycler)
      if (isAppend) {
        anchor.bottom.put(period.columnNumber, period.adapterPosition)
        offsetY += height
        if (offsetY > parentBottom) return offsetY - startY
      } else {
        anchor.top.put(period.columnNumber, period.adapterPosition)
        offsetY -= height
        if (offsetY < parentTop) return startY - offsetY
      }
    }
    return (offsetY - startY).absoluteValue
  }

  private fun addColumn(
    startPeriod: Period,
    offsetX: Int,
    startY: Int,
    isAppend: Boolean,
    recycler: Recycler
  ): Int {
    val columnNum = startPeriod.columnNumber
    val periods = columns[columnNum] ?: return 0
    val direction = if (isAppend) Direction.RIGHT else Direction.LEFT
    var offsetY = startY
    var columnWidth = 0
    for (i in startPeriod.positionInColumn until periods.size) {
      val period = periods[i]
      val (width, height) = addPeriod(period, direction, offsetX, offsetY, recycler)

      offsetY += height
      columnWidth = width

      if (i == startPeriod.positionInColumn) anchor.top.put(columnNum, period.adapterPosition)
      anchor.bottom.put(columnNum, period.adapterPosition)
      if (offsetY > parentBottom) break
    }
    return columnWidth
  }

  private fun fillHorizontalChunk(
    startColumnNum: Int,
    startX: Int,
    baseY: Int,
    basePeriod: Period,
    isAppend: Boolean,
    recycler: Recycler
  ): Int {
    val lastColumnNum = columns.size - 1
    val range = if (isAppend) {
      if (startColumnNum > 0) (startColumnNum..lastColumnNum) + (0 until startColumnNum)
      else startColumnNum..lastColumnNum
    } else {
      if (startColumnNum < lastColumnNum) (startColumnNum downTo 0) + (lastColumnNum downTo startColumnNum + 1)
      else (startColumnNum downTo 0)
    }

    var offsetX = startX
    val added = arrayListOf<Int>()
    for (nextColumnNum in range) {
      val startPeriod = calculateStartPeriodInColumn(nextColumnNum, baseY, basePeriod) ?: continue
      val offsetY = baseY + (startPeriod.startUnixMin - basePeriod.startUnixMin) * heightPerMinute
      val width = addColumn(startPeriod, offsetX, offsetY, isAppend, recycler)
      added += nextColumnNum
      if (isAppend) {
        anchor.rightColumn = nextColumnNum
        offsetX += width
      } else {
        anchor.leftColumn = nextColumnNum
        offsetX -= width
      }
      if (isAppend && offsetX > parentRight) break
      if (!isAppend && offsetX < parentLeft) break
    }

    return (offsetX - startX).absoluteValue
  }

  private fun recycleTop(recycler: Recycler) {
    (anchor.leftColumn..anchor.rightColumn).forEach { columnNum ->
      val column = columns[columnNum]
      val from = periods[anchor.top[columnNum]].positionInColumn
      val to = min(periods[anchor.bottom[columnNum]].positionInColumn + 1, column.size - 1)
      val range = column.subList(from, to)
      range.forEachIndexed { index, period ->
        val view = findViewByPosition(period.adapterPosition) ?: return
        if (getDecoratedBottom(view) >= parentTop) return

        removeAndRecycleView(view, recycler)
        anchor.top.put(columnNum, range.getOrNull(index + 1)?.adapterPosition ?: return)
      }
    }
  }

  private fun recycleBottom(recycler: Recycler) {
    (anchor.leftColumn..anchor.rightColumn).forEach { columnNum ->
      val column = columns[columnNum]
      val from = periods[anchor.top[columnNum]].positionInColumn
      val to = min(periods[anchor.bottom[columnNum]].positionInColumn + 1, column.size - 1)
      val range = column.subList(from, to).asReversed()
      range.forEachIndexed { index, period ->
        val view = findViewByPosition(period.adapterPosition) ?: return
        if (getDecoratedTop(view) <= parentBottom) return

        removeAndRecycleView(view, recycler)
        anchor.bottom.put(columnNum, range.getOrNull(index + 1)?.adapterPosition ?: return)
      }
    }
  }

  private fun recycleLeft(recycler: Recycler) {
    val removed = arrayListOf<Int>()
    val range = if (anchor.rightColumn > anchor.leftColumn) (anchor.leftColumn..anchor.rightColumn)
    else (anchor.leftColumn until columns.size()) + (0..anchor.rightColumn)
    range.forEach { columnNum ->
      val views = findViewsByColumn(columnNum)
      val view = views.firstOrNull() ?: return@forEach
      if (getDecoratedRight(view) >= parentLeft) return@forEach

      views.forEach { removeAndRecycleView(it, recycler) }
      removed += columnNum
      anchor.leftColumn = anchor.leftColumn.getNextColumn()
    }
  }

  private fun recycleRight(recycler: Recycler) {
    val range = if (anchor.leftColumn < anchor.rightColumn) (anchor.rightColumn downTo anchor.leftColumn)
    else (anchor.rightColumn downTo 0) + ((columns.size() - 1) downTo anchor.leftColumn)
    range.forEach { columnNum ->
      val views = findViewsByColumn(columnNum)
      val view = views.firstOrNull() ?: return@forEach
      if (getDecoratedLeft(view) <= parentRight) return@forEach

      views.forEach { removeAndRecycleView(it, recycler) }
      anchor.rightColumn = anchor.rightColumn.getPreviousColumn()
    }
  }

  private fun fixBottomLayoutGap(recycler: Recycler) {
    val bottomView = findBottomView() ?: return
    val bottomPeriod = periods[bottomView.adapterPosition]
    val bottom = getDecoratedBottom(bottomView)
    if (bottom > parentBottom) return

    val expectedGap = (lastEndUnixMin - bottomPeriod.endUnixMin) * heightPerMinute
    val actualGap = (parentBottom - bottom)
    offsetChildrenVertical(actualGap - expectedGap)
    anchor.top.forEach { columnNum, position ->
      val view = findViewByPosition(position) ?: return@forEach
      val top = getDecoratedTop(view)
      if (top > parentTop) {
        val left = getDecoratedLeft(view)
        val anchorPeriod = periods.getOrNull(position) ?: return@forEach
        val nextPeriod = columns.get(columnNum)
          .getOrNull(anchorPeriod.positionInColumn - 1) ?: return@forEach
        addPeriodsToColumn(nextPeriod, left, top, false, recycler)
      }
    }
  }

  private fun measureChild(view: View, period: Period) {
    val lp = view.layoutParams as RecyclerView.LayoutParams
    lp.width = columnWidth
    lp.height = period.durationMin * heightPerMinute

    val insets = Rect().apply { calculateItemDecorationsForChild(view, this) }
    val widthSpec = getChildMeasureSpec(
      width,
      widthMode,
      paddingLeft + paddingRight + insets.left + insets.right,
      lp.width,
      true
    )
    val heightSpec = getChildMeasureSpec(
      height,
      heightMode,
      paddingTop + paddingBottom + insets.top + insets.bottom,
      lp.height,
      true
    )
    view.measure(widthSpec, heightSpec)
  }

  private fun findTopView(): View? {
    var minTop: Int? = null
    var minView: View? = null
    anchor.top.forEach { _, position ->
      val view = findViewByPosition(position) ?: return@forEach
      val top = getDecoratedTop(view)
      if (minView == null) {
        minView = view
        minTop = top
        return@forEach
      }
      minTop?.let {
        if (top < it) {
          minView = view
          minTop = top
        }
      }
    }
    return minView
  }

  private fun findBottomView(): View? {
    var maxBottom: Int? = null
    var maxView: View? = null
    anchor.bottom.forEach { _, position ->
      val view = findViewByPosition(position) ?: return@forEach
      val bottom = getDecoratedBottom(view)
      if (maxView == null) {
        maxView = view
        maxBottom = bottom
        return@forEach
      }
      maxBottom?.let {
        if (bottom > it) {
          maxView = view
          maxBottom = bottom
        }
      }
    }
    return maxView
  }

  private fun findLeftView() = findViewByColumn(anchor.leftColumn)

  private fun findRightView() = findViewByColumn(anchor.rightColumn)

  private fun findViewByColumn(columnNumber: Int): View? {
    (0 until childCount).forEach { layoutPosition ->
      val view = getChildAt(layoutPosition) ?: return@forEach
      val period = periods[view.adapterPosition]
      if (period.columnNumber == columnNumber) return view
    }
    return null
  }

  private fun findViewsByColumn(columnNumber: Int): List<View> {
    return (0 until childCount).mapNotNull { layoutPosition ->
      val view = getChildAt(layoutPosition) ?: return@mapNotNull null
      val period = periods[view.adapterPosition]
      if (period.columnNumber == columnNumber) view else null
    }
  }

  private fun findFirstVisibleView(): View? {
    if (childCount == 0) return null

    return (0 until childCount).asSequence()
      .mapNotNull(this::getChildAt)
      .filter { getDecoratedLeft(it) <= parentLeft }
      .minWith(Comparator { viewL, viewR ->
        getDecoratedTop(viewL) - getDecoratedTop(viewR)
      })
  }

  private fun calculateStartPeriodInColumn(columnNumber: Int, top: Int, topPeriod: Period): Period? {
    val periods = columns[columnNumber] ?: return null
    var maxTopPeriod: Period? = null
    periods.filter { it.startUnixMin <= topPeriod.endUnixMin && it.endUnixMin >= topPeriod.startUnixMin }
      .forEach { period ->
        val gapHeight = (period.startUnixMin - topPeriod.startUnixMin) * heightPerMinute
        if (top + gapHeight <= parentTop)
          maxTopPeriod = maxTopPeriod?.let { if (it.startUnixMin < period.startUnixMin) period else it } ?: period
      }
    return maxTopPeriod
  }

  private fun calculateColumns() {
    periods.clear()
    columns.clear()
    firstStartUnixMin = NO_TIME
    lastEndUnixMin = NO_TIME

    (0 until itemCount).forEach {
      val periodInfo = periodLookUp(it)
      val column = columns.getOrPut(periodInfo.columnNumber) { ArrayList() }

      val period = Period(
        TimeUnit.MILLISECONDS.toMinutes(periodInfo.startUnixMillis).toInt(),
        TimeUnit.MILLISECONDS.toMinutes(periodInfo.endUnixMillis).toInt(),
        periodInfo.columnNumber,
        adapterPosition = it,
        positionInColumn = column.size
      )
      periods.add(period)
      column.add(period)

      if (it == 0) {
        firstStartUnixMin = period.startUnixMin
        lastEndUnixMin = period.endUnixMin
      } else {
        firstStartUnixMin = min(period.startUnixMin, firstStartUnixMin)
        lastEndUnixMin = max(period.endUnixMin, lastEndUnixMin)
      }
    }

    for (i in 0 until columns.size()) {
      val key = columns.keyAt(i)
      val periods = columns[i]
      if (key != i) {
        logw("column numbers are not Zero-based numbering.")
        break
      }
      if (periods == null || periods.isEmpty())
        logw("column $i is null or empty.")
    }
  }

  private fun Int.isFirstColumn() = this == 0

  private fun Int.isLastColumn() = this == columns.size() - 1

  private fun Int.getNextColumn() = if (this == columns.size - 1) 0 else this + 1

  private fun Int.getPreviousColumn() = if (this == 0) columns.size - 1 else this - 1

  private fun logw(log: String) {
    if (BuildConfig.DEBUG) Log.w(TimetableLayoutManager::class.java.simpleName, log)
  }
}