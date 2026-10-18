/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 * Binds com.zeekr.coreservice, tracks day/night, and reports changes. Mirrors
 * the engine reference: initialize + getDayNight + register callback, with
 * rebind on disconnect/death.
 */
package com.shilapi.xcertplay.zeekr.daynight

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

class ZeekrDayNight(context: Context, private val onChanged: (Boolean) -> Unit) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "zeekr-daynight").apply { isDaemon = true } }
    @Volatile private var connection: Conn? = null
    @Volatile private var started = false
    @Volatile private var closed = false
    private val retry = Runnable { if (!closed) bind() }

    private inner class Conn(val remote: IBinder) {
        val protocol = ZeekrDayNightProtocol(remote)
        @Volatile var registered = false
        val callback = ZeekrDayNightProtocol.Callback(
            onNight = { night -> night?.let { main.post { onChanged(it) } } },
            onMode = { worker.execute { runCatching { refresh(this) } } },
        )
        val death = IBinder.DeathRecipient { main.post { lost(this) } }
    }

    private val service = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            worker.execute {
                runCatching {
                    val c = Conn(binder); connection = c
                    binder.linkToDeath(c.death, 0)
                    c.protocol.initialize()
                    refresh(c)
                }.onFailure { Log.w(TAG, "connect failed", it); scheduleRetry() }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) { connection = null; scheduleRetry() }
        override fun onBindingDied(name: ComponentName) { connection = null; scheduleRetry() }
        override fun onNullBinding(name: ComponentName) { scheduleRetry() }
    }

    fun start() { if (closed || started) return; started = true; bind() }

    fun close() {
        if (closed) return
        closed = true
        main.removeCallbacks(retry)
        val c = connection; connection = null
        worker.execute {
            runCatching { if (c != null && c.registered) c.protocol.unregister(c.callback) }
            runCatching { c?.remote?.unlinkToDeath(c.death, 0) }
            runCatching { app.unbindService(service) }
        }
    }

    private fun bind() = runCatching {
        app.bindService(Intent("com.zeekr.intent.action.CoreService").setPackage("com.zeekr.coreservice"), service, Context.BIND_AUTO_CREATE)
    }.onFailure { scheduleRetry() }

    private fun refresh(c: Conn) {
        c.protocol.night()?.let { main.post { onChanged(it) } }
        if (!c.registered) c.registered = runCatching { c.protocol.register(c.callback) }.getOrDefault(false)
    }

    private fun lost(c: Conn) { if (connection === c) { connection = null; scheduleRetry() } }
    private fun scheduleRetry() { if (!closed) { main.removeCallbacks(retry); main.postDelayed(retry, 4000) } }

    companion object { private const val TAG = "ZeekrDayNight" }
}
