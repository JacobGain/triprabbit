# RoadJournal

## 1. Product Overview

RoadJournal is a simple, local-first Android application for recording and reviewing vehicle odometer readings.

The application should replace the experience of an older discontinued odometer application while improving on it with:

- Multiple vehicles
- Historical odometer records
- Useful mileage statistics
- A customizable interface
- Fast entry of new readings
- Local backup/export
- A clean, modern Android interface

The application should prioritize utility over complexity.

A user should be able to open the app, see their current vehicle and odometer reading, record a new reading in only a few taps, and immediately close the application.

The app should work completely offline.

---

## 2. Technology Stack

Use:

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle / ViewModel
- Kotlin Coroutines
- Kotlin Flow / StateFlow
- Room
- DataStore
- Hilt
- KSP where applicable
- Kotlin serialization where useful
- JUnit
- AndroidX test libraries
- Compose UI testing

Architecture:

**MVVM + Repository + Unidirectional Data Flow**

Do not introduce unnecessary architectural abstraction.

The application should initially use a single Gradle `app` module with package-by-feature organization. The structure should make later modularization possible without starting as a multi-module project.

---

## 3. Architectural Principles

### Source of Truth

Room is the source of truth for:

- Vehicles
- Odometer readings

DataStore is the source of truth for:

- Application preferences
- Theme preferences
- UI customization
- Last selected vehicle
- Other lightweight global state

Composable functions must never directly access Room or DataStore.

Data flows:

```text
Room / DataStore
       ↓
Repositories
       ↓
ViewModels
       ↓
StateFlow<UiState>
       ↓
Compose UI
```

User actions flow in the opposite direction:

```text
Compose UI
    ↓
UI Event
    ↓
ViewModel
    ↓
Repository
    ↓
Room / DataStore
```

---

## 4. Project Structure

Use approximately:

```text
gain.jacob.roadjournal
│
├── RoadJournalApplication.kt
├── MainActivity.kt
│
├── core
│   ├── database
│   │   ├── RoadJournalDatabase.kt
│   │   ├── dao
│   │   ├── entity
│   │   └── converter
│   │
│   ├── datastore
│   │   └── AppPreferencesDataSource.kt
│   │
│   ├── designsystem
│   │   ├── component
│   │   ├── theme
│   │   ├── typography
│   │   └── icon
│   │
│   ├── model
│   ├── repository
│   ├── navigation
│   ├── util
│   └── validation
│
├── feature
│   ├── dashboard
│   │   ├── DashboardScreen.kt
│   │   ├── DashboardViewModel.kt
│   │   ├── DashboardUiState.kt
│   │   └── component
│   │
│   ├── readings
│   │   ├── AddReadingScreen.kt
│   │   ├── AddReadingViewModel.kt
│   │   ├── ReadingHistoryScreen.kt
│   │   ├── ReadingHistoryViewModel.kt
│   │   └── component
│   │
│   ├── vehicles
│   │   ├── VehicleListScreen.kt
│   │   ├── VehicleDetailsScreen.kt
│   │   ├── VehicleEditorScreen.kt
│   │   ├── VehicleViewModel.kt
│   │   └── component
│   │
│   ├── statistics
│   │   ├── StatisticsScreen.kt
│   │   ├── StatisticsViewModel.kt
│   │   └── component
│   │
│   ├── settings
│   │   ├── SettingsScreen.kt
│   │   ├── AppearanceScreen.kt
│   │   ├── DataManagementScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   └── component
│   │
│   └── onboarding
│       ├── OnboardingScreen.kt
│       └── OnboardingViewModel.kt
│
└── di
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    └── DataStoreModule.kt
```

Avoid generic folders such as enormous `utils`, `helpers`, or `components` directories.

Feature-specific components should remain inside the feature.

Only genuinely reusable components belong under `core`.

---

## 5. Core Domain Model

There are two primary domain objects:

```text
Vehicle
   │
   └── 0...n OdometerReadings
```

Each odometer reading belongs to exactly one vehicle.

---

## 6. Vehicle Model

```kotlin
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
    val archivedAt: Instant?
)
```

### Required Field

`name`

Examples:

- Golf
- Daily Driver
- Civic
- Dad's Truck
- 2017 Golf

