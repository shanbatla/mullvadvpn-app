package net.mullvad.mullvadvpn.service

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger

class ServiceHandler(looper: Looper, val locationInfoCache: LocationInfoCache) : Handler(looper) {
    private val listeners = mutableListOf<Messenger>()

    override fun handleMessage(message: Message) {
        val request = Request.fromMessage(message)

        when (request) {
            is Request.RegisterListener -> listeners.add(request.listener)
            is Request.SetSelectedRelay -> {
                locationInfoCache.selectedRelayLocation = request.relayLocation
            }
        }
    }
}
