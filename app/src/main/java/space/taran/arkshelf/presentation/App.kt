package space.taran.arkshelf.presentation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.initArkLib
import space.taran.arklib.initRustLogger
import space.taran.arkshelf.di.DIManager
import timber.log.Timber

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("arklib")
        FoldersRepo.init(this)
        initArkLib()
        initRustLogger()
        Timber.plant(Timber.DebugTree())

        instance = this

        DIManager.init(this)
    }
}