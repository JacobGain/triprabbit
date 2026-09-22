package com.roadlog.core.validation

import com.roadlog.core.model.OdometerReading
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ReadingValidatorTest {
    private fun reading(value: Long) = OdometerReading(value, 1, value, Instant.EPOCH, null, Instant.EPOCH, null)
    @Test fun `first reading accepted`() = assertNull(ReadingValidator.validate(100, null, null))
    @Test fun `higher reading accepted`() = assertNull(ReadingValidator.validate(101, reading(100), null))
    @Test fun `equal reading accepted`() = assertNull(ReadingValidator.validate(100, reading(100), null))
    @Test fun `lower latest reading rejected`() = assertTrue(ReadingValidator.validate(99, reading(100), null) is ReadingError.BelowPrevious)
    @Test fun `negative reading rejected`() = assertEquals(ReadingError.Negative, ReadingValidator.validate(-1, null, null))
    @Test fun `historical reading in range accepted`() = assertNull(ReadingValidator.validate(150, reading(100), reading(200)))
    @Test fun `historical reading below previous rejected`() = assertTrue(ReadingValidator.validate(99, reading(100), reading(200)) is ReadingError.BelowPrevious)
    @Test fun `historical reading above next rejected`() = assertTrue(ReadingValidator.validate(201, reading(100), reading(200)) is ReadingError.AboveNext)
    @Test fun `edited reading respects previous boundary`() = assertTrue(ReadingValidator.validate(9, reading(10), reading(20)) is ReadingError.BelowPrevious)
    @Test fun `edited reading respects next boundary`() = assertTrue(ReadingValidator.validate(21, reading(10), reading(20)) is ReadingError.AboveNext)
}
