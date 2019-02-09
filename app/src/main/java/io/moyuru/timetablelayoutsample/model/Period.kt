package io.moyuru.timetablelayoutsample.model

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
      "The Price of Agony",
      "Confession",
      "A Day To Remember",
      "Mikoto",
      "Forever Cadence",
      "Altars",
      "Hand of Mercy",
      "Handredth",
      "The Amity Affliction",
      "Napoleon",
      "For the Fallen Dreams",
      "Counterparts",
      "Misery Signals",
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
      "August Burns Red",
      "All That Remains",
      "Lamb of God",
      "As I Lay Dying",
      "Unearth",
      "Killswitch Engage"
    ),
    listOf(
      "American Me",
      "Fathoms",
      "Elitist",
      "Rise of the North Star",
      "Awaken Demons",
      "Legend",
      "Nasty",
      "Thick as Blood",
      "Crystal Lake",
      "Wage War",
      "Architects",
      "Betrayal",
      "Emmure",
      "The Acasia Strain",
      "Hatebreed"
    ), listOf(
      "Carnifex",
      "Bleed from Within",
      "After the Burial",
      "All Shall Perish",
      "Chelsea Grin",
      "The Crimson Armada",
      "Oceano",
      "Upon a Burning Body",
      "Born of Osiris",
      "Veil of Maya",
      "Whitechapel",
      "Bring Me the Horizon",
      "Despised Icon",
      "Suicide Silence",
      "Job for a Cowboy"
    ), listOf(
      "AURAS",
      "Coat of Arms",
      "CHON",
      "Elitist",
      "Forever Orion",
      "Volumes",
      "Poryphia",
      "Periphery"
    )
  )

  val programs = ArrayList<Program>()
  val startAt1 = LocalDateTime.of(2019, 6, 1, 11, 0).toEpochSecond(ZoneOffset.UTC) * 1000
  val startAt2 = LocalDateTime.of(2019, 6, 1, 11, 20).toEpochSecond(ZoneOffset.UTC) * 1000
  val min20 = TimeUnit.MINUTES.toMillis(20)
  lists.forEachIndexed { i, bandNames ->
    bandNames.toPrograms(
      if (i % 2 == 0) startAt1 else startAt2,
      if (bandNames.size == 8) min20 * 2 else min20,
      if (bandNames.size == 8) min20 * 2 else min20,
      i
    )
      .let(programs::addAll)
  }
  return programs
}