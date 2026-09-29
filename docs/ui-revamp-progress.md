# UI revamp review

## Current state — September 28, 2026

This document records the UI refinement work and its historical review checkpoints.
Some checkpoint details describe intermediate UI states. Current behavior is defined
by the application source and README. The latest updates include per-vehicle in-progress
trip handling, vehicle-specific and all-vehicle reports with a shared km/mi selector,
trip filters without a This Month option, and updated Garage and Settings controls.

The changes have been organized into local Git commits for review. No remote push was
performed.

## Historical review checkpoints

## Design changes

- Ink and mint light/dark palettes, stronger typography and layered surfaces.
- Rounded bottom navigation retains Trips, Reports, Add, Garage and Settings in the same order.
- Trips: prominent distance summary, readable trip cards with distance and note previews, existing search, filters, vehicle selection and detail/edit actions.
- Reports: matching summary, metric tiles, charts and the existing PDF/CSV date-range export controls.
- Garage: redesigned comfortable and compact cards, current-vehicle status and existing selection/detail actions.
- Add/edit trip: odometer summary controls expand into numeric editors with increment/decrement, Apply and Cancel. Saving is disabled while an editor has unapplied values.
- Settings: light/dark preview tiles, selectable preferences and grouped data-management actions. Accent, density, deletion preference, backup/restore, privacy and debug sample-data actions remain available.
- Welcome/dashboard retain the TripRabbit name, rabbit mark and existing artwork. Shared headers, cards, inputs, metrics, empty states and confirmations carry the design through vehicle forms, detail screens and privacy.

## Responsive behavior

Phones request portrait orientation. Layouts also accommodate landscape when the OS allows it. All routes have a maximum content width; scrollable forms keep save actions reachable. Headings and paired content stack on narrow screens or with large fonts. Unit choices wrap, long vehicle names wrap, and trip-detail odometer values stack. Bottom-navigation labels cap scaling at 120% to keep all five destinations visible; screen content follows system text scaling.

## Verification

- `./gradlew :app:testDebugUnitTest --offline --max-workers=1`: 63 tests passed, zero failures/errors/skips.
- `./gradlew :app:assembleDebug --offline`: debug APK built at `app/build/outputs/apk/debug/app-debug.apk`.
- `./gradlew :app:lintDebug --offline --max-workers=1`: zero errors; two expected warnings for the requested portrait orientation restriction.
- `git diff --check`: clean.
- Reviewed generated native Android renders in `app/build/reports/ui/`, covering light/dark Trips, Reports, Settings, welcome, dashboard, vehicle creation/details, comfortable/compact Garage, add/edit trip, privacy and dialogs. Additional cases cover 320dp width, 200% text, 800x360 landscape, and 840dp tablet reports.
- Real-navigation tests use Hilt, Room and DataStore to exercise vehicle creation, trip save/edit, reports, Garage/details, theme persistence, privacy and archiving the only vehicle. UI tests cover search/filtering, date validation, numeric entry, confirmation preferences, accent application/contrast, navigation and responsive action visibility.
- Corrected the existing add-trip save bug: use the combined UI state so the displayed previous odometer is available as the default starting value.
- Updated stale test expectations to the current Trips flow, schema version 3, CSV start-odometer column and existing distance attribution by reading date. Export and statistics production logic were retained.

Lint intermittently crashed in Kotlin analysis when run alongside compilation; its separate single-worker run passed. Verification used the local Android runtime, not installation onto the connected personal device. The APK is ready for hands-on review.

## Requirement audit

| Requirement | Evidence |
| --- | --- |
| Full component revamp | Screen/component diffs and reviewed renders listed above |
| Preserve TripRabbit name and imagery | Existing drawable resources retained; BrandHeader/BrandArtwork used in welcome/dashboard |
| Preserve navigation and functionality | Same route callbacks and bottom destinations; workflow and UI tests pass |
| Preserve visibility and utility | Responsive stacking/wrapping, scrollable forms, notes and status information retained; large-text captures reviewed |
| Different screen sizes and landscape | Phone portrait request plus narrow, large-text, landscape and tablet rendering tests |
| No commits/pushes | All changes remain in the local worktree |

