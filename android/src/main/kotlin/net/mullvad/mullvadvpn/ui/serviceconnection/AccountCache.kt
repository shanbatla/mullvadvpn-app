package net.mullvad.mullvadvpn.ui.serviceconnection

import android.os.Messenger
import kotlinx.coroutines.CompletableDeferred
import net.mullvad.mullvadvpn.service.Event
import net.mullvad.mullvadvpn.service.Request
import net.mullvad.talpid.util.EventNotifier
import org.joda.time.DateTime

class AccountCache(val connection: Messenger, val eventDispatcher: EventDispatcher) {
    val onAccountNumberChange = EventNotifier<String?>(null)
    val onAccountExpiryChange = EventNotifier<DateTime?>(null)
    val onAccountHistoryChange = EventNotifier<ArrayList<String>>(ArrayList())

    private var accountNumber by onAccountNumberChange.notifiable()
    private var accountExpiry by onAccountExpiryChange.notifiable()
    private var accountHistory by onAccountHistoryChange.notifiable()

    private var createdAccountNumber: CompletableDeferred<String?>? = null

    var newlyCreatedAccount = false
        private set

    init {
        eventDispatcher.apply {
            registerHandler(Event.Type.AccountNumber) { event: Event.AccountNumber ->
                accountNumber = event.account
                createdAccountNumber?.complete(event.account)
            }

            registerHandler(Event.Type.AccountExpiry) { event: Event.AccountExpiry ->
                accountExpiry = event.expiry
            }

            registerHandler(Event.Type.AccountHistory) { event: Event.AccountHistory ->
                accountHistory = event.history ?: ArrayList()
            }

            registerHandler(Event.Type.NewAccountStatus) { event: Event.NewAccountStatus ->
                newlyCreatedAccount = event.isNew
            }
        }
    }

    suspend fun createNewAccount(): String? {
        createdAccountNumber = CompletableDeferred()
        connection.send(Request.CreateAccount().message)

        val account = createdAccountNumber?.await()

        createdAccountNumber = null

        return account
    }

    fun login(account: String) {
        connection.send(Request.Login(account).message)
    }

    fun fetchAccountExpiry() {
        connection.send(Request.FetchAccountExpiry().message)
    }

    fun invalidateAccountExpiry(accountExpiryToInvalidate: DateTime) {
        val request = Request.InvalidateAccountExpiry(accountExpiryToInvalidate)

        connection.send(request.message)
    }

    fun removeAccountFromHistory(account: String) {
        connection.send(Request.RemoveAccountFromHistory(account).message)
    }
}
