# Google Play submission guide

This file records answers for the current `com.jacobgain.triprabbit` release. Re-audit it
whenever code, dependencies, declarations, or the store listing changes. The
Play Console answers and the published privacy policy must always describe the
actual release artifact, not merely this document.

> The former RoadLog name was retired after the brand review found overlapping
> vehicle-software uses. The publisher selected TripRabbit after a preliminary
> collision screen. Preserve the decision record in `docs/brand-clearance.md`
> and complete professional clearance before commercial release if appropriate.

## Store listing copy

**App name (30-character limit):** TripRabbit: Odometer Log

**Short description (80-character limit):**

> Private, offline odometer history and mileage statistics for your vehicles.

**Full description:**

> Keep a clear odometer history for each of your vehicles with TripRabbit.
>
> Add timestamped readings, review recent activity, and see tracked, monthly,
> annual, and 30-day mileage statistics. Manage multiple vehicles independently
> and choose the theme and layout that suit you.
>
> Your data stays on your device. TripRabbit has no account, ads, analytics, or
> network service. You can create a JSON backup, restore a TripRabbit backup, or
> export the current vehicle's history as CSV using Android's system file
> picker.

Use screenshots captured from the submitted build. Do not add rankings,
awards, pricing claims, testimonials, unrelated keywords, or capabilities not
present in the app.

Recommended category: **Auto & Vehicles**.

## App content declarations

| Play Console item | Answer for this release | Evidence / note |
| --- | --- | --- |
| Privacy policy | Publish `docs/privacy-policy.html` at a stable public HTTPS URL; enter that URL | The same policy is available in Settings > Privacy policy |
| Ads | No | No ads SDK or ad UI |
| App access | All functionality is available without special access | No login, membership, region gate, or credentials |
| Target audience | Ages 18 and over | General vehicle utility intended for adults; listing must avoid child-directed imagery |
| Restrict minor access | No | App has no age-restricted content; the audience declaration describes design intent |
| Content rating | Complete the IARC questionnaire truthfully; expected to be the lowest general rating | No violence, sex, gambling, drugs, user interaction, or user-generated sharing |
| News and magazine | No | Odometer utility |
| Health apps | No | No health features or health data |
| Financial features | No | Mileage statistics are not financial services |
| Government apps | No | No government affiliation or function |
| COVID-19 | No | No tracing or status feature |
| Data safety | No data collected; no data shared | All app data remains on-device; user-initiated exports are excluded from collection |
| Account creation | No | No account exists, so the account-deletion requirement does not apply |
| Permissions declaration | None expected | Main manifest declares no permissions. AndroidX adds only an app-signature internal dynamic-receiver permission; confirm the merged release manifest before upload |
| Ads ID | No | No ads SDK and no `AD_ID` permission |

For the Data safety security/deletion follow-up questions, answer based on the
exact wording Play Console presents at submission time. Do not claim that data
is encrypted in transit: TripRabbit transmits no data. Explain that users can
delete individual local records, clear app storage, or uninstall the app.

## Reviewer notes

Use the following concise notes if Play Console provides an access-instructions
field:

> No login or special access is required. On first launch, tap Add Vehicle,
> enter a name and starting odometer reading, and save. All tabs and features
> then become available. Backup and CSV export use Android's system file picker.

## Release gates

Before every upload:

1. Build the signed release App Bundle and run unit, lint, and connected tests.
2. Confirm the selected name has completed the clearance decision recorded in
   `docs/brand-clearance.md` and that all listing/assets use the cleared name.
3. Run `./gradlew verifyPlayPolicyRelease`. This fails if the merged release
   manifest gains a platform permission, enables backup, loses the Android 12+
   extraction rules, or falls below API 36. Treat a failure as a mandatory
   policy/privacy review, not merely a build problem.
4. Inspect the merged release manifest and App Bundle for permissions and SDKs.
5. Confirm `targetSdk` still meets the current Play deadline. As of September
   22, 2026, phone submissions require API 36 or higher.
6. Confirm the public privacy-policy URL is live, HTTPS, globally accessible,
   non-editable by visitors, and not a PDF.
7. Check that the policy text, in-app text, Data safety answers, and current SDK
   behavior agree.
8. Verify every screenshot and every sentence in the listing against the exact
   submitted build.
9. Re-run the export review in `docs/export-compliance.md`.
10. Review Policy status and every item under Policy and programs > App content
   immediately before rollout; Google can add declarations or change policy.

## Local bundle workflow

The release signing configuration is loaded from the ignored root-level
`keystore.properties` file. Start from `keystore.properties.example` on a new
machine; do not copy credentials into Gradle files or commit the properties file
or keystore.

```sh
./gradlew check bundleRelease
jarsigner -verify app/build/outputs/bundle/release/app-release.aab
```

Upload `app/build/outputs/bundle/release/app-release.aab`, not an APK. New Play
apps use Play App Signing. This local key is the upload key; Google manages the
separate app-signing key used for APKs delivered to users. Preserve an offline
backup of the upload key and its credentials.

Creating an app record in Play Console reserves operational choices such as the
package name and signing setup. Do that only when the publisher is ready to use
the developer account; generating and testing the signed bundle locally does not
publish or upload anything.

## Privacy-policy hosting

`docs/privacy-policy.html` is ready to host but is not public merely because it
exists in this repository. One low-maintenance option is GitHub Pages configured
to publish from the repository's `docs/` directory. If this repository and Pages
site are public, the likely URL is
`https://jacobgain.github.io/triprabbit/privacy-policy.html`; verify the actual URL
instead of assuming it. Do not publish the branded URL until the name decision
and repository hosting path are final.

Before entering a URL in Play Console, open it in a signed-out/private browser
and from a second network. Confirm it returns the policy directly without a
login, redirect loop, geography restriction, download prompt, or edit controls.

## Current policy-risk review

- **User data:** low risk while data remains local. The privacy policy and Data
  safety form are still mandatory. A future network, analytics, crash-reporting,
  advertising, account, cloud-sync, or third-party SDK feature requires a fresh
  review before release.
- **Permissions and device abuse:** low risk. Keep the source manifest free of
  platform permissions unless one is essential to a disclosed core feature.
  AndroidX currently adds only an app-signature internal receiver permission.
- **Deceptive behavior and metadata:** use only the copy above and authentic
  screenshots. Keep offline/privacy claims synchronized with the binary.
- **Spam and minimum functionality:** the app has durable CRUD, multi-vehicle
  history, statistics, backup/restore, export, and customization. Test every
  advertised path; broken or placeholder UI creates avoidable review risk.
- **Restricted content, UGC, payments, ads, AI, and subscriptions:** not present.
  Any addition activates separate policy requirements and must block release
  until reviewed.
- **Families:** not applicable to the declared adult audience. Do not use child-
  directed art or marketing unless the app and all SDKs are intentionally made
  compliant with the Families policies first.

## Official references checked September 22, 2026

- Google Play Developer Program Policies: https://play.google/developer-content-policy/
- User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Prepare your app for review: https://support.google.com/googleplay/android-developer/answer/9859455
- Target audience: https://support.google.com/googleplay/android-developer/answer/9867159
- Target API requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Metadata policy: https://support.google.com/googleplay/android-developer/answer/9898842
