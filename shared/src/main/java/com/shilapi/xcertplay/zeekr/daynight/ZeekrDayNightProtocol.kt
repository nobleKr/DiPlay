/*
 * DiPlay — Zeekr adaptation. GPLv3 (see LICENSE).
 * CoreService (IZeekrCoreService) day/night binder client, domain device/dayNight.
 * Mirrors the engine reference: getDayNight / register / unregister + callback.
 */
package com.shilapi.xcertplay.zeekr.daynight

import android.os.Binder
import android.os.IBinder
import android.os.Parcel

class ZeekrDayNightProtocol(private val remote: IBinder) {
    private val serviceCallback = object : Binder() {
        init { attachInterface(null, SERVICE_CALLBACK_DESCRIPTOR) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) { reply?.writeString(SERVICE_CALLBACK_DESCRIPTOR); return true }
            if (code in 1..3) { reply?.writeNoException(); return true }
            return super.onTransact(code, data, reply, flags)
        }
    }

    fun initialize() {
        registerServiceCallback()
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(CORE_DESCRIPTOR)
            data.writeStringArray(arrayOf("device"))
            if (!remote.transact(1, data, reply, 0)) throw android.os.RemoteException("CoreService init unsupported")
            reply.readException()
        } finally { reply.recycle(); data.recycle() }
    }

    private fun registerServiceCallback() {
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(CORE_DESCRIPTOR)
            data.writeStrongBinder(serviceCallback)
            if (!remote.transact(2, data, reply, 0)) throw android.os.RemoteException("CoreService callback unsupported")
            reply.readException()
        } finally { reply.recycle(); data.recycle() }
    }

    fun night(): Boolean? = decodeNight(call("getDayNight", null))
    fun register(callback: IBinder): Boolean = protoReadBoolean(call("registerDayNightMode", callback))
    fun unregister(callback: IBinder) { call("unRegisterDayNightMode", callback) }

    private fun call(method: String, callback: IBinder?): ByteArray? {
        val data = Parcel.obtain(); val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(CORE_DESCRIPTOR)
            data.writeInt(1)
            data.writeString("device")
            data.writeString("dayNight")
            data.writeString(method)
            data.writeByteArray(null)
            data.writeByteArray(null)
            if (callback != null) data.writeStrongBinder(callback)
            if (!remote.transact(if (callback == null) 4 else 6, data, reply, 0)) return null
            reply.readException()
            if (reply.readInt() == 0) return null
            val code = reply.readInt(); reply.readString()
            val first = reply.createByteArray()
            if (code != 200) return null
            if (reply.dataAvail() == 0) return first
            val type = reply.readString() ?: return first
            if (type != "com.zeekr.sdk.base.msg.ServiceRetMessage") return null
            val serviceCode = reply.readInt(); reply.readString()
            val result = reply.createByteArray()
            return if (serviceCode == 200) result else null
        } finally { reply.recycle(); data.recycle() }
    }

    /** Server-pushed day/night callback (IDayNightChangeCallBack). */
    class Callback(private val onNight: (Boolean?) -> Unit, private val onMode: () -> Unit) : Binder() {
        init { attachInterface(null, CALLBACK_DESCRIPTOR) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) { reply?.writeString(CALLBACK_DESCRIPTOR); return true }
            if (code != 1 && code != 2) return super.onTransact(code, data, reply, flags)
            data.enforceInterface(CALLBACK_DESCRIPTOR)
            val value = data.readInt()
            if (code == 1) onMode() else onNight(nightValue(value))
            reply?.writeNoException()
            return true
        }
    }

    companion object {
        const val CORE_DESCRIPTOR = "com.zeekr.sdk.base.internal.IZeekrCoreService"
        const val CALLBACK_DESCRIPTOR = "com.zeekr.sdk.openapi.IDayNightChangeCallBack"
        const val SERVICE_CALLBACK_DESCRIPTOR = "com.zeekr.sdk.base.internal.IServiceCallback"
        private const val INTERFACE_TRANSACTION = 1598968902 // ('_','N','T','F')

        fun nightValue(value: Int): Boolean? = when (value) { 1 -> false; 2 -> true; else -> null }
        fun decodeNight(bytes: ByteArray?): Boolean? =
            if (bytes != null && bytes.size == 2 && bytes[0].toInt() == 8) nightValue(bytes[1].toInt()) else null
        /** protobuf field1 varint != 0 (ZeekrCommonMsg bool). */
        fun protoReadBoolean(b: ByteArray?): Boolean =
            b != null && b.size >= 2 && (b[0].toInt() and 255) == 8 && b[1].toInt() != 0
    }
}
