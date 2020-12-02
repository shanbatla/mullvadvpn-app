package net.mullvad.mullvadvpn.ui.serviceconnection

import android.os.Messenger
import kotlin.properties.Delegates.observable
import net.mullvad.mullvadvpn.model.GeoIpLocation
import net.mullvad.mullvadvpn.relaylist.Relay
import net.mullvad.mullvadvpn.relaylist.RelayCity
import net.mullvad.mullvadvpn.relaylist.RelayCountry
import net.mullvad.mullvadvpn.relaylist.RelayItem
import net.mullvad.mullvadvpn.service.Request

class LocationInfoCache(
    val connection: Messenger,
    val serviceCache: net.mullvad.mullvadvpn.service.LocationInfoCache
) {
    var onNewLocation
        get() = serviceCache.onNewLocation
        set(value) { serviceCache.onNewLocation = value }

    var selectedRelay by observable<RelayItem?>(null) { _, oldRelay, newRelay ->
        if (newRelay != oldRelay) {
            updateSelectedRelayLocation(newRelay)
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
