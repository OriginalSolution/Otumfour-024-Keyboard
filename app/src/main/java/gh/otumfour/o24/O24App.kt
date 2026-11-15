package gh.otumfour.o24

import android.app.Application
import android.os.StrictMode
import android.util.Log

class O24App : Application() {
    override fun onCreate() {
        super.onCreate()
        val app = this
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            Log.e(TAG, "Uncaught exception on ${thread.name}", error)
            runCatching { CrashReport.save(app, error) }
            previous?.uncaughtException(thread, error)
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build(),
            )
        }
    }

    private companion object {
        const val TAG = "O24"
    }
}
