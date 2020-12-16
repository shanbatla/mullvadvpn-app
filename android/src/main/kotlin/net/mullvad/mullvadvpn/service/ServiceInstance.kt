package net.mullvad.mullvadvpn.service

import android.os.Messenger

class ServiceInstance(
    val messenger: Messenger,
    val daemon: MullvadDaemon,
    val connectionProxy: ConnectionProxy,
    val customDns: CustomDns,
    val settingsListener: SettingsListener,
    val splitTunneling: SplitTunneling
) {
    val accountCache = AccountCache().also { accountCache ->
        accountCache.daemon = daemon
        accountCache.accountListener = settingsListener.accountNumberNotifier
    }

    fun onDestroy() {
        accountCache.onDestroy()
        connectionProxy.onDestroy()
        customDns.onDestroy()
        settingsListener.onDestroy()
    }
}
