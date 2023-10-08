package dev.arkbuilders.arkshelf.di

import android.app.Application
import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dev.arkbuilders.arkshelf.di.module.RepoModule
import dev.arkbuilders.arkshelf.presentation.searchedit.SearchEditFragment
import dev.arkbuilders.arkshelf.presentation.settings.SettingsFragment
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        RepoModule::class,
    ]
)
interface AppComponent {
    fun inject(searchEditFragment: SearchEditFragment)
    fun inject(searchEditFragment: SettingsFragment)

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance context: Context,
        ): AppComponent
    }
}