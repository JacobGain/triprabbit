package gain.jacob.roadjournal.core.validation

import gain.jacob.roadjournal.core.model.OdometerReading

sealed class ReadingError(message: String) : IllegalArgumentException(message) {
    data object Negative : ReadingError("Odometer readings cannot be negative.")
    data class BelowPrevious(val value: Long) : ReadingError("This reading is lower than the previous reading of $value.")
    data class AboveNext(val value: Long) : ReadingError("This reading is higher than the next reading of $value.")
    data object VehicleMissing : ReadingError("The vehicle no longer exists.")
    data object ReadingMissing : ReadingError("The reading no longer exists.")
}

object ReadingValidator {
    fun validate(value: Long, previous: OdometerReading?, next: OdometerReading?): ReadingError? = when {
        value < 0 -> ReadingError.Negative
        previous != null && value < previous.value -> ReadingError.BelowPrevious(previous.value)
        next != null && value > next.value -> ReadingError.AboveNext(next.value)
        else -> null
    }
}
