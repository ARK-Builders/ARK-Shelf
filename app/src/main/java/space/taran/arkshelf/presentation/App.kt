package space.taran.arkshelf.presentation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import space.taran.arkshelf.di.KOIN_MODULES
import java.nio.file.Files
import kotlin.io.path.Path

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

        cleanup()
    }

    private fun cleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            cacheDir.listFiles()?.forEach {
                it.deleteRecursively()
            }
        }
    }
}