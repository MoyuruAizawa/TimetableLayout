# TimetableLayout

![API](https://img.shields.io/badge/API-16%2B-green.svg)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)  
TimetableLayout is a layout manager and item decorations for display the timetable.

![sample01](https://github.com/MoyuruAizawa/Images/blob/master/TimetableLayout/sample_01.gif?raw=true)

# Usage
**LayoutManager**
```kotlin
binding.recyclerView.layoutManager = TimetableLayoutManager(columnWidth, heightPerMin) { position ->
  val period = periods[position]
  TimetableLayoutManager.PeriodInfo(period.startAt, period.endAt, period.stageNumber)
}
```

**ItemDecoration**
```kotlin
binding.recyclerView.addItemDecoration(ProgramTimeLabelDecoration(context, periods, heightPerMin))
binding.recyclerView.addItemDecoration(StageNameDecoration(context, periods, columnCount))

class ProgramTimeLabelDecoration(private val periods: List<Period>, ...) : TimeLabelDecoration(...) {

  private val formatter = DateTimeFormatter.ofPattern("HH:mm")

  override fun canDecorate(position: Int): Boolean = periods.getOrNull(position) is Program

  override fun getStartUnixMillis(position: Int): Long = periods.getOrNull(position)?.startAt ?: 0

  override fun formatUnixMillis(unixMillis: Long): String =
    LocalDateTime.ofEpochSecond(unixMillis / 1000, 0, ZoneOffset.UTC).format(formatter)
}

class StageNameDecoration(private val periods: List<Period>, ...) : ColumnNameDecoration(...) {

  override fun getColumnNumber(position: Int): Int {
    return periods.getOrNull(position)?.stageNumber ?: 0
  }

  override fun getColumnName(columnNumber: Int): String {
    return when (columnNumber) {
      0 -> "Melodic Hardcore"
      1 -> "Metalcore"
      2 -> "Hardcore"
      3 -> "Deathcore"
      else -> "Djent"
    }
  }
}
```

[**Sample**](https://github.com/MoyuruAizawa/TimetableLayout/blob/master/app/src/main/java/io/moyuru/timetablelayoutsample/MainActivity.kt)

# License
```
Copyright 2019 Moyuru Aizawa

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
