# TripRabbit

A simple, private odometer history app for Android.

## Implemented features

- Create, edit, archive, delete, and switch between vehicles
- Store independent odometer histories for multiple vehicles
- Add, edit, and delete timestamped readings with chronological validation
- Persist vehicles and readings with Room and the current selection with DataStore
- Dashboard mileage summaries, recent activity, and a history chart
- Detailed tracked, 30-day, annual, and monthly statistics
- System/light/dark themes, dynamic colour, accent palettes, AMOLED mode, and density controls
- JSON backup/restore and per-vehicle CSV export through system file pickers

## Tech

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Room, DataStore, Hilt, Coroutines, Flow, and KSP.

TripRabbit is fully local and requests no dangerous Android permissions.

## Build and verify

The project uses a Gradle Java 17 toolchain and can provision a compatible JDK automatically.

```sh
./gradlew testDebugUnitTest connectedDebugAndroidTest assembleDebug
./gradlew lintDebug
```

Connected tests require an Android device or emulator. The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

## Data and privacy

TripRabbit has no accounts, analytics, advertising, network backend, or automatic cloud backup. Vehicle data remains on the device unless the user explicitly exports a JSON backup or CSV file through Android's system file picker. Restoring a backup validates its records, presents a confirmation summary, and then atomically replaces the current local database.

Release-preparation materials live in [`docs/`](docs/): the privacy policy,
Google Play submission answers and gates, brand-clearance review, and the U.S.
export compliance review.
