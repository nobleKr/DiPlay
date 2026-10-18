# Zeekr 001 adaptation (branch `zeekr`)

Goal: add Zeekr 001 DHU support to DiPlay from the **open source**, as a parallel
platform backend next to the existing BYD one. No code is copied from any
third-party closed/obfuscated build; the decompiled reference is used only for
system-interface facts (interop), and no Carlinkit/MFi identity is bundled.

## License

DiPlay is GPLv3; DiAuto UI portions are AGPL-3.0. This branch stays under the
same terms — all Zeekr code added here is GPLv3 and published with the fork.
We do **not**:
- copy a third party's obfuscated payload classes verbatim;
- ship the Carlinkit MFi identity (bring your own MFi coprocessor / server).

## Architecture seam

Today `CarPlayController` calls `BydNavigationOutputs.*` statically — platform is
hard-wired. Adaptation introduces a backend interface and selects at runtime:

```
hud/NavigationHudBackend.kt      (interface: onFrame, start, endNow, battery,
                                   wheelSpeed, parked, cluster controls …)
hud/BydNavigationBackend.kt      (wraps existing BydNavigationOutputs — no logic change)
hud/zeekr/ZeekrNavigationBackend.kt
hud/HudBackends.kt               (detect platform → pick backend)
```

`CarPlayController` holds a `NavigationHudBackend` instead of calling `Byd…`
directly. BYD behaviour is unchanged (default backend).

Platform detect (open signals): presence of `com.zeekr.*` packages / Zeekr
`ro.*` props vs BYD `com.byd.*`. See `v60`-style probing already understood.

## Feature map (Zeekr ← where it plugs in)

| Feature | Open DiPlay anchor | Zeekr work |
|---|---|---|
| HUD frame output | `hud/BydHudBridge` (SomeIP/binder) | `zeekr/ZeekrHabHudOutput` — render 780×244 into `ImageReader`, push each `HardwareBuffer` to vendor `libHabCamera` via our own JNI shim `zeekr_hab.c`. **Not** the third party's `libhabmanager`/`com.pc.hablib`. |
| Cluster/HUD display pick | (BYD cluster monitors) | `zeekr/ZeekrDisplayRouter` — `DisplayManager` PRESENTATION category, exclude id 0, match by size, fallback by name `cluster`/`secondary`. |
| Turn/maneuver → HUD | `hud/BydHudRouteState`, `BydManeuverCodes` | `zeekr/ZeekrHudRouteState` — reuse iAP2 RouteGuidance parsing; map maneuver → Zeekr HUD icon set. |
| Vehicle data (charge/range/speed/gear) | `transport/VehicleStatusProvider`, `VehicleSpeedSource` | `zeekr/ZeekrVehicleStatus` — read VHAL via `android.car` CarPropertyManager / Zeekr DeviceAPI. Read-only. |
| Media-center source + artwork | `media/AndroidMediaSink`, MediaBrowserService | Zeekr: expose MediaBrowserService root, grant artwork URI to `com.zeekr.mediacenter`/`mediawidget`/launchers. |
| Steering-wheel keys | `WheelKeyService` | add Zeekr keycodes/scancodes + joystick/zoom mapping. |
| fdbus system HMI (if needed) | — | `zeekr/ZeekrSystemHmi` over `libfdbus-systemhmi-jni` — only if a feature needs it; prefer public `android.car`. |

## Device facts still needed (pull from the car)

- `dumpsys display` and `dumpsys SurfaceFlinger --display-id` → cluster/HUD
  display IDs, sizes, PRESENTATION flags.
- `libHabCamera.so` exported symbols/ABI (the frame-listener entry points) →
  to write `zeekr_hab.c` against the real vendor interface.
- fdbus system-HMI service name(s), if we end up needing fdbus.
- VHAL property IDs for charge/range/speed/gear on this firmware
  (`dumpsys car_service`), or confirm Zeekr DeviceAPI availability.
- Wheel key `getKeyCode()/getScanCode()` captured from the actual wheel.

## Increments

1. seam plan + `ZeekrDisplayRouter` + `ZeekrHabHudOutput` / `ZeekrHabNative` /
   `zeekr_hab.c` skeletons (device constants = TODO). **done**
2. `NavigationHudBackend` interface + `BydNavigationBackend` (delegate, no BYD
   change) + `ZeekrNavigationBackend` skeleton + `HudBackends` selector.
   **done (additive).**
