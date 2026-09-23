package com.jacobgain.triprabbit.core.util

import org.junit.Assert.*
import org.junit.Test

class DateTimeFormattingTest {
    @Test fun impossibleCalendarDatesAreRejected() {
        assertNull("2026-02-29 12:00".parseDateTime())
        assertNull("2026-04-31 12:00".parseDateTime())
        assertNull("2026-09-22 24:30".parseDateTime())
    }

    @Test fun validLeapDayRoundTrips() {
        val text = "2024-02-29 14:35"
        assertEquals(text, text.parseDateTime()?.inputDateTime())
    }
}
