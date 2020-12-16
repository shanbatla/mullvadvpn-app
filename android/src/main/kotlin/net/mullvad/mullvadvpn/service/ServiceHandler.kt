package net.mullvad.mullvadvpn.service

import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlin.properties.Delegates.observable

class ServiceHandler(looper: Looper, val locationInfoCache: LocationInfoCache) : Handler(looper) {
    private val listeners = mutableListOf<Messenger>()

    val accountCache = AccountCache()

    val keyStatusListener = KeyStatusListener().apply {
        onKeyStatusChange.subscribe(this@ServiceHandler) { keyStatus ->
            sendEvent(Event.WireGuardKeyStatus(keyStatus))
        }
    }

    var daemon by observable<MullvadDaemon?>(null) { _, _, newDaemon ->
        accountCache.daemon = newDaemon
        keyStatusListener.daemon = newDaemon
    }

    var settingsListener by observable<SettingsListener?>(null) { _, oldListener, newListener ->
        oldListener?.apply {
            unsubscribe(this@ServiceHandler)
            onDestroy()
        }

        newListener?.subscribe(this@ServiceHandler) { settings ->
            sendEvent(Event.SettingsUpdate(settings))
        }

        accountCache.accountListener = newListener?.accountNumberNotifier
    }

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
            is Request.WireGuardGenerateKey -> keyStatusListener.generateKey()
            is Request.WireGuardVerifyKey -> keyStatusListener.verifyKey()
        }
    }

    private fun registerListener(listener: Messenger) {
        listeners.add(listener)

        listener.apply {
            send(Event.SettingsUpdate(settingsListener?.settings).message)
            send(Event.NewLocation(locationInfoCache.location).message)
            send(Event.WireGuardKeyStatus(keyStatusListener.keyStatus).message)
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