### Optional Metadata

- Make
- Model
- Year
- License plate
- Notes
- Vehicle colour/accent

Do not require unnecessary information to create a car.

A new vehicle should be creatable with:

```text
Name
Odometer unit
Initial reading
```

Everything else is optional.

---

## 7. Distance Unit

```kotlin
enum class DistanceUnit {
    KILOMETERS,
    MILES
}
```

The unit belongs to the **vehicle**, not globally to the application.

Reason:

A user could own vehicles originating from different markets.

Do not silently convert stored odometer values between kilometres and miles.

---

## 8. Odometer Reading Model

```kotlin
data class OdometerReading(
    val id: Long,
    val vehicleId: Long,
    val value: Long,
    val recordedAt: Instant,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant?
)
```

Store readings as whole units initially.

Examples:

```text
124,521 km
87,214 mi
```

Do not use floating point storage for odometer values.

---

## 9. Room Schema

### VehicleEntity

```text
vehicles
---------
id              INTEGER PRIMARY KEY AUTOINCREMENT
name            TEXT NOT NULL
make            TEXT NULL
model           TEXT NULL
year            INTEGER NULL
license_plate   TEXT NULL
odometer_unit   TEXT NOT NULL
color_key       TEXT NULL
notes           TEXT NULL
created_at      INTEGER NOT NULL
archived_at     INTEGER NULL
```

### OdometerReadingEntity

```text
odometer_readings
-----------------
id              INTEGER PRIMARY KEY AUTOINCREMENT
vehicle_id      INTEGER NOT NULL
value           INTEGER NOT NULL
recorded_at     INTEGER NOT NULL
note            TEXT NULL
created_at      INTEGER NOT NULL
updated_at      INTEGER NULL
```

Foreign key:

```text
vehicle_id → vehicles.id
ON DELETE CASCADE
```

Indexes:

```text
vehicle_id
(vehicle_id, recorded_at)
(vehicle_id, value)
```

---

## 10. Reading Rules

Normally, an odometer should never decrease.

When adding a reading:

```text
newReading >= latestReading
```

If the new value is smaller than the latest value, block normal submission.

Possible message:

```text
This reading is lower than the previous reading of 124,521 km.
```

Editing old readings requires slightly different logic.

For a reading at time `T`:

```text
previous.value <= edited.value <= next.value
```

If either neighbour does not exist, only enforce the available boundary.

### Exceptions

Do not build odometer rollover or odometer replacement into V1.

The data model should not make those features impossible later.

---

## 11. Repositories

Create interfaces:

```kotlin
interface VehicleRepository
```

Responsibilities:

```text
observeVehicles()
observeActiveVehicles()
observeVehicle(id)
createVehicle(...)
updateVehicle(...)
archiveVehicle(id)
deleteVehicle(id)
```

And:

```kotlin
interface OdometerRepository
```

Responsibilities:

```text
observeReadings(vehicleId)
observeLatestReading(vehicleId)
observeReading(id)

addReading(...)
updateReading(...)
deleteReading(...)

observeDistanceStats(vehicleId)
```

And:

```kotlin
interface SettingsRepository
```

Responsibilities:

```text
observeSettings()
setThemeMode(...)
setAccentTheme(...)
setDynamicColor(...)
setDisplayDensity(...)
setSelectedVehicle(...)
...
```

Repository implementations are the only parts of the app that should coordinate persistence technologies.

---

## 12. Settings Model

```kotlin
data class AppSettings(
    val themeMode: ThemeMode,
    val accentTheme: AccentTheme,
    val useDynamicColor: Boolean,
    val useAmoledBlack: Boolean,
    val displayDensity: DisplayDensity,
    val showVehicleDetails: Boolean,
    val confirmReadingDeletion: Boolean,
    val selectedVehicleId: Long?,
    val firstLaunchComplete: Boolean
)
```

Possible enums:

```kotlin
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DisplayDensity {
    COMFORTABLE,
    COMPACT
}
```

---

## 13. Appearance / Theme System

Customization is an important feature of RoadJournal.

Use Material 3 as the underlying design system.

### Theme Mode

Support:

- System
- Light
- Dark

