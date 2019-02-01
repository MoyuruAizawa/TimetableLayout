package io.moyuru.timetablelayoutmanagersample

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import java.util.concurrent.TimeUnit

sealed class Period(
  open val startAt: Long,
  open val endAt: Long,
  open val stageNumber: Int
)

data class EmptyPeriod(
  override val startAt: Long,
  override val endAt: Long,
  override val stageNumber: Int
) : Period(startAt, endAt, stageNumber)

data class Program(
  val bandName: String,
  override val startAt: Long,
  override val endAt: Long,
  override val stageNumber: Int
) : Period(startAt, endAt, stageNumber)

private fun List<String>.toPrograms(
  firstStartAt: Long,
  duration: Long,
  restDuration: Long,
  stageNumber: Int
): List<Program> {
  var startAt = firstStartAt
  return map {
    Program(it, startAt, startAt + duration, stageNumber)
      .also { startAt += duration + restDuration }
  }
}

fun createPrograms(): List<Program> {
  val lists = listOf(
    listOf(
      "It Prevails",
      "Crystal Lake",
      "Napoleon",
      "Volumes",
      "Betrayal",
      "Emmure",
      "For the Fallen Dreams",
      "Counterparts",
      "Misery Signals",
      "The Acasia Strain",
      "The Ghost Inside"
    ), listOf(
      "Betraying the Martyrs",
      "Darkest Hour",
      "Erra",
      "Miss May I",
      "Obey the Brave",
      "Haven Shall Burn",
      "As Blood Runs Black",
      "I Killed The Prom Queen",
      "Parkway Drive",
      "August Burns Red"
    ), listOf(
      "Carnifex",
      "After the Burial",
      "All Shall Perish",
      "Chelsea Grin",
      "Upon a Burning Body",
      "Veil of Maya",
      "Whitechapel",
      "Despised Icon",
      "Suicide Silence",
      "Job for a Cowboy",
      "Bring Me the Horizon"
    ), listOf(
      "All Heart",
      "Bad Case",
      "Tidus Is Alive",
      "No! Not The Bees!",
      "Settle Your Scores",
      "In Her Own Words",
      "Abandoned By Bears",
      "Chunk! No, Captain Chunk",
      "Set Your Goals",
      "Four Year Strong",
      "A Day To Remember"
    ), listOf(
      "All That Remains",
      "Lamb of God",
      "As I Lay Dying",
      "Unearth",
      "Killswitch Engage"
    )
  )

  val programs = ArrayList<Program>()
  val startAt1 = LocalDateTime.of(2019, 6, 1, 12, 0).toEpochSecond(ZoneOffset.UTC) * 1000
  val startAt2 = LocalDateTime.of(2019, 6, 1, 12, 30).toEpochSecond(ZoneOffset.UTC) * 1000
  lists.forEachIndexed { i, bandNames ->
    bandNames.toPrograms(
      if (i % 2 == 0) startAt1 else startAt2,
      if (bandNames.size <= 6) TimeUnit.MINUTES.toMillis(90) else TimeUnit.MINUTES.toMillis(30),
      TimeUnit.MINUTES.toMillis(30),
      i
    ).let(programs::addAll)
  }
  return programs
}