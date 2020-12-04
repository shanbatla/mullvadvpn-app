package net.mullvad.mullvadvpn.ui.serviceconnection

import android.os.Messenger
import kotlin.properties.Delegates.observable
import net.mullvad.mullvadvpn.model.GeoIpLocation
import net.mullvad.mullvadvpn.relaylist.Relay
import net.mullvad.mullvadvpn.relaylist.RelayCity
import net.mullvad.mullvadvpn.relaylist.RelayCountry
import net.mullvad.mullvadvpn.relaylist.RelayItem
import net.mullvad.mullvadvpn.service.Event
import net.mullvad.mullvadvpn.service.Request

class LocationInfoCache(val connection: Messenger, val eventDispatcher: EventDispatcher) {
    private var location: GeoIpLocation? by observable(null) { _, _, newLocation ->
        onNewLocation?.invoke(newLocation)
    }

    var onNewLocation by observable<((GeoIpLocation?) -> Unit)?>(null) { _, _, callback ->
        callback?.invoke(location)
    }

    var selectedRelay by observable<RelayItem?>(null) { _, oldRelay, newRelay ->
        if (newRelay != oldRelay) {
            updateSelectedRelayLocation(newRelay)
        }
    }

    init {
        eventDispatcher.registerHandler(Event.Type.NewLocation) { event: Event.NewLocation ->
            location = event.location
        }
    }

    private fun updateSelectedRelayLocation(relayItem: RelayItem?) {
        val selectedLocation = when (relayItem) {
            is RelayCountry -> GeoIpLocation(null, null, relayItem.name, null, null)
            is RelayCity -> GeoIpLocation(
                null,
                null,
                relayItem.country.name,
                relayItem.name,
                null
            )
            is Relay -> GeoIpLocation(
                null,
                null,
                relayItem.city.country.name,
                relayItem.city.name,
                relayItem.name
            )
            else -> null
        }

        val request = Request.SetSelectedRelay(selectedLocation)

        connection.send(request.message)
    }
}
