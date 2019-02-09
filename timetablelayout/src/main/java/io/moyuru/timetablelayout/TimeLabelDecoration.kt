package io.moyuru.timetablelayout

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.ColorInt
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

abstract class TimeLabelDecoration(
  private val width: Int,
  private val heightPerMinute: Int,
  private val timeTextSize: Float,
  @ColorInt private val timeTextColor: Int,
  @ColorInt private val backgroundColor: Int
) : RecyclerView.ItemDecoration() {

  private val textPaint = Paint().apply {
    color = timeTextColor
    isAntiAlias = true
    textSize = timeTextSize
  }

  private val backgroundPaint = Paint().apply { color = backgroundColor }

  private val textHeight by lazy {
    val text = formatUnixMillis(System.currentTimeMillis())
    Rect().apply { textPaint.getTextBounds(text, 0, text.lastIndex, this) }.height()
  }

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDrawOver(c, parent, state)

    c.drawRect(0f, 0f, width.toFloat(), c.height.toFloat(), backgroundPaint)

    val adapter = parent.adapter ?: return
    if (parent.childCount < 0) return

    val startAtList = (0 until adapter.itemCount).map(this::getStartUnixMillis)

    val base = parent.children.filter { it.top <= parent.paddingTop }.minBy { it.top } ?: return
    val baseEpochMillis = startAtList.getOrNull(base.layoutPosition) ?: return

    startAtList
      .filterIndexed { i, startAt -> startAt >= baseEpochMillis && canDecorate(i) }
      .distinct()
      .forEach { startAt ->
        val gap = TimeUnit.MILLISECONDS.toMinutes(startAt - baseEpochMillis) * heightPerMinute
        val top = base.top + gap
        c.drawTextAtCenter(
          formatUnixMillis(startAt),
          Rect(0, top.toInt(), width, (top + textHeight).toInt()),
          textPaint
        )
      }
  }

  protected abstract fun canDecorate(position: Int): Boolean

  protected abstract fun getStartUnixMillis(position: Int): Long

  protected abstract fun formatUnixMillis(unixMillis: Long): String
}