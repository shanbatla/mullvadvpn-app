package net.mullvad.mullvadvpn.service

import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger

class ServiceHandler(looper: Looper, val locationInfoCache: LocationInfoCache) : Handler(looper) {
    private val listeners = mutableListOf<Messenger>()

    init {
        locationInfoCache.onNewLocation = { location ->
            sendEvent(Event.NewLocation(location))
        }
    }

    override fun handleMessage(message: Message) {
        val request = Request.fromMessage(message)

        when (request) {
            is Request.RegisterListener -> registerListener(request.listener)
            is Request.SetSelectedRelay -> {
                locationInfoCache.selectedRelayLocation = request.relayLocation
            }
        }
    }

    private fun registerListener(listener: Messenger) {
        listeners.add(listener)

        listener.apply {
            send(Event.NewLocation(locationInfoCache.location).message)
        }
    }

    private fun sendEvent(event: Event) {
        val deadListeners = mutableListOf<Messenger>()

        for (listener in listeners) {
            try {
                listener.send(event.message)
            } catch (_: DeadObjectException) {
                deadListeners.add(listener)
            }
        }

        for (deadListener in deadListeners) {
            listeners.remove(deadListener)
        }
    }
}
