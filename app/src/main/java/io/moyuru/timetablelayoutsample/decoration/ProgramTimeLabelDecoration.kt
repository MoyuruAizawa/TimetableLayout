package io.moyuru.timetablelayoutsample.decoration

import io.moyuru.timetablelayout.TimeLabelDecoration
import io.moyuru.timetablelayoutsample.model.Period
import io.moyuru.timetablelayoutsample.model.Program
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter

class ProgramTimeLabelDecoration(
  private val periods: List<Period>,
  width: Int,
  heightPerMinute: Int,
  timeTextSize: Float,
  timeTextColor: Int,
  backgroundColor: Int
) : TimeLabelDecoration(width, heightPerMinute, timeTextSize, timeTextColor, backgroundColor) {

  private val formatter = DateTimeFormatter.ofPattern("HH:mm")

  override fun canDecorate(position: Int): Boolean = periods.getOrNull(position) is Program

  override fun getStartUnixMillis(position: Int): Long = periods.getOrNull(position)?.startAt ?: 0

  override fun formatUnixMillis(unixMillis: Long): String =
    LocalDateTime.ofEpochSecond(unixMillis / 1000, 0, ZoneOffset.UTC).format(formatter)
}