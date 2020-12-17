package net.mullvad.mullvadvpn.service

import android.os.Bundle
import android.os.Message
import android.os.Messenger
import net.mullvad.mullvadvpn.model.GeoIpLocation
import org.joda.time.DateTime

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

    class CreateAccount : Request() {
        override val type = Type.CreateAccount
        override fun prepareMessage(message: Message) {}
    }

    class FetchAccountExpiry : Request() {
        override val type = Type.FetchAccountExpiry
        override fun prepareMessage(message: Message) {}
    }

    class InvalidateAccountExpiry(val expiry: DateTime) : Request() {
        companion object {
            private val expiryKey = "expiry"
        }

        override val type = Type.InvalidateAccountExpiry

        constructor(data: Bundle) : this(DateTime(data.getLong(expiryKey))) {}

        override fun prepareData(data: Bundle) {
            data.putLong(expiryKey, expiry.millis)
        }
    }

    class Login(val account: String?) : Request() {
        companion object {
            private val accountKey = "account"
        }

        override val type = Type.Login

        constructor(data: Bundle) : this(data.getString(accountKey)) {}

        override fun prepareData(data: Bundle) {
            data.putString(accountKey, account)
        }
    }

    class RemoveAccountFromHistory(val account: String?) : Request() {
        companion object {
            private val accountKey = "account"
        }

        override val type = Type.RemoveAccountFromHistory

        constructor(data: Bundle) : this(data.getString(accountKey)) {}

        override fun prepareData(data: Bundle) {
            data.putString(accountKey, account)
        }
    }

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
        CreateAccount({ _ -> CreateAccount() }),
        FetchAccountExpiry({ _ -> FetchAccountExpiry() }),
        InvalidateAccountExpiry({ message -> InvalidateAccountExpiry(message.data) }),
        Login({ message -> Login(message.data) }),
        RegisterListener({ message -> RegisterListener(message.replyTo) }),
        RemoveAccountFromHistory({ message -> RemoveAccountFromHistory(message.data) }),
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
