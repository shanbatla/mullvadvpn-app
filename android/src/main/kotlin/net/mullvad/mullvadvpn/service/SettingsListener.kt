package net.mullvad.mullvadvpn.service

import net.mullvad.mullvadvpn.model.Settings
import net.mullvad.talpid.util.EventNotifier

class SettingsListener(val daemon: MullvadDaemon, val initialSettings: Settings) {
    var settings: Settings = initialSettings
        private set(value) {
            settingsNotifier.notify(value)
            field = value
        }

    val accountNumberNotifier = EventNotifier(initialSettings.accountToken)
    val dnsOptionsNotifier = EventNotifier(initialSettings.tunnelOptions.dnsOptions)
    val settingsNotifier: EventNotifier<Settings> = EventNotifier(settings)

    init {
        daemon.onSettingsChange.subscribe(this) { maybeSettings ->
            maybeSettings?.let { settings -> handleNewSettings(settings) }
        }
    }

    fun onDestroy() {
        daemon.onSettingsChange.unsubscribe(this)

        accountNumberNotifier.unsubscribeAll()
        settingsNotifier.unsubscribeAll()
    }

    fun subscribe(id: Any, listener: (Settings) -> Unit) {
        settingsNotifier.subscribe(id, listener)
    }

    fun unsubscribe(id: Any) {
        settingsNotifier.unsubscribe(id)
    }

    private fun handleNewSettings(newSettings: Settings) {
        synchronized(this) {
            if (settings.accountToken != newSettings.accountToken) {
                accountNumberNotifier.notify(newSettings.accountToken)
            }

            if (settings.tunnelOptions.dnsOptions != newSettings.tunnelOptions.dnsOptions) {
                dnsOptionsNotifier.notify(newSettings.tunnelOptions.dnsOptions)
            }

            settings = newSettings
        }
    }
}
