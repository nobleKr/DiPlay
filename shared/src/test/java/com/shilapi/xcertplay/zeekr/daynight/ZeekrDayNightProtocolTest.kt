package com.shilapi.xcertplay.zeekr.daynight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeekrDayNightProtocolTest {
    @Test fun nightValueMapping() {
        assertEquals(false, ZeekrDayNightProtocol.nightValue(1))
        assertEquals(true, ZeekrDayNightProtocol.nightValue(2))
        assertNull(ZeekrDayNightProtocol.nightValue(0))
    }
    @Test fun decodeNightFromProtobuf() {
        assertEquals(true, ZeekrDayNightProtocol.decodeNight(byteArrayOf(8, 2)))
        assertEquals(false, ZeekrDayNightProtocol.decodeNight(byteArrayOf(8, 1)))
        assertNull(ZeekrDayNightProtocol.decodeNight(byteArrayOf(8)))
        assertNull(ZeekrDayNightProtocol.decodeNight(null))
    }
    @Test fun protoReadBoolean() {
        assertTrue(ZeekrDayNightProtocol.protoReadBoolean(byteArrayOf(8, 1)))
        assertFalse(ZeekrDayNightProtocol.protoReadBoolean(byteArrayOf(8, 0)))
        assertFalse(ZeekrDayNightProtocol.protoReadBoolean(null))
    }
}
