# Changelog

## Unreleased

- Add a Motion Lab page (settings menu): six live sliders — speed, bounciness, damping, liquid morph amount, transparency, blur strength — that retarget in-flight menu animations in real time, an on-page preview menu to try them immediately, and a reset-to-defaults button.
- Introduce `MenuMotionConfig`, a single centralized object that owns every liquid-menu motion and material parameter. Spring specs resolve damping/response through it at retarget time, all timeline delays scale by it, morph amplitudes multiply its morph factor, and the material recipes derive their clarity factor from its transparency setting — the settings page writes the same values the engine reads.
- Even clearer glass: the menu material recipes now scale their black luma, face fill, dark roll-off, and overlay alpha by a clarity factor derived from `MenuMotionConfig.transparency`, and the default container tint drops again (light 0.5 → 0.42, dark 0.44 → 0.38 alpha) so backdrop content reads through menus while text stays legible.
- Fix the capsule dead zone: the collapse tap router no longer checks the anchor-acquired flag, which releases a frame or two before the popup leaves the window and briefly made the just-collapsed capsule untappable. Taps landing on the live capsule bounds (with touch forgiveness) now reopen the menu instantly; verified on-device at 50/120/300 ms gaps after collapse.
- Fix the collapse flicker: the menu no longer fades the whole surface out with a tween when settling into the capsule, and reopening snaps surface alpha back instead of replaying the fade, so the morph into the capsule plays without a half-transparent flash.
- Fix the settings menu clipping its last item: the item count now matches the actual entries so Motion Lab is reachable.

- Make mid-collapse reopening reliable: the fullscreen popup's dismiss layer no longer swallows taps on the anchor while the liquid absorb animation plays. A topmost tap router that only exists during a collapse sends taps landing on the live anchor (with touch forgiveness) back to expansion, and Popup window properties follow the popup session instead of the expanded flag so rapid open-close flips never transiently drop window focus.
- Morph collapses keep the surface fully opaque and let the motion channels own visibility (content alpha, source opacity, drop shadow); reopening snaps the surface alpha back instead of replaying a 120 ms fade, removing the half-transparent flash on interruption.
- Clearer glass: lower the light recipe's black luma floor (0.4 → 0.3) and white face fill (0.2 → 0.1), soften the dark recipe's white lift and luminance roll-off, and thin the default container tint (light 0.64 → 0.5, dark 0.54 → 0.44 alpha) so backdrop content reads through menus while text stays legible.
- Stop capture-band ratcheting in the liquid menu renderer: backdrop sample, portal foreground, and material recording bands previously only ever unioned, so repeated open/close cycles grew the RenderEffect area to the worst case ever seen. Bands now step back down to the grid-quantized live requirement (128 px grid keeps sliding captures from resizing their backing store every frame), bounding GPU fill and blur cost.
- Trim the demo to the Menus page only: the app launches straight into it, the back button and every other page are gone, and the header action capsule gains a settings entry with FPS overlay, HDR output, and reduce-transparency toggles.
- Rewrite the FPS overlay: default-on, fixed to the bottom-left, allocation-free ring buffer, a 500 ms sliding-window cadence that only publishes on change, and the frame stream doubles as input for the new performance governor.
- Add adaptive resource limiting: a governor watches frame budgets and, under sustained load, steps the glass backdrop sampling down (0.75x then 0.5x via `GlassSamplingPolicy.globalScale`) and caps the display refresh rate, recovering one step at a time once frames calm down.
- Menu open/close interruption is verified end to end: springs retarget from the live contour, and rapid open-close cycling leaves no artifacts.

- Lower the minimum supported Android version from API 33 to API 31. Backdrop frost, tints, shapes, and every interaction run on Android 12; the AGSL refraction pass, optical highlights, and the native control lens remain API 33+ and degrade to the Gaussian frost automatically. `isRenderEffectSupported()` (API 31) and `isRuntimeShaderSupported()` (API 33) stay the public capability probes.
- Shrink the Demo APK from 17.7 MB to 7.6 MB without dropping content: the bundled wallpapers ship as WebP instead of PNG (−7.5 MB), release builds keep only arm64-v8a and armeabi-v7a copies of the native SDF library (−1.9 MB), `isShrinkResources` prunes unreferenced resources, and the native library drops RTTI, exceptions, unwind tables, and unused sections (−2.7 MB across ABIs, no arithmetic change).
- Start faster and allocate less: SF symbol names are resolved only when a symbol font is installed (the 8087-entry catalog no longer builds on every icon), sample wallpapers decode per appearance instead of both on cold start, and settings save through `apply()` so an output-mode restart never blocks on disk.
- Strip `android.util.Log` calls from release builds. Removed calls also remove their format strings.
- Verify the Android 12 path on an API 31 emulator end to end: menu morph, materials, navigation tabs, and controls launch without crashes. Without AGSL, tab-bar selection indicators, switch/slider thumbs, and the menu field-texture pipeline fall back to portable drawing instead of disappearing.

