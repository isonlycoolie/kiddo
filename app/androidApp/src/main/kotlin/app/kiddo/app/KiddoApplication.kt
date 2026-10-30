package app.kiddo.app

import android.app.Application
import app.kiddo.core.model.AppInfo
import org.koin.android.ext.koin.androidContext

class KiddoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKiddo(androidPlatform, AppInfo(BuildConfig.VERSION_NAME, SOURCE_URL, AUTHOR_URL, AUTHOR)) {
            androidContext(this@KiddoApplication)
        }
        InboxJob.schedule(this)
    }
}
