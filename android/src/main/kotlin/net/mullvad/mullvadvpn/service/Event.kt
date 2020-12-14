package net.mullvad.mullvadvpn.service

import android.os.Bundle
import android.os.Message
import net.mullvad.mullvadvpn.model.GeoIpLocation

sealed class Event {
    abstract val type: Type

    val message: Message
        get() = Message.obtain().apply {
            what = type.ordinal
            data = Bundle()

            prepareData(data)
        }

    open fun prepareData(data: Bundle) {}

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

    enum class Type(val build: (Bundle) -> Event) {
        NewLocation({ data -> NewLocation(data) }),
    }

    companion object {
        fun fromMessage(message: Message): Event {
            val type = Type.values()[message.what]

            return type.build(message.data)
        }
    }
}
