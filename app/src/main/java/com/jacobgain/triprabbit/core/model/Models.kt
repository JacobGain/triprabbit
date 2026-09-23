package com.jacobgain.triprabbit.core.model

import java.time.Instant

enum class DistanceUnit(val abbreviation: String) { KILOMETERS("km"), MILES("mi") }

data class Vehicle(
    val id: Long,
    val name: String,
    val make: String?,
    val model: String?,
    val year: Int?,
    val licensePlate: String?,
    val odometerUnit: DistanceUnit,
    val colorKey: String?,
    val notes: String?,
    val createdAt: Instant,
    val archivedAt: Instant?,
)

data class OdometerReading(
    val id: Long,
    val vehicleId: Long,
    val value: Long,
    val recordedAt: Instant,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant?,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentTheme { DEFAULT, BLUE, GREEN, ORANGE, RED, PURPLE, MONOCHROME }
enum class DisplayDensity { COMFORTABLE, COMPACT }

data class AppSettings(
    val selectedVehicleId: Long? = null,
    val firstLaunchComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentTheme: AccentTheme = AccentTheme.DEFAULT,
    val useDynamicColor: Boolean = false,
    val useAmoledBlack: Boolean = false,
    val displayDensity: DisplayDensity = DisplayDensity.COMFORTABLE,
    val showVehicleDetails: Boolean = true,
    val confirmReadingDeletion: Boolean = true,
)

data class MileageStats(
    val current: Long? = null,
    val totalTracked: Long = 0,
    val last30Days: Long = 0,
    val currentYear: Long = 0,
    val readingCount: Int = 0,
    val averagePerMonth: Long = 0,
)

data class VehicleInput(
    val name: String,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val licensePlate: String? = null,
    val odometerUnit: DistanceUnit,
    val colorKey: String? = null,
    val notes: String? = null,
)
