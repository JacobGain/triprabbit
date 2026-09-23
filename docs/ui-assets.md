# UI artwork

The five commissioned PNGs are installed in `app/src/main/res/drawable-nodpi/`. This folder prevents Android from rescaling the artwork. Each file is used directly by the production UI; replacing a PNG with another of the same name updates the app without changing Kotlin code.

| Resource | Source dimensions | Use |
| --- | --- | --- |
| `brand_mark.png` | 1254 × 1254, transparent | Rabbit mark in the 44 dp app header. It sits on a pale mint tile for contrast and appears on Home and Welcome. |
| `welcome_art.png` | 1942 × 809 | Wide, rounded illustration on the welcome screen. |
| `dashboard_art.png` | 1942 × 809 | Wide, rounded illustration on Home, below the mileage summary. |
| `launcher_foreground_art.png` | 1254 × 1254, transparent | Full-colour adaptive launcher foreground. |
| `launcher_monochrome_art.png` | 1254 × 1254, transparent | Single-colour Android themed icon foreground. |

The two landscape images are displayed at a 2.4:1 ratio with centre cropping. Keep important details away from the edges when replacing them, and leave text in the native UI. The launcher PNGs are referenced by `res/drawable/ic_launcher_foreground.xml` and `ic_launcher_monochrome.xml`; those XML wrappers inset the supplied artwork so adaptive icon masks leave the rabbit visible. Both launcher shapes use the pale background in `res/values/colors.xml`. Keep the PNGs transparent and do not bake in a background or icon mask.

The 24 dp outline control icons in `res/drawable/ic_*.xml` are functional UI symbols and are separate from the rabbit identity. They share a consistent stroke style and are mapped through `AppIcon` in `BrandComponents.kt`.

For a future store listing, export separate store graphics and screenshots based on the final UI. These are not packaged in the app. [The original mockup](ui-reference.png) is a design reference only; its GPS trip tracking, business classification, and deduction features are not in the app.

Run `./gradlew testDebugUnitTest --tests 'com.jacobgain.triprabbit.ui.UiOverhaulTest'` to regenerate screen and launcher previews under `app/build/reports/ui/`. Review light, dark, compact, and large-text screens before release.