### Dynamic Colour

Support Android Material You dynamic colour.

Toggle:

```text
Use system colours
```

If enabled on supported devices, dynamic colour overrides the manually selected accent.

### Built-in Accent Themes

Provide approximately 6 presets initially.

For example:

```text
Default
Blue
Green
Orange
Red
Purple
Monochrome
```

These should be semantic application palettes rather than hardcoded colours scattered through UI code.

### AMOLED Option

Optional:

```text
Pure black background
```

Only enabled when dark mode is active.

### Layout Density

```text
Comfortable
Compact
```

Compact mode should reduce vertical padding in:

- History rows
- Vehicle cards
- Settings rows

Do not globally scale text to accomplish this.

---

## 14. Design Direction

The interface should feel like a combination of:

- A modern vehicle instrument cluster
- A physical log book
- A clean Android utility

Avoid:

- Excessive gradients
- Glassmorphism
- Fake carbon fibre
- Fake gauges everywhere
- Automotive clichés
- Excessive animations

Use a restrained design.

The odometer value itself should visually dominate the dashboard.

Example:

```text
Golf
2017 Volkswagen Golf

       124,521
          km

+ Add Reading

Since last reading
327 km

This month
842 km
```

The number should use tabular figures if available.

---

## 15. Main Navigation

Use bottom navigation with four destinations:

```text
Home
History
Vehicles
Settings
```

Suggested icons:

```text
Home       → dashboard / speed
History    → history
Vehicles   → directions_car
Settings   → settings
```

Do not add a dedicated Statistics destination initially.

Statistics should be accessible from Home and/or vehicle details.

---

## 16. Navigation Graph

Conceptually:

```text
Root
│
├── onboarding
│
└── main
    │
    ├── home
    │   ├── add-reading/{vehicleId}
    │   └── statistics/{vehicleId}
    │
    ├── history
    │   └── reading/{readingId}/edit
    │
    ├── vehicles
    │   ├── vehicle/add
    │   ├── vehicle/{vehicleId}
    │   └── vehicle/{vehicleId}/edit
    │
    └── settings
        ├── appearance
        ├── data-management
        └── about
```

Pass IDs through navigation.

Do not pass complete `Vehicle` or `OdometerReading` objects between destinations.

---

## 17. First Launch

On first launch:

### Screen 1

```text
RoadJournal

Keep a simple history of your vehicle's mileage.
```

Button:

```text
Get Started
```

### Screen 2

Create first vehicle.

Required:

```text
Vehicle name
Unit
Current odometer
```

Optional expandable fields:

```text
Make
Model
Year
```

Action:

```text
Create Vehicle
```

Creating the first vehicle should also create its first odometer reading.

Afterward navigate directly to Home.

Do not build a long onboarding carousel.

---

## 18. Home / Dashboard

The dashboard is the most important screen.

It displays the currently selected vehicle.

### Top Section

Vehicle selector.

Example:

```text
Golf ▼
2017 Volkswagen Golf
```

Tapping the selector opens a sheet containing all active vehicles.

### Main Odometer Card

Display:

```text
124,521
km
```

Below:

```text
Updated Sep 21
```

Primary action:

```text
Add Reading
```

This should be the strongest CTA in the application.

### Quick Statistics

Show approximately three:

```text
Since last reading
327 km

Last 30 days
1,182 km

Total tracked
14,251 km
```

Do not overwhelm the home screen with analytics.

### Recent Activity

Show latest 3–5 readings:

```text
Sep 21       124,521 km       +327
Sep 12       124,194 km       +411
Sep 02       123,783 km       +296
```

Button:

```text
View History
```

---

## 19. Add Reading

This should be extremely fast.

Screen:

```text
Add Reading

Golf

Previous
124,521 km

New reading
[          ]

Date
Today

Note
Optional

Save Reading
```

### Input Behavior

- Numeric keyboard
- Automatically focus reading field
- Large input
- Display unit next to input
- Format thousands separators visually
- Date defaults to current date/time

The previous reading should remain visible while entering the new value.

After entering a value, optionally show:

```text
+327 km since Sep 21
```

before submission.

### Successful Submission

Save to Room.

Navigate back.

Dashboard updates reactively.

