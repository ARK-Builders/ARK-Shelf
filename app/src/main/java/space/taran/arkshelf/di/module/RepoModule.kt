package space.taran.arkshelf.di.module

import dagger.Binds
import dagger.Module
import space.taran.arkshelf.data.LinkRepoImpl
import space.taran.arkshelf.data.UserPreferencesImpl
import space.taran.arkshelf.data.network.NetworkStatus
import space.taran.arkshelf.data.network.NetworkStatusImpl
import space.taran.arkshelf.domain.LinkRepo
import space.taran.arkshelf.domain.UserPreferences
import javax.inject.Singleton

@Module
interface RepoModule {

    @Singleton
    @Binds
    fun linkRepo(linkRepoImpl: LinkRepoImpl): LinkRepo

    @Singleton
    @Binds
    fun preferences(preferencesImpl: UserPreferencesImpl): UserPreferences

    @Singleton
    @Binds
    fun networkStatus(networkStatusImpl: NetworkStatusImpl): NetworkStatus
}