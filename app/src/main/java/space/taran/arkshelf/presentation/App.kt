package space.taran.arkshelf.presentation

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.acra.config.dialog
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import space.taran.arkfilepicker.folders.FoldersRepo
import space.taran.arklib.initArkLib
import space.taran.arklib.initRustLogger
import space.taran.arkshelf.BuildConfig
import space.taran.arkshelf.R
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
        initAcra()

        instance = this

        DIManager.init(this)
    }

    private fun initAcra() = CoroutineScope(Dispatchers.IO).launch {
        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            dialog {
                text = getString(R.string.crash_dialog_description)
                title = getString(R.string.crash_dialog_title)
                commentPrompt = getString(R.string.crash_dialog_comment)
            }
            httpSender {
                uri = BuildConfig.ACRA_URI
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}