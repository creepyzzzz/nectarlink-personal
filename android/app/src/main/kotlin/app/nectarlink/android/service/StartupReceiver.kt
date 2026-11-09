// SPDX-License-Identifier: GPL-3.0-or-later
package app.nectarlink.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.nectarlink.android.NectarlinkApplication
import app.nectarlink.android.core.CoreStatus
import app.nectarlink.android.ui.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Brings the connection back after the phone restarts or NectarLink is
 updated, without waiting for the app to be opened. Starting the core starts
 * the connection service when a PC is paired (Core.start).
 */
class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_BOOT_COMPLETED -> Unit
            else -> return
        }
        val app = context.applicationContext as NectarlinkApplication
        if (!Preferences.isBackgroundServiceEnabled(context)) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.core.start()
                // Kept alive until the core is up, so the service starts inside the
                // window Android allows after these broadcasts.
                withTimeoutOrNull(START_WAIT_MS) { app.core.state.first { it.status !is CoreStatus.Starting } }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val START_WAIT_MS = 8_000L
    }
}