## September 28 — shared styling and Trips refinement

The initial pass still devoted too much space to decorative cards and introductory copy. The next pass starts with the shared styling and Trips, preserving navigation destinations, their order, filtering, search, vehicle selection, trip data and detail/edit actions.

- Reduced shared corner radii and removed decorative heading bars.
- Flattened navigation elevation and tightened navigation shapes.
- Removed the Trips tagline and replaced the large accent summary with a compact distance/count summary and divider.
- Removed repeated trip icon badges and tightened record spacing while retaining names, dates, distances and note previews.
- Updated the normal Trips render fixture to include the vehicle selector, matching production state.
- Debug build and the existing unit/UI workflow suite passed after the UI changes; reviewed normal and 200% text Trips renders.

This is a review checkpoint for Trips and the general styling. Reports, Garage, forms and Settings still need an individual refinement pass against the utilitarian direction; the initial app-wide work above is not proof that the full objective is complete.

## September 28 — Reports refinement

- Replaced the oversized accent summary and icon tiles with compact labelled totals, retaining total distance, reading count, last 30 days and monthly average with units.
- Flattened the trend and weekly chart sections into divider-separated content. Year-to-date distance, date endpoints, weekly attribution and all chart data remain present in their original order.
- Simplified the export section, preserving both date fields, pickers, validation, busy states and PDF/CSV actions in their original order.
- Updated the workflow assertion for the sentence-case heading and scoped the total-distance check now that all metrics include units.
- Added a 320dp/200% text render that exercises both export callbacks.

Reports joins Trips as a refined screen. Garage, forms and Settings remain for individual refinement; the overall goal remains active.

Reports verification: 64 tests passed with no failures/errors/skips; debug APK rebuilt; diff whitespace check clean. Reviewed light/dark, export and 320dp/200% text renders. Verification used the local Android runtime.

## September 28 — Garage refinement

- Reduced comfortable-card padding and odometer typography, removed repeated car badges, and retained names, vehicle descriptions, current status, odometers, details and selection actions.
- Kept compact mode distinct with short records and its existing whole-row details action; removed the decorative badge.
- Kept vehicle order, add-vehicle action and selection callbacks unchanged. Vehicles without readings now explicitly show “No reading” in comfortable mode, matching compact mode.
- Included bottom navigation in standard Garage render fixtures and added dark-mode coverage exercising add, details and selection callbacks.

Vehicle details, forms, Settings and the remaining welcome/dashboard surfaces still need their individual refinement pass. The goal remains active.

Garage verification: 65 tests passed with no failures/errors/skips; debug APK rebuilt; diff whitespace check clean. Reviewed comfortable and compact layouts, dark mode, and 320dp large-text renders.

## September 28 — vehicle details and forms

- Vehicle creation/editing now uses full-width fields with one divider between essentials and optional details. Removed duplicate page headings and introductory filler; retained field order, unit guidance and edit-mode unit restrictions.
- Vehicle details retains the odometer, Add Trip, statistics, navigation actions, plate/notes and management actions in order, with dividers replacing outer cards and decorative metric icons removed.
- Trip add/edit forms lose duplicate headings and outer cards. Vehicle context, previous odometer, numeric editing with Apply/Cancel, date/time, name, notes, save and deletion flows remain present in order.
- Reduced odometer control padding and removed repeated gauge badges while retaining edit affordances.

Settings and welcome/dashboard still require individual refinement. The goal remains active.

Forms/details verification: 65 tests pass, debug APK rebuilt, diff whitespace clean. Reviewed create-vehicle, vehicle-details, add/edit-trip and large-text add-trip renders. Existing workflow tests cover creation, saving/editing trips and vehicle archive confirmation.

## September 28 — Settings and privacy