Show short snackbar:

```text
Reading added
```

---

## 20. Reading History

Display readings newest first.

Example:

```text
September 2026

Sep 21
124,521 km
+327 km

Sep 12
124,194 km
+411 km

Sep 02
123,783 km
+296 km
```

Group readings by month/year.

Each entry should display:

- Date
- Odometer value
- Difference from previous reading
- Note indicator if applicable

Tap an entry to edit.

---

## 21. History Filtering

Initial version:

Vehicle filter only.

If History is entered from the main navigation, use currently selected vehicle.

Add vehicle selector to the top app bar.

Future-compatible filters may include:

- Date range
- Year
- Notes
- Mileage range

Do not implement those initially unless trivial.

---

## 22. Edit Reading

Allow changing:

- Odometer
- Date/time
- Note

Revalidate against surrounding readings.

Actions:

```text
Save Changes
Delete Reading
```

Deletion requires confirmation by default.

Example:

```text
Delete this reading?

124,521 km
September 21, 2026

This cannot be undone.
```

The setting `confirmReadingDeletion` can eventually control this behaviour, but confirmation should default to enabled.

---

## 23. Vehicles Screen

List all active vehicles.

Example:

```text
Vehicles

Golf
2017 Volkswagen Golf
124,521 km

Civic
2012 Honda Civic
207,832 km

+ Add Vehicle
```

Each card should show:

- Vehicle name
- Optional year/make/model
- Latest reading
- Unit
- Accent indicator

The selected/current vehicle should be visibly indicated.

---

## 24. Vehicle Details

Display:

```text
Golf

2017 Volkswagen Golf
Ontario • ABCD 123

Current
124,521 km

Tracked
14,251 km

First reading
110,270 km
Mar 14, 2025

Entries
47
```

Actions:

```text
Add Reading
View History
Statistics
Edit Vehicle
```

---

## 25. Edit Vehicle

Editable fields:

```text
Name
Make
Model
Year
License Plate
Unit
Colour
Notes
```

Changing unit should **not automatically convert existing readings**.

For V1, if a vehicle already contains readings, prevent changing its odometer unit or show a strong confirmation explaining that existing values will not be converted.

The safer V1 behaviour is to disable unit changes after the initial reading.

---

## 26. Archive vs Delete

Support archiving.

Archive removes a vehicle from normal selectors without destroying its history.

Vehicle detail menu:

```text
Archive Vehicle
Delete Vehicle
```

Deleting should require explicit confirmation.

Example:

```text
Delete Golf?

This will permanently delete the vehicle and all 47 odometer readings.
```

Make the destructive consequence clear.

---

## 27. Statistics

Keep statistics simple and deterministic.

Initial metrics:

### Current Odometer

```text
124,521 km
```

### Distance Tracked

```text
latest - earliest
```

### Number of Readings

```text
count(readings)
```

### Distance in Last 30 Days

Derive from the closest appropriate historical readings.

### Distance This Calendar Year

```text
latest value this year - earliest value this year
```

### Average per Month

Use tracked time range rather than assuming twelve months.

---

## 28. Mileage History Chart

Provide one simple line chart.

Axes:

```text
X → Date
Y → Odometer
```

Do not over-engineer chart interactions initially.

Basic support:

- Smooth horizontal presentation
- Correct chronological ordering
- Respect light/dark theme
- Vehicle accent colour

If adding a chart dependency significantly complicates the first pass, isolate chart rendering behind:

```kotlin
MileageHistoryChart(...)
```

so the initial implementation can use a simple custom Compose Canvas implementation or placeholder.

---

## 29. Settings Screen

Suggested structure:

```text
Settings

Appearance
  Theme
  Accent colour
  Dynamic colours
  Pure black
  Layout density

Behaviour
  Confirm before deleting readings

Data
  Export data
  Import data

About
  RoadJournal
  Version
```

Do not create settings merely for the sake of customization.

Every setting should have an observable effect.

---

## 30. Data Export

Data ownership is important for a local-only utility application.

Support exporting all application data.

Primary backup format:

```text
JSON
```

Example conceptual structure:

```json
{
  "version": 1,
  "exportedAt": "...",
  "vehicles": [
    {
      "vehicle": {},
      "readings": []
    }
  ]
}
```