3. Wire `CarPlayController` to `HudBackends.select(context)` (field `nav`);
   all 8 `BydNavigationOutputs.*` call sites now go through the backend.
   BYD delegates 1:1, so BYD behaviour is unchanged. **done — needs `./gradlew` verify.**
4. Zeekr display geometry (ZeekrDisplays 2560x1600/2560x536/1024x576, ±5%),
   HAB region 780x244 (ZeekrHudRegion), and VHAL read (ZeekrCarSignals/
   ZeekrVehicleStatus: battery/range/gear/speed via android.car reflection,
   property ids from the engine reference). Wired into ZeekrNavigationBackend
   (battery + parked) and ZeekrDisplayRouter. Unit test: ZeekrDisplaysTest.
   **done.**
5. Wheel-speed source (ZeekrWheelSpeed/ZeekrSpeedBuffer) via ZeekrCarSignals,
   wired into ZeekrNavigationBackend.wheelSpeed. Unit test: ZeekrSpeedBufferTest.
   **done.** (HUD route-state + maneuver icon map folded into on-car step —
   needs the Zeekr HUD icon set + libHabCamera JNI.)
6. Media/wheel reference data: ZeekrMediaTargets (artwork authority + consumer
   packages + grant helper) and ZeekrWheelCommand (engine command strings).
   Unit test: ZeekrMediaAndWheelTest. **done.** (MediaBrowserService + artwork
   provider + accessibility-service capture = app-module manifest wiring, on-car.)
7. On-car bring-up: libHabCamera JNI symbols, HUD icons, MFi key source
   (user-supplied), app-module services wiring + manifest.

## On-car / app-module remainder (findings 2026-10-18)

Device analysis of libHabCamera.so / libHabCameraJni.so and the user's
zeekr-adapt project settled how the remaining pieces work:

### HUD output via HAB — native, on-car
- `libHabCamera.so` exports only the vendor C++ `com::driving::parking::`
  `HabCameraSocket` / `HabCameraHandler`; `libHabCameraJni.so`
  (`com.zeekr.habcamera.HabCamera`) is camera **preview** (setPreviewSurface),
  i.e. input, not an HUD-output API.
- The third-party `libhabmanager.so` outputs to the HUD by **subclassing the
  vendor `HabCameraListener`** (virtuals `set_image_attribute(HabCameraBuffer*)`,
  `notifyHabWriteBuffer(HabCameraBuffer*)`, `notifyHabRelease()`), registering it
  with libHabCamera, and writing the frame into the HabCameraBuffer the vendor
  hands back — the same HAB channel the parking camera uses.
- So `jni/zeekr_hab` must be a C++ shim subclassing that vendor listener. Without
  vendor headers the class/vtable layout is reconstructed, so it must be built
  and tested on the car (a wrong layout crashes). Not verifiable in CI.

### Media centre — reuse the user's verified bridge
- The user's zeekr-adapt has a firmware-verified binder protocol
  (docs/zeekr-mediacenter-protocol-verified.md, MediaBridge, CoverProvider):
  direct bind to `ecarx.xsf.mediacenter.ZeekrMediaCenterService`
  (exported, no permission), two-phase register, transact codes from smali.
- Port MediaBridge/CoverProvider (his code) into the fork's app module rather
  than reversing the third-party build. This plus ZeekrMediaTargets covers
  "CarPlay as a source + artwork".

### MFi — user-supplied keys
- Accessory cert/key fed via a configurable source (local i2c MfiTarget or
  remoteMfiServer); wire a config path for the user's own test keys. No
  Carlinkit identity is bundled.

### App-module wiring (not shared/)
- MediaBrowserService, artwork FileProvider, WheelKey AccessibilityService and
  manifest entries live in common/mobile; done when building the app for Zeekr.

### HUD broadcast (AMap) — ported
- The engine reference drives the HUD/cluster primarily by the AMap standard
  broadcast `AUTONAVI_STANDARD_BROADCAST_SEND` (KEY_TYPE 10001 guidance / 10019
  state) to com.zeekr.automap/navi/hud/carlauncher, com.geely.navi, com.ecarx.navi,
  plus the ECARX DimInteraction SDK and a 780x244 AR-HUD pixel path as extra
  channels. Ported: ZeekrHudTurnId, ZeekrHudMessage, ZeekrHudBroadcaster, wired
  into ZeekrNavigationBackend.onFrame via the engine route parse. Tests:
  ZeekrHudTest. ECARX DimInteraction + AR-HUD pixel path remain for on-car.
