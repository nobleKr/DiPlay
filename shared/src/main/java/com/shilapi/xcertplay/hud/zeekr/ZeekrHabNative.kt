/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 *
 * JNI facade for the Zeekr HUD (HAB) frame channel. The native side
 * (jni/zeekr_hab.c) dlopen()s the vendor /system/lib64/libHabCamera.so and
 * pushes HardwareBuffers to it. This is our own shim — not the third-party
 * `com.pc.hablib`/`libhabmanager`.
 *
 * Mirrors the style of the existing NativeLinuxI2cBridge / LegacyHotspotNative.
 */
package com.shilapi.xcertplay.hud.zeekr

import android.hardware.HardwareBuffer
import android.util.Log

object ZeekrHabNative {
    private const val TAG = "Zeekr-HAB"
    @Volatile var available: Boolean = false; private set

    init {
        available = runCatching { System.loadLibrary("zeekr_hab") }
            .onFailure { Log.w(TAG, "libzeekr_hab unavailable: ${it.message}") }
            .isSuccess
    }

    // width/height/format of the HUD buffer channel. Returns a handle (>=0) or <0 on error.
    external fun open(width: Int, height: Int, format: Int): Long
    external fun sendHardwareBuffer(handle: Long, buffer: HardwareBuffer): Int
    external fun close(handle: Long): Int
}