## 0.1.2

- Add adaptive top tabs, native sidebar transitions, and fullscreen overlays above adaptive navigation.
- Improve sidebar materials, content sampling, and navigation glass rendering performance.

## 0.1.1

- Reduce active navigation tab rendering work by sharing scene and lens programs.
- Reuse tab programs and build atlas shader sources lazily.

## 0.1.0

Initial Android Compose framework extraction from MeiloX.

- Introduce Core, Glass, UI, Scaffold, Preference, and Icons modules with one-way dependencies.
- Add light/dark themes, host-configurable colors and typography, and continuous rounded shapes across the component system.
- Preserve backdrop sampling, liquid refraction, press highlights, shadows, navigation contact effects, and menu morph rendering.
- Provide buttons, switches, sliders, segmented controls, text/search fields, steppers, cards, lists, toolbars, navigation bars, menus, alerts, sheets, a color picker, and reorderable groups.
- Add collapsing large-title pages, lazy content, overscroll, pull-refresh rebound, insets, and generic anchor-based sheet state.
- Collapse large titles on a scroll breakpoint (56 dp enter, 24 dp exit) instead of mapping the scroll offset, and play the collapse with the navigation spring. `rememberGlasmioGridCollapseProgress` keeps its name but returns the animated breakpoint value; `rememberGlasmioHeaderScrollEdge` exposes the still-continuous top-bar material progress.
- Separate preference presentation from persistence and symbol rendering from application resources.
- Provide one root-level SF symbol font provider, a replaceable icon resolver, and a local debug font option for the sample.
- Add an independent interactive sample and documentation for component use, backdrop hosting, and migration.
- Unify standalone glass under the navigation renderer with Regular Small, Regular Medium, Regular Large, and Clear scene presets from Apple's Materials page.
- Preserve the original calibrated navigation defaults and complete outline, side-shadow, inner-shadow and shadow-lift layers; scene overrides remain opt-in.
- Add Home, Materials, Settings, and About tabs, reusable floating search, collapsing titles, progressive edge blur, root-tab fades, and Miuix detail-page transitions.
- Use solid catalog backgrounds and the original light/dark iOS 27 wallpaper in glass demonstrations.
- Match the reference text metrics, tracking, control label weights, and continuous corners.
- Add eleven semantic text styles, 34 emphasis/italic variants, and all twelve Dynamic Type categories.
- Match tall swipe actions, structural separators, reorder handles, Alert field spacing, and Stepper press clipping.
- Return plain Pop-up menus to their live text source, preserve corner tangents on tall menus, and retain four-sided shadows through opening and dismissal.
- Retain source-material shadows and interpolate backdrop frost and tint when glass menus return to their toolbar or button.
- Add compact and expanded text editing menus, real 16 dp selection handles, and a Clear cursor magnifier adapted from AndroidLiquidGlass, with safe-area placement and spring following.
- Match the caret loupe to UIKit's glass implementation: a 115 × 85 dp capsule at 1.25× lifted 75 dp above the finger, a 0.3 low-pass into a damping 0.7 / 0.2 s spring, a two-pass opening, a 0.23 s dismissal that collapses the sampled content towards 0.125×, acceleration-driven body deformation with UIKit's tanh soft clamp and live-bounds leading-edge drift, and a dormant line-break dismissal hook. `GlasmioMagnifier` exposes the timings as `GlasmioLoupeMotion`, `rememberGlasmioLoupeFollow` reuses the follower for other lenses, and `docs/ios-loupe-motion.zh-CN.md` records the recovered parameters. The follower keeps UIKit's model and presentation centres apart, and the deformation differentiates absolute velocity the way `prefersDirectionlessAcceleration` does, so a drag in either direction flattens the body and only the drift mirrors.
- Configure signed Maven artifacts, sources, API documentation, and automated Central publication.
- Apache-2.0 license with third-party notices included in every published module.

MeiloX remains responsible for music playback, product navigation, application settings, and player-specific rendering.

