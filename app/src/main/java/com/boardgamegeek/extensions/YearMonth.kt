package com.boardgamegeek.extensions

import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun YearMonth.monthsBetween(other: YearMonth) =
    ChronoUnit.MONTHS.between(this, other) + 1

fun YearMonth.monthsToNow() =
    monthsBetween(YearMonth.now())
