/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 * Audio routing to the Zeekr car amp, mirroring the engine reference:
 * phone-audio speaker selection via android.car ZeekrCarAudioManager
 * (setParameterToAmp "playback_for_phone"), with save/restore, plus the
 * per-route AudioAttributes policy.
 */
package com.shilapi.xcertplay.zeekr.audio

import android.content.Context
import android.media.AudioAttributes

enum class PhoneSpeakers(val id: Int, val value: String?) {
    AUTO(0, null), DRIVER(1, "driver"), FRONT_ALL(2, "front_all"),
    REAR_ALL(3, "rear_all"), ALL(4, "all");
    companion object { fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: AUTO }
}

enum class AudioRoute(val id: Int) {
    AUTO(0), MEDIA(1), NAVIGATION(2), PHONE(3), ASSISTANT(4);
    companion object { fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: AUTO }
}

interface PhoneSpeakerAmp {
    fun read(): String?
    fun write(value: String): Boolean
}

/** android.car "audio" manager (android.car.zeekr.ZeekrCarAudioManager) via reflection. */
class ZeekrCarAmp(context: Context) : PhoneSpeakerAmp {
    private val manager: Any? = runCatching {
        val car = Class.forName("android.car.Car")
        val instance = car.getMethod("createCar", Context::class.java).invoke(null, context.applicationContext)
        car.getMethod("getCarManager", String::class.java).invoke(instance, "audio")
    }.getOrNull()?.takeIf { it.javaClass.name == "android.car.zeekr.ZeekrCarAudioManager" }

    override fun read(): String? = manager?.let { m ->
        runCatching { m.javaClass.getMethod("getParameterToAmp", String::class.java).invoke(m, KEY) as? String }.getOrNull()
    }

    override fun write(value: String): Boolean = manager?.let { m ->
        runCatching {
            m.javaClass.getMethod("setParameterToAmp", String::class.java, String::class.java).invoke(m, KEY, value)
            true
        }.getOrDefault(false)
    } ?: false

    companion object { const val KEY = "playback_for_phone" }
}

/** Routes phone audio to the requested speakers while a stream is active; restores on stop. */
class PhoneSpeakerRouter(private val amp: PhoneSpeakerAmp?) {
    constructor(context: Context) : this(ZeekrCarAmp(context))
    private val active = LinkedHashMap<Any, PhoneSpeakers>()
    private var saved: String? = null

    @Synchronized
    fun start(stream: Any, speakers: PhoneSpeakers) {
        val amp = amp ?: return
        if (speakers == PhoneSpeakers.AUTO) return
        val value = speakers.value ?: return
        if (active.isEmpty()) saved = amp.read()
        active[stream] = speakers
        amp.write(value)
    }

    @Synchronized
    fun stop(stream: Any) {
        val amp = amp ?: return
        active.remove(stream)
        val next = active.values.lastOrNull()?.value
        when {
            next != null -> amp.write(next)
            else -> saved?.let { amp.write(it); saved = null }
        }
    }
}

/** Per-route AudioAttributes (usage/contentType) as the engine reference builds them. */
object ZeekrAudioPolicy {
    fun media(): AudioAttributes = AudioAttributes.Builder().setUsage(1).setContentType(2).build()
    fun navigation(): AudioAttributes = AudioAttributes.Builder().setUsage(12).setContentType(1).build()
    fun call(): AudioAttributes = AudioAttributes.Builder().setUsage(2).setContentType(1).build()
    fun assistant(): AudioAttributes = AudioAttributes.Builder().setUsage(11).setContentType(1).build()

    fun attributesFor(route: AudioRoute): AudioAttributes = when (route) {
        AudioRoute.NAVIGATION -> navigation()
        AudioRoute.PHONE -> call()
        AudioRoute.ASSISTANT -> assistant()
        AudioRoute.MEDIA, AudioRoute.AUTO -> media()
    }
}
