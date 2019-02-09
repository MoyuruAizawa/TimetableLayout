package io.moyuru.timetablelayout.decoration

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.moyuru.timetablelayout.drawTextAtCenter
import io.moyuru.timetablelayout.layoutPosition

abstract class ColumnNameDecoration(
  columnCount: Int,
  private val columnWidth: Int,
  private val height: Int,
  private val columnNameTextSize: Float,
  @ColorInt private val columnNameTextColor: Int,
  @ColorInt private val backgroundColor: Int
) : RecyclerView.ItemDecoration() {

  private val textPaint = Paint().apply {
    color = columnNameTextColor
    isAntiAlias = true
    textSize = columnNameTextSize
  }

  private val backgroundPaint = Paint().apply { color = backgroundColor }

  private val lastColumnNumber = columnCount - 1

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDrawOver(c, parent, state)

    c.drawRect(0f, 0f, c.width.toFloat(), height.toFloat(), backgroundPaint)

    if (parent.childCount < 0) return

    val rightView = parent.children.maxBy { it.right } ?: return
    val rightColumnNumber = getColumnNumber(rightView.layoutPosition)
    val range = if (rightColumnNumber == lastColumnNumber) rightColumnNumber downTo 0
    else (rightColumnNumber downTo 0) + (lastColumnNumber downTo rightColumnNumber + 1)

    var offsetX = rightView.right
    range.forEach {
      c.drawTextAtCenter(getColumnName(it), Rect(offsetX - columnWidth, 0, offsetX, height), textPaint)
      offsetX -= columnWidth
    }
  }

  protected abstract fun getColumnNumber(position: Int): Int

  protected abstract fun getColumnName(columnNumber: Int): String
}