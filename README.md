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

## Build a Play Store bundle

Google Play requires an Android App Bundle (`.aab`) for a new app. Copy
`keystore.properties.example` to the ignored `keystore.properties` file and fill
in the upload-keystore path and credentials. Keep the keystore and both passwords
backed up outside this repository; keep using that upload key unless Google Play
has explicitly completed an upload-key reset.

Build and validate the signed release bundle with:

```sh
./gradlew check bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

The upload artifact is written to
`app/build/outputs/bundle/release/app-release.aab`. In Android Studio, use
**Build > Generate Signed Bundle or APK**, select **Android App Bundle** (not
APK), select the `release` variant, and use the same upload keystore.

The `.aab`, `keystore.properties`, and common keystore file extensions are
ignored by Git. Never commit or send the upload key or its passwords.

## Data and privacy

TripRabbit has no accounts, analytics, advertising, network backend, or automatic cloud backup. Vehicle data remains on the device unless the user explicitly exports a JSON backup or CSV file through Android's system file picker. Restoring a backup validates its records, presents a confirmation summary, and then atomically replaces the current local database.

Release-preparation materials live in [`docs/`](docs/): the privacy policy,
Google Play submission answers and gates, brand-clearance review, and the U.S.
export compliance review.
