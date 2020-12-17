package net.mullvad.mullvadvpn.service

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import net.mullvad.mullvadvpn.model.GeoIpLocation

sealed class Request {
    abstract val type: Type

    val message: Message
        get() = Message.obtain().apply {
            what = type.ordinal

            prepareMessage(this)
        }

    open fun prepareMessage(message: Message) {
        message.data = Bundle()
        prepareData(message.data)
    }

    open fun prepareData(data: Bundle) {}

    class RegisterListener(val listener: Messenger) : Request() {
        override val type = Type.RegisterListener

        override fun prepareMessage(message: Message) {
            message.replyTo = listener
        }
    }

    class SetSelectedRelay(val relayLocation: GeoIpLocation?) : Request() {
        companion object {
            private val relayLocationKey = "relayLocation"
        }

        override val type = Type.SetSelectedRelay

        constructor(data: Bundle) : this(data.getParcelable(relayLocationKey)) {}

        override fun prepareData(data: Bundle) {
            data.putParcelable(relayLocationKey, relayLocation)
        }
    }

    class WireGuardGenerateKey : Request() {
        override val type = Type.WireGuardGenerateKey
        override fun prepareMessage(message: Message) {}
    }

    class WireGuardVerifyKey : Request() {
        override val type = Type.WireGuardVerifyKey
        override fun prepareMessage(message: Message) {}
    }

    enum class Type(val build: (Message) -> Request) {
        RegisterListener({ message -> RegisterListener(message.replyTo) }),
        SetSelectedRelay({ message -> SetSelectedRelay(message.data) }),
        WireGuardGenerateKey({ _ -> WireGuardGenerateKey() }),
        WireGuardVerifyKey({ _ -> WireGuardVerifyKey() }),
    }

    companion object {
        fun fromMessage(message: Message): Request {
            val type = Type.values()[message.what]

            return type.build(message)
        }
    }
}
