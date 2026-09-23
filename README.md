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

## UI

The Compose UI uses an evergreen and warm-neutral design system, temporary `TR`
branding pending commissioned artwork, and a consistent outline control icon
family. Onboarding, dashboard, history, reports, forms, vehicles, settings, and
privacy share typography, surfaces, controls, and light/dark palettes. Home /
History / Reports / Settings navigation keeps the main destinations close.

Vehicle management is available from Home and the vehicle selector. History
supports search and All Readings / This Month / With Notes filters; Reports
includes CSV export. Reading forms include date/time pickers. The branded palette
is the default, with system colours still available in Settings. Custom PNG
artwork can be added through the slots documented in [UI assets](docs/ui-assets.md).

The mockup's GPS trip tracking, business/personal classification, and tax
deductions are not implemented. Screens show actual local odometer data;
weekly bars attribute measured intervals to the day the ending reading was logged.

## Build and verify

The project uses a Gradle Java 17 toolchain and can provision a compatible JDK automatically.

```sh
./gradlew testDebugUnitTest connectedDebugAndroidTest assembleDebug
./gradlew lintDebug
```

Connected tests require an Android device or emulator. The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

Local UI rendering and interaction tests run with Robolectric (no device needed):

```sh
./gradlew testDebugUnitTest --tests 'com.jacobgain.triprabbit.ui.UiOverhaulTest'
```

These render the production screens in light/dark themes and at a large font size,
exercise search, filters, validation and deletion, and write review PNGs to
`app/build/reports/ui/`. The first run downloads the Android test runtime.

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
