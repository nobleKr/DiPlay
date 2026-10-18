/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * Wires the ported MediaBridge to DiPlay's CarPlay MediaSession, only on a
 * detected Zeekr unit. On other platforms this is a no-op, so the call sites in
 * CarPlayMediaKeys stay platform-agnostic. The bridge binds to the Zeekr media
 * centre and mirrors the CarPlay now-playing MediaController to it.
 */
package com.shilapi.xcertplay.zeekr.mediabridge

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.util.Log
import com.shilapi.xcertplay.hud.HudBackends

object ZeekrMediaBridgeHost {
    private const val TAG = "Zeekr-MediaBridge"
    private var bridge: MediaBridge? = null

    @Synchronized
    fun onSessionStarted(context: Context, session: MediaSession) {
        if (bridge != null) return
        if (HudBackends.select(context).id != "zeekr") return
        runCatching {
            MediaBridge(context.applicationContext).also { b ->
                b.bind()
                b.attachController(MediaController(context, session.sessionToken))
                bridge = b
            }
            Log.i(TAG, "attached CarPlay session to Zeekr media centre")
        }.onFailure { Log.w(TAG, "media bridge attach failed", it) }
    }

    @Synchronized
    fun onSessionReleased() {
        bridge?.let { runCatching { it.teardown() } }
        bridge = null
    }
}
