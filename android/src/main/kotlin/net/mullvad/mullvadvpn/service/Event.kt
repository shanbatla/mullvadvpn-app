package net.mullvad.mullvadvpn.service

import android.os.Bundle
import android.os.Message
import java.util.ArrayList
import net.mullvad.mullvadvpn.model.GeoIpLocation
import net.mullvad.mullvadvpn.model.KeygenEvent
import net.mullvad.mullvadvpn.model.Settings
import org.joda.time.DateTime

sealed class Event {
    abstract val type: Type

    val message: Message
        get() = Message.obtain().apply {
            what = type.ordinal
            data = Bundle()

            prepareData(data)
        }

    open fun prepareData(data: Bundle) {}

    class AccountExpiry(val expiry: DateTime?) : Event() {
        companion object {
            private val expiryKey = "expiry"

            fun buildExpiry(data: Bundle): DateTime? {
                val expiryInMilliseconds = data.getLong(expiryKey)

                if (expiryInMilliseconds == 0L) {
                    return null
                } else {
                    return DateTime(expiryInMilliseconds)
                }
            }
        }

        override val type = Type.AccountExpiry

        constructor(data: Bundle) : this(DateTime(data.getLong(expiryKey))) {}

        override fun prepareData(data: Bundle) {
            data.putLong(expiryKey, expiry?.millis ?: 0L)
        }
    }

    class AccountHistory(val history: ArrayList<String>?) : Event() {
        companion object {
            private val historyKey = "history"

            fun buildHistory(data: Bundle): ArrayList<String>? {
                return data.getStringArray(historyKey)?.let { historyArray ->
                    ArrayList(historyArray.toList())
                }
            }
        }

        override val type = Type.AccountHistory

        constructor(data: Bundle) : this(buildHistory(data)) {}

        override fun prepareData(data: Bundle) {
            data.putStringArray(historyKey, history?.toTypedArray())
        }
    }

    class AccountNumber(val account: String?) : Event() {
        companion object {
            private val accountKey = "account"
        }

        override val type = Type.AccountNumber

        constructor(data: Bundle) : this(data.getString(accountKey)) {}

        override fun prepareData(data: Bundle) {
            data.putString(accountKey, account)
        }
    }

    class NewLocation(val location: GeoIpLocation?) : Event() {
        companion object {
            private val locationKey = "location"
        }

        override val type = Type.NewLocation

        constructor(data: Bundle) : this(data.getParcelable(locationKey)) {}

        override fun prepareData(data: Bundle) {
            data.putParcelable(locationKey, location)
        }
    }

    class SettingsUpdate(val settings: Settings?) : Event() {
        companion object {
            private val settingsKey = "settings"
        }

        override val type = Type.SettingsUpdate

        constructor(data: Bundle) : this(data.getParcelable(settingsKey)) {}

        override fun prepareData(data: Bundle) {
            data.putParcelable(settingsKey, settings)
        }
    }

    class WireGuardKeyStatus(val keyStatus: KeygenEvent?) : Event() {
        companion object {
            private val keyStatusKey = "keyStatus"
        }

        override val type = Type.WireGuardKeyStatus

        constructor(data: Bundle) : this(data.getParcelable(keyStatusKey)) {}

        override fun prepareData(data: Bundle) {
            data.putParcelable(keyStatusKey, keyStatus)
        }
    }

    enum class Type(val build: (Bundle) -> Event) {
        AccountExpiry({ data -> AccountExpiry(data) }),
        AccountHistory({ data -> AccountHistory(data) }),
        AccountNumber({ data -> AccountNumber(data) }),
        NewLocation({ data -> NewLocation(data) }),
        SettingsUpdate({ data -> SettingsUpdate(data) }),
        WireGuardKeyStatus({ data -> WireGuardKeyStatus(data) }),
    }

    companion object {
        fun fromMessage(message: Message): Event {
            val type = Type.values()[message.what]

            return type.build(message.data)
        }
    }
}