JSON should preserve:

- Vehicle IDs or migration-safe identifiers
- Vehicle metadata
- Every reading
- Dates
- Units
- Notes

Use Android's Storage Access Framework.

Do not request broad file-system permissions.

---

## 31. CSV Export

Optional but desirable for first release.

Allow exporting an individual vehicle's history as:

```csv
date,odometer,unit,difference,note
2026-09-21,124521,km,327,""
2026-09-12,124194,km,411,"Oil checked"
```

JSON = backup/restore.

CSV = human-readable/export to spreadsheets.

Keep those concepts separate.

---

## 32. Import / Restore

Support restoring RoadJournal JSON exports.

Before committing imported data:

1. Parse file
2. Validate backup version
3. Validate vehicle records
4. Validate reading relationships
5. Show summary

Example:

```text
Import RoadJournal backup?

3 vehicles
147 readings
Exported September 21, 2026
```

Then:

```text
Import
Cancel
```

For first implementation, restoring may replace all local application data rather than merging.

Clearly tell the user this before import.

---

## 33. Application State

Each screen should expose one immutable UI state object.

Example:

```kotlin
data class DashboardUiState(
    val isLoading: Boolean = true,
    val vehicle: Vehicle? = null,
    val latestReading: OdometerReading? = null,
    val recentReadings: List<OdometerReading> = emptyList(),
    val stats: MileageStats? = null,
    val availableVehicles: List<Vehicle> = emptyList(),
    val error: String? = null
)
```

ViewModel:

```kotlin
val uiState: StateFlow<DashboardUiState>
```

Compose:

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
```

Avoid:

```text
mutableStateOf scattered throughout ViewModel
```

for persisted/domain state.

---

## 34. UI Events

Prefer explicit events.

Example:

```kotlin
sealed interface AddReadingEvent {
    data class ReadingChanged(val value: String) : AddReadingEvent
    data class DateChanged(val value: Instant) : AddReadingEvent
    data class NoteChanged(val value: String) : AddReadingEvent
    data object Save : AddReadingEvent
}
```

The exact event abstraction does not need to be used mechanically everywhere.

For very simple screens, normal ViewModel functions are fine.

Avoid architecture ceremony.

---

## 35. One-Time UI Effects

Navigation, snackbar messages, etc. should not be persisted as normal UI state.

Use an appropriate event mechanism such as a Channel/Flow for transient effects.

Example:

```kotlin
sealed interface AddReadingEffect {
    data object NavigateBack : AddReadingEffect
    data class ShowSnackbar(val message: String) : AddReadingEffect
}
```

Do not use `SingleLiveEvent`.

---

## 36. Date and Time

Store timestamps as UTC epoch values / `Instant`.

Convert to the user's local timezone for presentation.

Use modern Java/Kotlin time APIs.

Do not store formatted date strings in Room.

---

## 37. Number Formatting

Stored:

```text
124521
```

Displayed:

```text
124,521
```

Respect locale-specific grouping where appropriate.

The database value must never contain formatting characters.

---

## 38. Empty States

Every list screen needs a useful empty state.

Example history:

```text
No readings yet

Add your first odometer reading to start building your vehicle history.

Add Reading
```

Vehicles:

```text
No vehicles

Add a vehicle to start tracking mileage.