- Replaced outer Settings cards with divider-separated groups, removed the introductory tagline and redundant group captions, and retained preference/action order.
- Reduced theme previews and removed decorative icons from selection rows. System/light/dark choices, accent editing, comfortable/compact density, deletion confirmation, backup/restore, debug sample data and privacy remain available.
- Privacy retains all policy wording and its order, with paragraphs replacing individual section cards.

Welcome/dashboard refinement and the final app-wide audit remain. The goal remains active.

Settings verification: 65 tests pass, debug APK rebuilt and diff whitespace clean. Reviewed light/dark Settings, 200% text, data actions and privacy renders. Theme persistence and accent editing remain covered by existing workflow/UI tests.

## September 28 — final refinement and audit

Welcome now has one concise introduction and its existing Get Started action, retaining the rabbit identity and artwork. Dashboard uses a plain odometer summary, compact metrics and divider-separated weekly/recent content, retaining vehicle selection and all actions in their existing order. README navigation/filter descriptions now match the app.

### Completion evidence

| Requirement | Current evidence |
| --- | --- |
| General theme and styling | Shared theme, typography, corner shapes, headers, navigation and controls reviewed in regenerated light/dark renders |
| Each screen refined | Trips, Reports, Garage, vehicle details, create/edit vehicle, add/edit trip, Settings, privacy, welcome and dashboard changes inspected; screen renders reviewed during their respective passes |
| Less unnecessary text and sections | Duplicate form headings removed; summary cards reduced; preference/chart/detail groups separated with dividers; welcome feature copy consolidated |
| Navigation functions and order preserved | AppNavigation destinations remain Trips, Reports, Add, Garage, Settings; MainActivity diff only changes layout sizing; workflow tests exercise real navigation |
| Trips information retained | Search/filter logic and detail content retained; names, dates, distances and notes shown; current vehicle in selector and detail dialog; tests cover search, filters and detail editing |
| Other data and flows retained | Report calculations/export handlers, Settings data handlers, vehicle editor fields/callbacks and archive/delete confirmations inspected against the baseline; data repositories and calculation production sources unchanged |
| Usability retained | Existing 320dp/large-text, landscape and tablet cases pass; rendered layouts reviewed; scrolling keeps form/export actions reachable; 65 tests pass with zero failures/errors/skips |
| Build and static checks | Debug APK rebuilt; lint completes with zero errors and two warnings for the existing portrait restriction; git diff whitespace check clean |

One functional repair remains included from the initial pass: AddReadingViewModel saves from combined UI state so the displayed previous odometer is available as the default start. The real create/log/edit workflow exercises this path. Odometer entry uses a full numeric field with Apply/Cancel and increment/decrement controls; saving is disabled while values are unapplied.

Verification used the local Android runtime and generated native renders, not installation on a personal device. The completed implementation is ready for hands-on review; subjective visual preference can be refined from feedback.

## September 28 — requested finishing changes

1. Trip rows now contain title/date on the left and distance on the right with space before the chevron. Notes remain in details, not rows. Reviewed normal and 200% text renders.
2. Trip details uses a dedicated header row to keep X at top right, plus a full-width Close text action below Edit/Finish. Both dismissal actions are exercised by a UI test.
3. Numeric editors initialize with the actual value or default. Changing the start in Add Trip resets the finish to that start. Each digit has independent +/− controls; direct numeric typing and Apply/Cancel remain available. Tests verify prefilled values and digit changes.
4. Start trip saves the start, date, name and notes as an in-progress record. Trips labels it In progress and opens a Finish trip action. Finishing updates the same record. Room schema 4 migrates older records to completed status; backup/restore preserves pending status. Pending trips are excluded from report exports, mileage statistics and completed-reading charts/defaults. A real workflow test starts after personal mileage, recreates the activity and finishes with the correct trip-only distance. Backup, migration and statistics assertions pass.
5. Removed the Trips distance summary; Reports and Garage totals remain.

Final verification: 69 tests pass with zero failures/errors/skips; debug APK built; lint passes with the two existing portrait warnings; diff whitespace check clean. Reviewed compact rows, odometer controls, both modal close controls and in-progress list/detail renders. All five requested items are implemented. No device installation, commits or pushes performed.
