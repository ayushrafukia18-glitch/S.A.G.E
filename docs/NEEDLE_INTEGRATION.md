# Needle 3 native integration — current state

This documents exactly what the supplied `needle-main` source (the
`cactus-needle` PyPI package, v3.0.2 engine) confirms, and exactly what is
still missing before `NeedleNativeEngine` can route real requests. It exists
so nobody re-derives or re-guesses this later.

## Confirmed by the source (safe to rely on)

| Fact | Evidence |
|---|---|
| Model weights filename: `needle3.cact` | `needle/agent/fetch.py`, `BASE_WEIGHTS = {3: "needle3.cact"}` |
| Android is a supported deploy target | `needle/agent/fetch.py`, `PLATFORMS` includes `"android-arm64"`, `"android-armv7"`, `"android-riscv64"` |
| Android's native engine filename: `libneedle.so` | `needle/agent/fetch.py`, `_lib_name()` returns `libneedle.so` for every target that isn't `darwin`/`win32` (Android included) |
| Engine + weights are fetched, not compiled locally | README: "Every deployment target ships a prebuilt engine under 1 MB that loads the `needle3.cact` weights at start." |
| Fetch command | `needle build --platform android-arm64 [--layers N] --out <dir>` (CLI, run on a dev machine with `cactus-needle` installed — **not** a Gradle/Maven step) |
| That command also fetches a C header | llms.txt: "`--platform` also downloads that platform's engine and header and makes `--out` a folder holding them plus `needle3.cact`." |
| Tool schema format (raw JSON schema) | llms.txt, "Raw JSON schema (what the engine actually consumes)" — `{"name", "description", "parameters": {"type","properties","required"}}`. Mirrored exactly in `NeedleToolSchema.kt`. |
| Response shape | llms.txt, "Response shape" — `{"type","success","error","error_code","function_calls","reasoning","confidence","suppressed_calls","validation","prefill_tps","decode_tps"}`. Mirrored exactly in `NeedleResult.kt`. |
| Confidence floor for auto-suppression | llms.txt: "confidence below 0.1 or a grounding gate fired" moves a call to `suppressed_calls`. This is Needle's own behavior, not app policy. |
| No Maven/Gradle artifact exists for Needle | The package is Python-only (`pip install cactus-needle`); the Android engine is a prebuilt binary fetched by the CLI, never published as a Gradle dependency. |

## NOT in the supplied source (do not invent)

- **The C header's contents.** It's fetched by the `needle build --platform
  ...` command above, into whatever `--out` directory you choose — it is not
  part of this repository's source tree, and `llms.txt` (which explicitly
  states it's meant to be a complete reference for AI coding assistants and
  warns "do not invent API that is not listed here") does not list any C
  function names.
- Therefore: no JNI `external fun` bindings to real Needle symbols, no
  `needle_init`/`needle_route`-style function names, and no struct layouts
  have been written anywhere in this project. `NeedleBridge.kt` and
  `native/needle/NeedleJNI.{h,cpp}` define only SAGE's *own* Kotlin↔C++
  boundary (names we chose), with empty/stub bodies on the C++ side.

## What's implemented right now

- `NeedleToolSchema.kt`, `NeedleResult.kt` — real, usable data models matching
  the confirmed formats above.
- `NeedleEngine.kt` — the `initialize()/isAvailable()/route()/close()`
  abstraction.
- `NeedleUnavailableEngine.kt` — fully functional. Always returns a
  `"respond"` result with no function calls, so `SageOrchestrator` correctly
  falls through to `LlmGateway` for every request. **This is what SAGE
  actually runs on today.**
- `NeedleNativeEngine.kt` — checks whether `needle3.cact` (assets) and
  `libneedle.so` (jniLibs) are present, and honestly reports itself
  unavailable either way right now, since the JNI wrapper that would call
  into them doesn't exist yet either.
- `NeedleRouter.kt` — picks native if `initialize()` succeeds, else the
  fallback. No other code needs to know which one is active.
- `native/needle/CMakeLists.txt`, `NeedleJNI.h`, `NeedleJNI.cpp` — scaffold
  only, **not** wired into `app/build.gradle.kts`, so their unfinished state
  doesn't affect whether the app compiles.

## Steps to complete native integration

1. On a dev machine: `pip install cactus-needle`, then
   `needle build --platform android-arm64 --out native/needle/android-arm64`
   (repeat per ABI you plan to ship — arm64 covers the overwhelming majority
   of real Android devices; add armv7 only if you need it).
2. Copy the resulting `libneedle.so` into
   `app/src/main/jniLibs/<abi>/libneedle.so`, and `needle3.cact` into
   `app/src/main/assets/needle3.cact`.
3. Inspect the fetched header. Fill in `native/needle/NeedleJNI.cpp`'s three
   function bodies against what that header *actually* declares — not
   against any name mentioned in this document, all of which are ours, not
   Needle's.
4. Add `external fun nativeInit/nativeRoute/nativeClose` declarations to
   `NeedleBridge.kt` matching the JNI exports you just implemented, and call
   them from `NeedleNativeEngine`.
5. Add an `externalNativeBuild { cmake { path = "../native/needle/CMakeLists.txt" } }`
   block to `app/build.gradle.kts`, and uncomment the `IMPORTED` target lines
   in `CMakeLists.txt`.
6. Delete the "not yet implemented" `Result.failure(...)` branches in
   `NeedleNativeEngine.initialize()` and replace with the real
   `System.loadLibrary("sage_needle_jni")` + `NeedleBridge().nativeInit(...)`
   call.

Until step 6, `NeedleRouter` will keep resolving to `NeedleUnavailableEngine`,
and the app behaves exactly as documented in PRODUCT VISION for "not an
action": every request goes to Gemini/Grok.