Add Vehicle
```

---

## 39. Loading States

Because data is local, prolonged loading screens should be rare.

Avoid showing a giant spinner on every navigation transition.

Prefer:

- Brief skeleton/placeholder where necessary
- Immediate layout
- Reactive Room streams

---

## 40. Error Handling

User-facing errors should be understandable.

Bad:

```text
SQLiteConstraintException
```

Good:

```text
That reading is lower than the previous odometer reading.
```

Unexpected persistence errors can produce:

```text
Couldn't save the reading. Try again.
```

Log technical detail separately.

---

## 41. Accessibility

All interactive elements require:

- Meaningful accessibility labels
- Adequate touch targets
- Correct semantic roles
- Sufficient contrast

Do not communicate selected states using colour alone.

Support system font scaling reasonably.

The giant dashboard odometer must not break layouts at increased font scales.

---

## 42. Responsive Layout

Phone portrait is the primary target.

The layout should still behave correctly on:

- Landscape phones
- Foldables
- Tablets

Do not design a dedicated tablet UI in V1.

Use sensible max-width containers on large displays rather than stretching content edge-to-edge.

---

## 43. Permissions

V1 should require **no dangerous Android permissions**.

Do not request:

- Location
- Contacts
- Photos
- Storage
- Bluetooth
- Notifications

Use system file pickers for imports/exports.

This should be a privacy-friendly application.

---

## 44. Privacy

No:

- User accounts
- Analytics
- Advertising
- Tracking
- Location collection
- Remote database

All personal vehicle information stays on-device unless manually exported by the user.

---

## 45. Dependency Injection

Use Hilt.

Provide singletons for:

```text
RoadJournalDatabase
VehicleDao
OdometerReadingDao
VehicleRepository
OdometerRepository
SettingsRepository
DataStore
```

ViewModels should receive repositories via constructor injection.

Do not access dependency containers from composables.

---

## 46. Database Queries

Prefer reactive Room queries returning Flow.

Examples:

```kotlin
@Query("""
    SELECT * FROM vehicles
    WHERE archived_at IS NULL
    ORDER BY created_at ASC
""")
fun observeActiveVehicles(): Flow<List<VehicleEntity>>
```

```kotlin
@Query("""
    SELECT * FROM odometer_readings
    WHERE vehicle_id = :vehicleId
    ORDER BY recorded_at DESC
""")
fun observeReadings(
    vehicleId: Long
): Flow<List<OdometerReadingEntity>>
```

```kotlin
@Query("""
    SELECT * FROM odometer_readings
    WHERE vehicle_id = :vehicleId
    ORDER BY recorded_at DESC
    LIMIT 1
""")
fun observeLatestReading(
    vehicleId: Long
): Flow<OdometerReadingEntity?>
```

---

## 47. Transactions

Vehicle creation with an initial reading should be atomic.

Conceptually:

```text
BEGIN

create vehicle
create first reading

