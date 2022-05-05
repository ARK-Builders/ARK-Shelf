package space.taran.arkshelf.di

import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module
import space.taran.arkshelf.data.LinkLocalDataSource
import space.taran.arkshelf.data.LinkRemoteDataSource
import space.taran.arkshelf.data.LinkRepoImpl
import space.taran.arkshelf.data.UserPreferencesImpl
import space.taran.arkshelf.data.network.NetworkStatus
import space.taran.arkshelf.data.network.NetworkStatusImpl
import space.taran.arkshelf.data.network.OkHttpClientBuilder
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences
import space.taran.arkshelf.presentation.folderpicker.FolderPickerViewModel
import space.taran.arkshelf.presentation.searchedit.SearchEditViewModel
import space.taran.arkshelf.presentation.settings.SettingsViewModel

val KOIN_MODULES by lazy {
    listOf(linkModule, viewModelsModule)
}

private val viewModelsModule = module {
    viewModel { SearchEditViewModel(get(), get()) }
    viewModel { FolderPickerViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}

private val linkModule = module {
    single { LinkRemoteDataSource(get(), get()) }
    single { LinkLocalDataSource() }
    single<NetworkStatus> { NetworkStatusImpl(get()) }
    single<UserPreferences> { UserPreferencesImpl(get()) }
    single<LinkRepo> { LinkRepoImpl(get(), get(), get()) }
    single { OkHttpClientBuilder.build() }
}