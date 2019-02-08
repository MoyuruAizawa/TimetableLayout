package io.moyuru.timetablelayoutsample.decoration

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import io.moyuru.timetablelayoutsample.dp
import io.moyuru.timetablelayoutsample.model.Period

class Decoration(private val periods: List<Period>, private val columnWidth: Int, private val stageNameHeight: Int) :
  RecyclerView.ItemDecoration() {
  private val paintBlack = Paint().apply { color = Color.parseColor("#222222") }
  private val stageNamePaint = Paint().apply {
    color = Color.WHITE
    isAntiAlias = true
    textSize = 16.dp.toFloat()
  }
  private val maxStageNumber = requireNotNull(periods.maxBy { it.stageNumber }?.stageNumber)

  override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    super.onDrawOver(c, parent, state)

    if (parent.childCount == 0) return

    c.drawRect(Rect(0, 0, c.width, stageNameHeight), paintBlack)

    val rightView = parent.children.maxBy { it.right } ?: return
    val rightPeriod = periods[parent.getChildAdapterPosition(rightView)]
    var offset = rightView.right
    ((rightPeriod.stageNumber downTo 0) + (maxStageNumber downTo rightPeriod.stageNumber + 1)).forEach {
      c.drawTextAtCenter(it.stageName, Rect(offset - columnWidth, 0, offset, stageNameHeight), stageNamePaint)
      offset -= columnWidth
    }
  }

  private val Int.stageName: String
    get() {
      return when (this) {
        0 -> "Hardcore"
        1 -> "Metalcore"
        2 -> "Deathcore"
        3 -> "Easycore"
        else -> "Metalcore Legends"
      }
    }

  private fun Canvas.drawTextAtCenter(text: String, rect: Rect, paint: Paint) {
    val baseX = rect.centerX().toFloat() - paint.measureText(text) / 2f
    val textBounds = Rect().apply { paint.getTextBounds(text, 0, text.length - 1, this) }
    val baseY = rect.centerY() +
        if (textBounds.height() != 0) textBounds.height() / 2f
        else -(paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
    drawText(text, baseX, baseY, paint)
  }
}