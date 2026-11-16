# Omnidroid — working notes

Android emulator frontend (Kotlin + Compose), a heavily extended Lemuroid fork. Upstream is
`Ahmed-Abousaif/Omnidroid`; this checkout works from the fork `mlnchlc/Omnidroid`.

## Branches and remotes
- `origin` = upstream (read-only for us). `fork` = `mlnchlc/Omnidroid`.
- `dev` = personal integration branch: upstream `master` plus all of our in-flight PR branches,
  merged with `--no-ff`. This is what gets built and installed on the phone.
- PR branches (`fix/*`, `feat/*`) always start from upstream `master`, one change each, pushed to
  `fork` and opened against `Ahmed-Abousaif/Omnidroid:master`. PRs from a fork cannot be stacked,
  so keep them independently mergeable and avoid adding code at the same spot as another open PR.
- New work: branch from `master`, open the PR, then `git merge --no-ff <branch>` into `dev`.
- Upstream moved: `git fetch origin && git checkout master && git merge --ff-only origin/master`,
  then merge `master` into `dev` (and rebase open PR branches if they conflict).
- `CLAUDE.md` and `tools/arm64-host/` live only on `dev`; never include them in upstream PRs.

## Building (this VM is arm64)
- `./gradlew assembleFreeDynamicDebug` → `omnidroid-app/build/outputs/apk/freeDynamic/debug/`
  (`Omnidroid-dynamic-arm64-v8a-debug-v3.0.0.apk`; installs as `com.omnidroid.debug`, label
  "OmnidroidDebug", alongside the release app).
- Build speed: Gradle build cache + configuration cache are on, so incremental builds take ~10-30 s
  even across branch switches; run one Gradle invocation per check (tests + assemble together) and
  filter tests with `--tests`. Don't use `-Pandroid.injected.build.abi=…`: it produces a test-only
  APK with versionCode 3000 (below the split APK's 3003), which neither adb nor the phone's installer
  accepts as an update. Never start a shell command with a bare `cat > file` (it waits on stdin).
- Install: when adb is connected, `adb -s <ip:port> install -r <arm64 apk>` and check for `Success`
  (it can fail silently behind `tail`); also Taildrop the same APK.
- Unit tests: `./gradlew :omnidroid-app:testFreeDynamicDebugUnitTest` (JUnit only — no Robolectric
  or mocking libs; keep logic testable as pure functions, see `ScreenLayoutCycle`).
- `ktlintCheck` currently crashes with an internal `HEADER_KEYWORD` error on every source set,
  unrelated to our code; don't treat it as a lint failure.
- The CI's Switch (Eden) native build (`omnidroid-cores/update_cores.py --cores switch`) is skipped
  locally; the app builds without it.
- Private submodules `omni-libretrodroid` and `modules/cemu/upstream/cemu-libretro-android` are not
  accessible. Without the first, the build uses the JitPack AAR
  `com.github.Ahmed-Abousaif:omni-libretrodroid`, which needs the `libc++_shared.so` pickFirst
  (upstream PR #39) or `mergeNativeLibs` fails. A failed `git submodule update` can leave other
  submodules (PadKit, libchdr) cloned but empty — fix with `git -C <path> reset --hard HEAD`.
- arm64 toolchain: Google's SDK tools are x86_64-only. `tools/arm64-host/setup-arm64-sdk.sh`
  sets up the SDK in `~/Android/Sdk` (arm64 aapt2 via `android.aapt2FromMavenOverride` in
  `~/.gradle/gradle.properties`, NDK clang wrapped around Ubuntu LLVM 19). `local.properties`
  needs `sdk.dir=/home/ubuntu/Android/Sdk` and `cmake.dir=/usr`.

## Device testing
- Phone: Galaxy S25 (`abhisheks-s25` on Tailscale, `100.83.100.60`).
- After every successful build, send the arm64 APK without asking:
  `tailscale file cp <apk> abhisheks-s25:` — works even when Tailscale lists the phone offline (it pulls
  queued files when it reconnects), so never skip it.
- adb over Tailscale works once the user enables Wireless debugging and gives a pairing code:
  `adb pair 100.83.100.60:<pair-port> <code>` then `adb connect 100.83.100.60:<port>`.
  Ports change every time. With adb connected, `adb install -r <apk>` and logcat (game process is
  `com.omnidroid.debug:game`) are the fastest way to debug.

## Architecture map
- `retrograde-app-shared`: UI-free core. `lib/library/GameSystem.kt` is the central registry
  (systems, cores, default `CoreVariable`s, in-game `ExposedSetting`s); `ControllerConfigs.kt` maps
  systems to touch layouts (`allowTouchOverlay` etc.). Also storage scanning, Room DB, saves, core
  download (`GithubCoreDownloader`), backend interfaces (`lib/backend`).
- `omnidroid-app`: UI. Games run in a separate `:game` process (`GameService` calls `exitProcess`
  shortly after the game finishes). `BaseGameScreenViewModel` composes helpers in
  `shared/game/viewmodel` (`GameViewModelRetroGameView`, `…Input`, `…TouchControls`, `…Saves`,
  `…SideEffects` for one-off UI events). `MobileGameScreen.kt` lays out game view + PadKit pads.
  The in-game menu is a separate activity whose results land in `BaseGameActivity.onActivityResult`,
  which calls `refreshSettings()` (re-sends every core option to the running core).
- Backends: `LibretroBackendSession` drives LibretroDroid through `GLRetroView` or
  `VulkanRetroView`; `SwitchBackendSession` drives Eden.
- `omnidroid-touchinput` (layouts per console) on PadKit (`modules/padkit/upstream`).

## Gotchas learned the hard way
- The renderer (GL vs Vulkan view) is fixed at launch from the renderer core options; never hand a
  running core a different renderer (it crashes) — see `GameViewModelRetroGameView.RENDERER_KEYS`.
- `disableTouchEvents()` swallows all native touches on the game view for every system; DS/3DS
  touch only arrives through `TouchScreenPointerOverlay`.
- Vulkan: LibretroDroid steps with a null view, so it never gets the `refreshAspectRatio`
  callback GL gets, and its descriptor binding cache can keep a destroyed core image when the
  core recreates its frame with a reused handle (garbage/flicker). `VulkanRetroView` refreshes and
  clears the cache via `onTrimMemory(TRIM_MEMORY_RUNNING_LOW)` after option changes.
- Apply core-option batches between frames (emulation thread / `GLSurfaceView.queueEvent`), not
  from the UI thread.
- 3DS runs on Azahar; it accepts the legacy Citra layout values the app sends
  (`Single Screen Only`, `Side by Side`, swap `Top`/`Bottom`).
