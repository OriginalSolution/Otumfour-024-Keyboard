package gh.otumfour.o24.ui

import android.content.Context
import gh.otumfour.o24.data.SettingsStore

/** Small bridge so onboarding can mark itself finished without a second store type. */
object SettingsStoreHolder {
    fun markDone(context: Context) {
        SettingsStore(context).setOnboarded()
    }
}
