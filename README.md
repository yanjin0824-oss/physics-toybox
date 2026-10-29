# 物理玩具箱 — PICO Spatial

A native Kotlin / PICO Spatial SDK 6.1.9 physics playground in **Full Space with Mixed Stage**. The real environment remains visible through video see-through. A1.28m arena holds exactly50 colorful0.12m blocks in a six-tier pyramid. Click a block to fire at its current native bounds center; clicking the arena fires at the pyramid center. The 发射 (Fire) button targets the tower center and 重建 (Rebuild) restores all50 blocks. The default app title, controls, feedback and accessible labels use Chinese for the CN release.

The stage uses native rigid bodies, gravity, friction, geometry-derived inertia and continuous collision detection. A live HMD tracking listener supplies the latest Stage-coordinate head position. Each ball starts0.20m in front of that head position along the shot ray; its velocity compensates gravity to reach the target. No fixed front-edge or four-side approximation is used. Shared meshes/materials/collision shapes are retained across rebuilding and explicitly released when the scene closes.

## Build and run

```sh
./gradlew assembleDebug
pico-cli app install app/build/outputs/apk/debug/app-debug.apk --device emulator-5554
pico-cli app launch com.example.physicstoybox.pfa72bc90 --activity .platform.LaunchActivity --device emulator-5554
```

Use an Android SDK with API 35, JDK 21, and network access to Google Maven, Maven Central, Gradle Plugin Portal and the public PICO Maven repository. In Android Studio, run **Sync Project with Gradle Files** before the first IDE run.

```sh
./gradlew testDebugUnitTest
```

The tests cover the exact block count, unique positions, tier support, all six colors, empty layouts, and the ready/fire/rebuild/physics feedback protocol. Runtime screenshots and verification logs live under `.scratch/`.

The native Rebuild regression is checked on an emulator/device:

```sh
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.physicstoybox.pfa72bc90.SceneResourcesRegressionTest,com.example.physicstoybox.pfa72bc90.RebuildInteractionTest,com.example.physicstoybox.pfa72bc90.SceneAimRegressionTest
```

`SceneResources` retains shared SDK meshes, materials and collision shapes with `toGlobal()` before attaching them to entities. Rebuild destroys the previous entities while retaining these resources; scene disposal destroys entities and explicitly closes the resources. A Kotlin reference alone cannot prevent the SDK from releasing a resource when its last entity owner is destroyed. The regression tests cover repeated destruction of all 50 resource owners, translated/moved native block aim, 12 consecutive Fire actions without waiting, six serial shots, and four real Fire/Rebuild cycles that require a native ball–block collision. Commands remain in a queue until the scene acknowledges them, so UI state conflation cannot drop shots. Projectiles have physics colliders but do not intercept pointer taps.

## Implementation

- `ui/toybox/components/ToyboxScene.kt`: scene resources, native physics, launch/cleanup and moved-block counters.
- `ui/toybox/components/ToyboxStage.kt`: SpatialView, targeted Entity taps, ordered command consumption, live head-pose tracking and in-stage control attachment.
- `domain/usecase/BuildTowerUseCase.kt`: the 20+12+9+6+2+1 layout.
- `ui/toybox/components/ToyboxControls.kt`: native SpatialUI controls and feedback.

Editor-authored source was preserved in `editor-asset/`, but the Editor's automatic capture failed and its bundle delivery did not complete. The running app uses code-owned ECS geometry. `.scratch/editor-fallback.json` records the failure and recovery.

The installed style verifier excludes all source files when a parent directory is named `test`, and its project-root scan includes skill example sources. The isolated copy under `.scratch/validation-skills/` scopes exclusions and source roots to the application; it preserves the actual checks. Run the full verification with:

