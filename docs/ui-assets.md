# Artwork handoff

The app UI, colours, type, navigation, charts, and 24 dp outline control icons are implemented. The remaining artwork is isolated in the slots below. The app builds and works before any of it arrives. Its temporary `TR` monogram and text first panels are placeholders, not the intended identity.

| Priority | Asset to commission | Deliverable and placement |
| --- | --- | --- |
| 1 | App icon | A distinctive TripRabbit mark in a transparent, square foreground with no words or background. Supply a full colour version and a single colour silhouette for Android themed icons. Keep the subject inside the central safe area so adaptive launcher masks can crop it. Replace `app/src/main/res/drawable/ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml` with the final vector XML, or remove each XML and add a PNG of the same resource name under `res/drawable-nodpi/` (suggested 432 × 432 px). Both round and standard launchers already reference these resources. Change `launcher_background` in `res/values/colors.xml` if the final mark calls for another background colour. |
| 2 | In-app brand mark | A transparent square PNG (at least 512 × 512 px), legible at 44 dp. Put `brand_mark.png` in `app/src/main/res/drawable-nodpi/`, then set `BrandArtwork.mark = R.drawable.brand_mark` in `core/designsystem/component/BrandComponents.kt`. It appears in the header and welcome panel. The temporary `TR` letters disappear automatically. |
| 3 | Welcome artwork | A 2.4:1 PNG (suggested 1440 × 600 px) for the onboarding panel. Put `welcome_art.png` in `drawable-nodpi/` and set `BrandArtwork.welcome = R.drawable.welcome_art`. |
| 4 | Dashboard artwork | A 2.4:1 PNG (suggested 1440 × 600 px) for the home page after the mileage summary. Put `dashboard_art.png` in `drawable-nodpi/` and set `BrandArtwork.dashboard = R.drawable.dashboard_art`. |

The image slots centre crop inside rounded cards. Keep key details away from all edges and avoid embedded text. Native UI text stays readable, localised, and accessible. Check transparent artwork against both warm white and deep green surfaces, or supply a self-contained opaque composition. Until images are connected, the dashboard and welcome screens show complete text panels in those positions.

After copying the three in-app PNGs, connect them in `BrandComponents.kt`:

```kotlin
@DrawableRes val mark: Int? = R.drawable.brand_mark
@DrawableRes val welcome: Int? = R.drawable.welcome_art
@DrawableRes val dashboard: Int? = R.drawable.dashboard_art
```

The prepared `drawable-nodpi/` directory prevents Android from rescaling the source bitmaps. Keep the filenames lowercase with underscores and remove a same-named XML if replacing a vector resource.

The existing control icons (`ic_home`, `ic_history`, `ic_reports`, and the other `ic_*.xml` files) are finished functional UI symbols, not commissioned brand art. They share an outline style and are mapped in `AppIcon`. Replacing them is optional; if you do, deliver the full family as 24 × 24 vector drawables with consistent strokes and keep their resource names. No decorative car, road, rabbit, or landscape image is hidden in Compose code.

For a future store listing, commission a separate high resolution store icon, feature graphic, and screenshots based on the final UI. These are marketing exports and are not packaged in the app. [The original mockup](ui-reference.png) is a design reference only; it is not used at runtime and depicts GPS trip tracking, business classification, and deductions that this app does not provide.

Run `./gradlew testDebugUnitTest --tests 'com.jacobgain.triprabbit.ui.UiOverhaulTest'` after connecting artwork. Screen and launcher previews are written to `app/build/reports/ui/`; review light, dark, small screen, and large text renders before release.
