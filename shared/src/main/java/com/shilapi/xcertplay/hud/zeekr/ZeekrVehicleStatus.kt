/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * Adapts ZeekrCarSignals into the engine's VehicleStatusProvider (EV charge/
 * range for iAP2 VehicleStatus) and a parked() check. Wheel-speed source is a
 * stub until increment 5.
 */
package com.shilapi.xcertplay.hud.zeekr

import android.content.Context
import com.shilapi.xcertplay.transport.VehicleGear
import com.shilapi.xcertplay.transport.VehicleStatusProvider
import com.shilapi.xcertplay.transport.VehicleStatusSnapshot

object ZeekrVehicleStatus {
    fun batteryProvider(context: Context): VehicleStatusProvider {
        val app = context.applicationContext
        return VehicleStatusProvider {
            ZeekrCarSignals.battery(app)?.let { b ->
                VehicleStatusSnapshot(
                    rangeKm = b.rangeKm,
                    rangeWarning = false,
                    batteryPercent = b.percent,
                    currentChargeWh = null,
                    maxChargeWh = null,
                    maxRangeKm = 0,
                    charging = b.charging,
                )
            }
        }
    }

    /** true = parked (P), false = a drive gear, null = unknown. */
    fun parked(context: Context): Boolean? =
        ZeekrCarSignals.gear(context.applicationContext)?.let { it == VehicleGear.PARK }
}
