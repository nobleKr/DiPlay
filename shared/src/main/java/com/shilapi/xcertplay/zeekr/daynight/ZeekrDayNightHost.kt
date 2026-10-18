/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 * Starts the Zeekr day/night source only on a detected Zeekr unit and feeds the
 * result to a callback (DiPlay's night-mode controller). No-op elsewhere.
 */
package com.shilapi.xcertplay.zeekr.daynight

import android.content.Context
import com.shilapi.xcertplay.hud.HudBackends

object ZeekrDayNightHost {
    private var source: ZeekrDayNight? = null

    @Synchronized
    fun start(context: Context, onNight: (Boolean) -> Unit) {
        if (source != null) return
        if (HudBackends.select(context).id != "zeekr") return
        source = ZeekrDayNight(context.applicationContext, onNight).also { it.start() }
    }

    @Synchronized
    fun stop() { source?.close(); source = null }
}