COMMIT
```

Likewise, destructive operations involving related records should maintain database consistency.

---

## 48. Domain Logic

Introduce small use cases where actual business rules exist.

Useful examples:

```text
AddOdometerReadingUseCase
EditOdometerReadingUseCase
CreateVehicleUseCase
CalculateMileageStatsUseCase
ImportBackupUseCase
```

Do NOT wrap every repository method in a pointless use-case class.

Business-rule-heavy operations deserve use cases.

Simple CRUD does not necessarily need one.

---

## 49. AddOdometerReadingUseCase

Responsibilities:

```text
1. Verify vehicle exists
2. Verify value >= 0
3. Find surrounding readings for timestamp
4. Validate chronological odometer sequence
5. Insert reading
```

This is better than putting validation inside a composable or DAO.

---

## 50. Selected Vehicle

Store:

```text
selectedVehicleId
```

in DataStore.

At startup:

1. Load selected vehicle ID.
2. Confirm vehicle still exists and is active.
3. If valid, use it.
4. Otherwise select first active vehicle.
5. If there are no vehicles, enter onboarding/create-vehicle flow.

When a vehicle is archived/deleted and it was selected, select another active vehicle.

---

## 51. Suggested Shared Compose Components

Create reusable components only where repetition exists.

Candidates:

```text
RoadJournalTopAppBar
OdometerDisplay
VehicleSelector
VehicleCard
ReadingRow
MileageStatCard
EmptyState
SettingsRow
SettingsSection
DestructiveConfirmationDialog
UnitLabel
```

---

## 52. OdometerDisplay

This is the signature UI component.

API could resemble:

```kotlin
@Composable
fun OdometerDisplay(
    value: Long,
    unit: DistanceUnit,
    modifier: Modifier = Modifier,
    emphasis: OdometerEmphasis = OdometerEmphasis.Large
)
```

It should:

- Use tabular digits if possible
- Properly format thousands
- Clearly distinguish value from unit
- Scale cleanly
- Work in all themes

---

## 53. Animations

Use subtle animations only.

Good:

- Crossfade when selected vehicle changes
- Animated number transition after adding a reading
- Expand/collapse optional form sections
- Bottom-sheet transitions
- Normal Material navigation motion

Avoid:

- Animated speedometer needles
- Constant glowing effects
- Excessive number counting every launch
- Splash-screen theatrics

The application is a tool.

---

## 54. Destructive Actions

Never put destructive actions next to primary actions without visual separation.

Use Material destructive/error styling.

Require confirmation for:

```text
Delete vehicle
Delete reading
Replace database from backup
```

---

## 55. Testing Strategy

### Unit Tests

Prioritize business rules.

#### Reading Validation

Test:

```text
first reading accepted
higher reading accepted
equal reading accepted
lower reading rejected
negative reading rejected
reading inserted chronologically
edited reading respects previous boundary
edited reading respects next boundary
```

#### Statistics

Test:

```text
distance between two readings
distance across many readings
single-reading vehicle
empty history
calendar-year calculation
30-day calculation
average monthly mileage
```

#### Vehicle Selection

Test:

```text
selected vehicle restored
missing vehicle falls back
archived selected vehicle falls back
no vehicles triggers empty state
```

---

## 56. Repository Tests

Use an in-memory Room database.

Test:

- CRUD
- Cascading vehicle deletion
- Ordering
- Flow updates
- Latest-reading queries
- Transactions

---

## 57. Compose UI Tests

At minimum:

```text
onboarding → create vehicle
dashboard displays first reading
add reading updates dashboard
history displays new reading
switch vehicle
change dark/light theme
archive vehicle
```

---

## 58. Preview Support

Important Compose components should have previews.

Provide representative fake models.

Include:

- Light
- Dark
- Large font where useful
- Empty state
- Populated state

Do not require Hilt or database objects to render previews.

---

## 59. Build Configuration

Recommended baseline:

```text
minSdk: 26
compileSdk: latest stable installed
targetSdk: latest stable installed
```

Use Gradle version catalogs:

```text
gradle/libs.versions.toml
```

Keep dependencies current and stable.

Avoid alpha/beta libraries unless they solve a specific requirement.

---

## 60. Git / Repository Structure

Repository:

```text
roadjournal/
```

Suggested top-level files:

```text
roadjournal/
├── app/
├── gradle/
├── .github/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── README.md
├── LICENSE
└── docs/
```

Optional:

```text
docs/
├── architecture.md
├── data-model.md
└── screenshots/
```

---

## 61. README

Initial README should include:

```text
# RoadJournal

A simple, private odometer history app for Android.

## Features

- Track multiple vehicles
- Record odometer history
- View mileage statistics
- Customize appearance
- Export and restore your data
- Fully offline

## Tech