```sh
bash .scratch/validation-skills/spatial-design-to-app/scripts/validate_workflow_and_build.sh . \
  --input-mode intent_only --generation-mode existing_module \
  --design-color 'coral=#FF686B' --design-color 'amber=#FFBC42' \
  --design-color 'mint=#60D394' --design-color 'blue=#4D9DE0' \
  --design-color 'violet=#A882DD' --design-color 'pink=#F78FB3' \
  --design-color 'arena=#171F2D' --design-color 'grid=#334056' \
  --design-color 'ball=#F4F6FA'
```

Native text uses `labelPrimaryLight` for readability over the dark front rail; the browser design preview uses dark labels on its light environment. This foreground adaptation is documented in the implementation mapping. Native theme roles are unchanged. No experimental API is enabled. Real headset comfort and performance remain unmeasured; validation uses the PICO Emulator.

## Full Space tracking and lifecycle

The launcher uses `DefaultStage` and `pico.spatial.stage.style=1` (Mixed). Mixed does not use Progressive immersion blending. The app adds no enclosing sky or window material that hides VST. The scene root is translated to(0,1.1,-2.2)m; head data is converted from the current Stage space to that root using public SDK coordinate conversion.

`HeadPoseTracker` stores callback data in an atomic reference. Tracking callbacks never update ECS or Compose. The scene reads the latest pose on the main thread at firing time and enables Fire only after a valid pose has arrived. Listener removal, provider stop, entity destruction and resource close are paired at disposal.

`PlanShotUseCase.fromHead` has tests for continuous small head shifts, all approach directions, muzzle clearance and ballistic targeting. Real-world surface anchoring and collisions against physical furniture are outside this toy's current scope; physics collisions are between virtual objects.

## Continuous firing repair verification

The repaired build passed 19 JVM tests and three native Android tests on `emulator-5554` (PICO Emulator 6.2): the 12-action Fire regression, six same-round shots, four Fire/Rebuild cycles requiring real projectile/block collision events, native translated/moved block bounds, and shared-resource reuse. Evidence is saved in `.scratch/fire-diagnostics/final-tests/` and `final-runtime-log.txt`; the crash buffer was empty during verification. Recording startup failed on this emulator, so no video is claimed.

## Rebuild pointer routing repair

Native rays previously reached the arena rails or floor behind the attached controls, and the all-entity tap recognizer consumed those clicks. The control panel now has a non-solid trigger collider and `PanelInputRouter` maps the received Stage hit through the panel’s native transform into its current Compose button bounds. Panel input is consumed separately from scene shooting; cancelled presses do not activate either button. Native Button semantics continue to use the same ViewModel events. The panel keeps its original size, placement and scene ownership.

The pointer regression was reproduced with real emulator mouse clicks, not Accessibility actions: see `.scratch/fire-diagnostics/rebuild-pointer-red.txt` and `rebuild-pointer-green.txt`. Native tests additionally cover repeated Rebuild before any Fire, panel parent translation/scale, cancelled presses, and resource reuse.

Final Rebuild repair validation passed `assembleDebug`, all 19 JVM tests, and three native tests (`PanelInputRouterRegressionTest`, `RebuildInteractionTest`, `SceneResourcesRegressionTest`). On the installed clean build, real pointer input executed three Rebuilds without shooting, then four shots with native block collisions; after camera restoration two further Rebuilds and two Fires worked. A readable 117-second recording was captured at `.scratch/fire-diagnostics/rebuild-pointer-final.mp4`. Camera reset reported `restored=true` and `injected=false`. No Toybox fatal exception was observed; the crash buffer included an older unrelated `os.teatracker` startup exception.

## Public Maven distribution

The project now uses Spatial BOM `6.1.9` from `https://artifact.bytedance.com/repository/Volcengine`. Both plugin and dependency repository lists contain only public endpoints. The internal `https://maven.byted.org/repository/android_public` entries were removed. At verification, the public BOM metadata listed `0.12.2`, `0.13.3`, `6.0.0`, and `6.1.9`; `6.1.9` was the latest release, and the previous `6.2.6` BOM returned HTTP 404. All seven used Spatial modules had public POMs returning HTTP 200; Gradle refreshed their POM/AAR metadata and confirmed public repository checksums.

