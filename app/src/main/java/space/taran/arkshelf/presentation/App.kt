package space.taran.arkshelf.presentation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arkshelf.di.DIManager

class App : Application() {

    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()

        FoldersRepo.init(this)

        instance = this

        DIManager.init(this)
    }
}