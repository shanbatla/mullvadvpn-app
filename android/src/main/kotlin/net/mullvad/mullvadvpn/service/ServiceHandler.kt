package net.mullvad.mullvadvpn.service

import android.os.DeadObjectException
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import kotlin.properties.Delegates.observable
import kotlinx.coroutines.runBlocking

class ServiceHandler(
    looper: Looper,
    val locationInfoCache: LocationInfoCache,
    val splitTunneling: SplitTunneling
) : Handler(looper) {
    private val listeners = mutableListOf<Messenger>()

    val accountCache = AccountCache().apply {
        onAccountNumberChange.subscribe(this@ServiceHandler) { account ->
            sendEvent(Event.AccountNumber(account))
        }

        onAccountExpiryChange.subscribe(this@ServiceHandler) { expiry ->
            sendEvent(Event.AccountExpiry(expiry))
        }

        onAccountHistoryChange.subscribe(this@ServiceHandler) { history ->
            sendEvent(Event.AccountHistory(history))
        }

        onNewAccountStatusChange = { isNew ->
            sendEvent(Event.NewAccountStatus(isNew))
        }
    }

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

        splitTunneling.onChange.subscribe(this) { excludedApps ->
            sendEvent(Event.SplitTunnelingUpdate(excludedApps))
        }
    }

    override fun handleMessage(message: Message) {
        val request = Request.fromMessage(message)

        when (request) {
            is Request.CreateAccount -> runBlocking { accountCache.createNewAccount() }
            is Request.ExcludeApp -> {
                request.packageName?.let { packageName ->
                    splitTunneling.excludeApp(packageName)
                }
            }
            is Request.FetchAccountExpiry -> accountCache.fetchAccountExpiry()
            is Request.IncludeApp -> {
                request.packageName?.let { packageName ->
                    splitTunneling.includeApp(packageName)
                }
            }
            is Request.InvalidateAccountExpiry -> {
                accountCache.invalidateAccountExpiry(request.expiry)
            }
            is Request.Login -> {
                request.account?.let { account ->
                    runBlocking { accountCache.login(account) }
                }
            }
            is Request.PersistExcludedApps -> splitTunneling.persist()
            is Request.RegisterListener -> registerListener(request.listener)
            is Request.RemoveAccountFromHistory -> {
                request.account?.let { account ->
                    accountCache.removeAccountFromHistory(account)
                }
            }
            is Request.SetEnableSplitTunneling -> splitTunneling.enabled = request.enable
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
