package com.sunpawtechnologies.scrollless.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

/** Source of "now" and the user's calendar settings, injectable so date logic stays testable. */
interface TimeProvider {
    fun now(): Instant

    /** Re-read on every call so a device time-zone change is picked up immediately. */
    fun zone(): ZoneId

    fun weekFields(): WeekFields

    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}

object SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
    override fun weekFields(): WeekFields = WeekFields.of(Locale.getDefault())
}
