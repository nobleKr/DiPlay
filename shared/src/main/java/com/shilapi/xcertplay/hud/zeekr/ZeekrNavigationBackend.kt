/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * Zeekr backend skeleton. Brings up the HUD output (ZeekrHabHudOutput) and will
 * render iAP2 RouteGuidance onto it (increment 3) and report VHAL vehicle data
 * (increment 4). Unmarked paths are TODO; nothing here changes the BYD path.
 */
package com.shilapi.xcertplay.hud.zeekr

import android.content.Context
import android.util.Log
import com.shilapi.xcertplay.hud.NavigationHudBackend
import com.shilapi.xcertplay.iap2.wire.Iap2Frame
import com.shilapi.xcertplay.transport.VehicleSpeedReading
import com.shilapi.xcertplay.transport.VehicleSpeedSource
import com.shilapi.xcertplay.transport.VehicleStatusProvider

object ZeekrNavigationBackend : NavigationHudBackend {
    private const val TAG = "Zeekr-Nav"
    override val id = "zeekr"

    @Volatile private var hud: ZeekrHabHudOutput? = null

    override fun start(context: Context) {
        if (hud != null) return
        ZeekrDisplayRouter.clusterDisplay(context) // logs the pick; wired to Presentation in increment 3
        hud = ZeekrHabHudOutput().also { if (!it.start()) Log.w(TAG, "HUD output not available yet") }
    }

    override fun carPlaySessionStarted() { /* TODO(increment 3): reset HUD route state */ }

    override fun onFrame(frame: Iap2Frame) {
        // TODO(increment 3): ZeekrHudRouteState.accept(frame) -> render onto hud.inputSurface
    }

    override fun endNow(preserveTurnOverlay: Boolean) {
        hud?.close(); hud = null
    }

    override fun setClusterStreamControl(control: (Boolean) -> Unit) { /* TODO(increment 3) */ }
    override fun clearClusterStreamControl(control: (Boolean) -> Unit) { /* TODO(increment 3) */ }

    // TODO(increment 4): read VHAL (android.car CarPropertyManager / Zeekr DeviceAPI).
    override fun batteryStatus(context: Context): VehicleStatusProvider = VehicleStatusProvider { null }
    override fun wheelSpeed(context: Context): VehicleSpeedSource = object : VehicleSpeedSource {
        override fun start() {}
        override fun stop() {}
        override fun drain(): VehicleSpeedReading? = null
    }
    override fun parked(context: Context): Boolean? = null
}
