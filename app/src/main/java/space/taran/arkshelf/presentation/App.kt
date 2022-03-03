package space.taran.arkshelf.presentation

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import space.taran.arkshelf.di.KOIN_MODULES

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        startKoin {
            androidContext(this@App)
            modules(KOIN_MODULES)
        }
    }
}