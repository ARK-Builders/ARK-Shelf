package dev.arkbuilders.arkshelf.presentation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.arkbuilders.arkfilepicker.folders.FoldersRepo
import dev.arkbuilders.arklib.initArkLib
import dev.arkbuilders.arklib.initRustLogger
import dev.arkbuilders.arkshelf.di.DIManager
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