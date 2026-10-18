/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * Picks the instrument-cluster / HUD presentation display on a Zeekr 001 DHU.
 * Pure open Android API (DisplayManager). Written for the fork; no third-party
 * code. Device-specific constants are marked TODO and must be confirmed from
 * `dumpsys display` / `dumpsys SurfaceFlinger --display-id` on the car.
 */
package com.shilapi.xcertplay.hud.zeekr

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display

object ZeekrDisplayRouter {
    private const val TAG = "Zeekr-Display"

    // TODO(device): confirm from dumpsys. These are the expected cluster/HUD
    // surface sizes on the 001; leave null to fall back to name/size heuristics.
    private val CLUSTER_SIZE: Point? = null // e.g. Point(1920, 720)
    private val HUD_SIZE: Point = Point(780, 244) // HAB frame size used by the HUD channel

    /** Candidate presentation displays, main display (id 0) excluded. */
    fun presentationDisplays(context: Context): List<Display> {
        val dm = context.getSystemService(DisplayManager::class.java) ?: return emptyList()
        val byCategory = dm.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).toList()
        val all = dm.displays.toList()
        return (byCategory + all).distinctBy { it.displayId }.filter { it.displayId != Display.DEFAULT_DISPLAY }
    }

    /** Best guess for the cluster (center/driver) display, or null. */
    fun clusterDisplay(context: Context): Display? {
        val candidates = presentationDisplays(context)
        // 1) exact size match when we know it
        CLUSTER_SIZE?.let { want ->
            candidates.firstOrNull { sizeOf(it) == want }?.let { return logPick("size", it) }
        }
        // 2) name hint
        candidates.firstOrNull { nameHints(it, "cluster", "instrument") }?.let { return logPick("name", it) }
        // 3) fallback: largest non-default presentation display
        return candidates.maxByOrNull { val p = sizeOf(it); p.x.toLong() * p.y }?.let { logPick("largest", it) }
    }

    /** Best guess for the HUD (windshield) display, or null. */
    fun hudDisplay(context: Context): Display? {
        val candidates = presentationDisplays(context)
        candidates.firstOrNull { sizeOf(it) == HUD_SIZE }?.let { return logPick("hud-size", it) }
        candidates.firstOrNull { nameHints(it, "hud", "head-up", "headup", "secondary") }?.let { return logPick("hud-name", it) }
        return null
    }

    private fun sizeOf(display: Display): Point = Point().also { display.getRealSize(it) }

    private fun nameHints(display: Display, vararg hints: String): Boolean {
        val n = display.name?.lowercase() ?: return false
        return hints.any { n.contains(it) }
    }

    private fun logPick(how: String, d: Display): Display {
        Log.i(TAG, "picked display by $how: id=${d.displayId} name=${d.name} size=${sizeOf(d)}")
        return d
    }
}