`assembleDebug testDebugUnitTest --refresh-dependencies` passed with 19 tests; four native tests passed on PICO Emulator 6.2. The knowledge workspace is aligned to SDK line 6.1. Existing Editor/Emulator installations were retained; the runtime validation uses ROM 6.2, which is newer than the selected SDK line. Evidence is under `.scratch/public-maven-migration/`. Earlier SDK 6.2.6 evidence in this document describes the prior build.

The final app manifest explicitly sets `pico.spatial.use_experimental_api=0`, overriding the UI library’s inherited default. The effective merged manifest has exactly one disabled entry; all four native tests also pass with this final setting.

## Original launcher icon

The launcher icon is original Toybox artwork: a stepped colorful cube tower, a white cannon ball, a flying block and the arena. Editable SVG masters and previews live in `design-assets/icons/`; regenerate with `uv run scripts/generate_app_icon.py`. PICO identity uses three 1024×1024 sRGB layers (background/arena, blocks, ball) and matching 32px interior RGBA distance fields. All foreground alpha is binary; the background is fully opaque within the inscribed circle. The template PNGs were removed from production resources.

`assembleDebug lintDebug` passed with zero lint errors and no icon-related findings. APK icon pixels were checked against the generated assets after Android PNG processing. Launcher evidence is `.scratch/icon-refresh/launcher-new-icon.png`.

The updated icon is visible in the PICO launcher and the installed application passes an explicit launch smoke check (`SCENE_READY`, `HMD_READY`). Assets and source remain on Spatial SDK6.1.9/public Maven.

## VibeLab release preparation

The CN release is named **物理玩具箱** and uses APK version `V1.0.0`. `enable_vst=1` declares the existing Mixed Stage experience for VibeLab feature detection. Runtime artwork and physics are unchanged. Release configuration lives in `vibelab.yaml`; `.vibelab/`, `.pico-env.json` and root publishing `assets/` are local inputs excluded from Git.

A clean isolated `assembleRelease testDebugUnitTest` succeeded using new Gradle/Maven caches and with Node and Spatial Editor absent from PATH. After localization, `assembleRelease assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug` passed; all 19 JVM tests passed and lint had zero errors. Instrumentation selectors use string resources so Fire/Rebuild checks follow the app language. The localized instrumentation APK has been compiled; its device tests have not yet been rerun.

The installable debug APK is `app/build/outputs/apk/debug/app-debug.apk`. The release build produces `app/build/outputs/apk/release/app-release-unsigned.apk`; release signing is handled separately by the distribution build. Publishing screenshots must show the current Chinese UI and come from actual runtime captures.

## Publishing media and final localized interaction check

The current `V1.0.0` debug APK was installed and launched on PICO Emulator 6.2. Actual mouse operations fired projectiles and rebuilt the tower. The localized `RebuildInteractionTest` passed (1 native test, zero failures/errors), covering consecutive Rebuilds, a 12-action burst, six serial shots, and four Fire/Rebuild rounds with native projectile/block collisions. Earlier unit checks passed all 19 tests.

Publishing assets are local inputs under ignored root `assets/`: the landscape poster and detail images 1–3 were prepared from the Chinese runtime; detail images 4–5 and the dedicated video cover were extracted from the user-provided runtime recording. The recording retains its original English controls; the current application and publishing copy use Chinese. The 41-second source was cropped and encoded as `2560×1440` H.264/AAC without replacing in-app content. Every configured image passed format, dimension, size and unique-hash checks.

A small camera probe produced live views, but a later probe and explicit reset reported failures. Read-only camera inspection reported no active pose injector. The subsequent fresh launch and native interaction regression passed; no multi-angle capture success is claimed.

The first VibeLab add rejected a missing-HEAD placeholder because the backend requires a full 40-character commit SHA. The release therefore creates a local preparation commit before app allocation, then commits the durable app ID in the final source commit. The release manifest omits `build.commit_sha` so the CLI resolves the final pushed HEAD without a self-referential commit.
