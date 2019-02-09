package io.moyuru.timetablelayoutsample.decoration

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import io.moyuru.timetablelayout.decoration.TimeLabelDecoration
import io.moyuru.timetablelayoutsample.R
import io.moyuru.timetablelayoutsample.model.Period
import io.moyuru.timetablelayoutsample.model.Program
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

class ProgramTimeLabelDecoration(context: Context, private val periods: List<Period>, heightPerMin: Int) :
  TimeLabelDecoration(
    context.resources.getDimensionPixelSize(R.dimen.timeLabelWidth),
    heightPerMin,
    context.resources.getDimension(R.dimen.timeLabelTextSize),
    Color.WHITE,
    ContextCompat.getColor(context, R.color.black)
  ) {

  private val formatter = DateTimeFormatter.ofPattern("HH:mm")

  override fun canDecorate(position: Int): Boolean = periods.getOrNull(position) is Program

  override fun getStartUnixMillis(position: Int): Long = periods.getOrNull(position)?.startAt ?: 0

  override fun formatUnixMillis(unixMillis: Long): String =
    LocalDateTime.ofEpochSecond(unixMillis / 1000, 0, ZoneOffset.UTC).format(formatter)
}