Kotlin
Jetpack Compose
Room
DataStore
Hilt
```

Do not exaggerate features that are not implemented yet.

---

## 62. V1 Scope

The first usable release should contain:

### Core

- Create vehicle
- Edit vehicle
- Archive vehicle
- Delete vehicle
- Multiple vehicles
- Select current vehicle

### Readings

- Add reading
- Edit reading
- Delete reading
- Date/time
- Optional note
- Validation
- Historical list

### Dashboard

- Current reading
- Selected vehicle
- Quick mileage statistics
- Recent readings
- Add-reading CTA

### Statistics

- Total tracked
- Last 30 days
- Current year
- Reading count
- Simple historical chart

### Appearance

- Light/dark/system
- Material You dynamic colour
- Accent presets
- AMOLED mode
- Comfortable/compact density

### Data

- JSON backup/export
- JSON restore
- CSV export

### Quality

- Empty states
- Error handling
- Accessibility basics
- Unit tests
- Basic Compose UI tests

---

## 63. Explicitly Out of Scope for V1

Do not implement:

- Login
- Cloud accounts
- Remote backend
- Automatic trip tracking
- GPS
- Fuel logging
- Fuel economy
- Maintenance tracking
- Service reminders
- Expense tracking
- Insurance information
- VIN lookup
- CarPlay / Android Auto
- Home-screen widget
- Wear OS
- Notifications
- OCR
- Automatic odometer recognition
- Image attachments
- Sharing vehicles
- Fleet management

The architecture should allow future features, but V1 should remain an **odometer application**.

---

## 64. Potential V2 Features

Possible later additions:

```text
Maintenance records
Fuel fill-ups
Mileage-based maintenance reminders
Vehicle photos
Home-screen widget
Quick-add notification action
Automatic backups
Optional cloud sync
Mileage goals
Annual comparison
Service history
Odometer replacement / rollover support
Custom dashboard widgets
```

These should not influence the first implementation enough to make V1 complicated.

---

## 65. Important UX Principle

Optimize for this interaction:

```text
Open RoadJournal
↓
Tap Add Reading
↓
Enter 124842
↓
Tap Save
↓
Done
```

A routine odometer update should take only a few seconds.

Everything else is secondary.

---

## 66. Suggested First-Pass Implementation Order

### Phase 1 — Foundation

Create:

- Android project
- Compose
- Material 3
- Hilt
- Navigation
- Room
- DataStore
- Theme system
- Base navigation shell

### Phase 2 — Data Model

Implement:

- Vehicle entity/DAO/repository
- Odometer entity/DAO/repository
- Domain models
- Mapping
- Validation
- Initial migrations framework

Add tests before UI becomes dependent on the data model.

### Phase 3 — Vehicle Flow

Implement:

```text
First launch
Create vehicle
Vehicle list
Vehicle details
Edit vehicle
Selected vehicle
```

### Phase 4 — Odometer Flow

Implement:

```text
Add reading
History
Edit reading
Delete reading
Validation
```

At this point the application should already be practically usable.

### Phase 5 — Dashboard

Implement:

```text
Current vehicle
Current odometer
Quick stats
Recent readings
Vehicle selector
```

### Phase 6 — Appearance

Implement:

```text
System/light/dark
Dynamic colour
Accent colours
AMOLED
Density
```

### Phase 7 — Statistics

Implement:

```text
Tracked mileage
30-day mileage
Year mileage
Reading count
Chart
```

### Phase 8 — Data Portability

Implement:

```text
JSON export
JSON restore
CSV export
```

### Phase 9 — Polish

Add:

- Accessibility
- Animations
- Empty states
- Error states
- Confirmation dialogs
- Better previews
- UI tests
- README

---

## 67. Definition of Done for First Pass

The first-pass implementation is successful when this sequence works reliably:

```text
Install app
↓
Create "Golf"
↓
Enter 120000 km
↓
See Golf dashboard
↓
Add 120350 km
↓
Dashboard updates to 120,350 km
↓
History shows both entries
↓
App reports +350 km
↓
Create second vehicle
↓
Switch between vehicles
↓
Each maintains independent history
↓
Close application
↓
Reopen application
↓
All data and selected vehicle persist
↓
Change theme
↓
Theme persists after restart
↓
Export backup
↓
Delete/reinstall or clear data
↓
Restore backup
↓
Vehicles/readings are restored correctly
```

---

## 68. Implementation Constraints for Coding Agent

When generating the implementation:

1. Use idiomatic Kotlin.
2. Use Jetpack Compose only; do not introduce XML layouts.
3. Use Material 3.
4. Use immutable UI state.
5. Use StateFlow for observable ViewModel state.
6. Use lifecycle-aware state collection in Compose.
7. Do not access Room directly from composables.
8. Do not access DataStore directly from composables.
9. Do not perform database work on the main thread.
10. Do not create a backend.
11. Do not add permissions that are unnecessary.
12. Do not add Firebase.
13. Do not add analytics.
14. Do not add authentication.
15. Keep abstractions proportional to application complexity.
16. Favor readable code over clever code.
17. Add comments only when they explain reasoning rather than restating code.
18. Ensure all implemented features have working navigation.
19. Provide previews for reusable UI components.
20. Build and run tests before considering the implementation complete.

---

## 69. First-Pass Agent Objective

Implement a functional Android application called **RoadJournal** based on this specification.

Prioritize:

1. Correct persistent data model
2. Very fast odometer entry
3. Reliable multi-vehicle handling
4. Clear historical records
5. Clean Material 3 interface
6. Theme customization
7. Maintainable Kotlin architecture

Do not prematurely expand the product beyond odometer tracking.

The application should feel polished enough to use every day while remaining small enough that its architecture is immediately understandable to another Android developer.
