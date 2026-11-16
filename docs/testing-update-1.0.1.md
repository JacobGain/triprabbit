# TripRabbit 1.0.1 testing update

## Release notes

- Preview the actual PDF using the selected vehicles, date range, and distance unit, directly below Export PDF.
- A fictional sample using the same PDF renderer remains available before adding a vehicle.
- Save or share the sample without adding fictional trips to your records.
- Choose a vehicle colour and icon (Car, Pickup truck, Van, Motorcycle, or SUV) and recognise it by its badge in Trips, Garage, and vehicle selectors.
- Trips filter chips display vehicle names without icons.
- Debug sample vehicles include matching colours and icons.
- Send feedback from Settings using your email app.
- PDF reports now include a summary beneath each month.

Version code: 4. Database version 5 adds a vehicle icon field; existing vehicles default to Car, and older backups remain supported. Blank or unrecognised legacy colour labels display a neutral badge until another colour is selected.

## Tester tasks

1. On a fresh install, choose Explore reports, then View sample PDF. With existing data, open Reports, choose vehicles and dates, then Preview PDF. Is the report useful and understandable? Save or share it and check it opens.
2. Create or edit two vehicles, give them different colours and icons, and record trips. Can you quickly tell which vehicle each trip belongs to?
3. Restart the app and verify the colours remain. Export and restore a backup and check colours and trips.
4. Select vehicles, dates, and units, then Preview PDF. Check that saving from the preview produces the displayed report. Export a real PDF for one vehicle and all vehicles, with a date range. Check totals, vehicle labels, notes, and monthly summaries.
5. Check the same screens in light and dark themes, with large text.
6. Use Settings > Send feedback. Check the recipient and version; sending is optional.

Record the build, device, observed result, actual tester comments, and any resulting fixes. These changes were identified by the developer during review of the closed-testing period; do not attribute them to testers unless confirmed by real responses.

## Release checks

- Install over the previous release and verify existing vehicles/trips.
- Check the production sample preview before adding records (no debug data import).
- Test saving and sharing through Android's system pickers.
- Publish the updated privacy-policy HTML alongside the release.

## Local validation

- 86 JVM/Robolectric tests passed, including first-install report navigation, colour selection, and colour backup/restore.
- Release APK, signed Play app bundle, and device-test APK built successfully.
- Release lint and Play policy manifest verification passed. Lint retains warnings, including existing dependency and orientation notices.
- Reviewed rendered colour-picker and large-text trip screens.
- The native PDF-generation/opening test is in Android instrumentation tests because the local Robolectric runtime does not implement PdfDocument. It compiled but was not run: no emulator is configured, and the connected phone is not authorised for USB debugging. Verify PDF preview, zoom, save/share, feedback email drafting, and upgrade preservation on a device before upload.
