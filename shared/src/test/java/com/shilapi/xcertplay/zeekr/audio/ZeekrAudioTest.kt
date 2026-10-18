package com.shilapi.xcertplay.zeekr.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZeekrAudioTest {
    private class FakeAmp(var current: String?) : PhoneSpeakerAmp {
        val writes = mutableListOf<String>()
        override fun read() = current
        override fun write(value: String): Boolean { current = value; writes.add(value); return true }
    }

    @Test fun enumsFromId() {
        assertEquals(PhoneSpeakers.ALL, PhoneSpeakers.fromId(4))
        assertEquals(PhoneSpeakers.AUTO, PhoneSpeakers.fromId(99))
        assertEquals(AudioRoute.PHONE, AudioRoute.fromId(3))
    }

    @Test fun routerSavesAndRestores() {
        val amp = FakeAmp("driver_default")
        val r = PhoneSpeakerRouter(amp)
        val s = Any()
        r.start(s, PhoneSpeakers.ALL)
        assertEquals("all", amp.current)
        r.stop(s)
        assertEquals("restored previous amp value", "driver_default", amp.current)
    }

    @Test fun autoIsNoOp() {
        val amp = FakeAmp("x")
        PhoneSpeakerRouter(amp).start(Any(), PhoneSpeakers.AUTO)
        assertEquals(0, amp.writes.size)
    }

    @Test fun secondStreamKeepsRoutingUntilAllStop() {
        val amp = FakeAmp("base")
        val r = PhoneSpeakerRouter(amp)
        val a = Any(); val b = Any()
        r.start(a, PhoneSpeakers.DRIVER)
        r.start(b, PhoneSpeakers.ALL)
        r.stop(b)
        assertEquals("driver", amp.current) // falls back to remaining stream
        r.stop(a)
        assertEquals("base", amp.current)   // restored
    }
}
