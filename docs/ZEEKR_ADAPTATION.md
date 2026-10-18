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

1. **(this commit)** seam plan + `ZeekrDisplayRouter` + `ZeekrHabHudOutput` /
   `ZeekrHabNative` / `zeekr_hab.c` skeletons (device constants = TODO).
2. Backend interface + wire `CarPlayController` to it (BYD unchanged).
3. HUD route state + maneuver icon map; bring up HAB output on emulator stub.
4. Vehicle data (VHAL), media-center source, wheel keys.
5. On-car bring-up with the dumpsys facts above.
