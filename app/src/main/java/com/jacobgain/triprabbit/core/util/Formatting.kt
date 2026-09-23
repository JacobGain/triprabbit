package com.jacobgain.triprabbit.core.util

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

fun Long.grouped(): String = NumberFormat.getIntegerInstance().format(this)
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val inputFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT)
private val inputDateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
fun Instant.displayDate(): String = atZone(ZoneId.systemDefault()).format(dateFormatter)
fun Instant.displayMonth(): String = atZone(ZoneId.systemDefault()).format(monthFormatter)
fun Instant.inputDateTime(): String = atZone(ZoneId.systemDefault()).format(inputFormatter)
fun Instant.inputDate(): String = atZone(ZoneId.systemDefault()).format(inputDateFormatter)
fun String.parseInputDate(): Instant? = runCatching { LocalDate.parse(this, inputDateFormatter).atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
fun String.parseDateTime(): Instant? = runCatching { java.time.LocalDateTime.parse(this, inputFormatter).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
