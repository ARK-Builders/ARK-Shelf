package dev.arkbuilders.arkshelf.di.module

import dagger.Binds
import dagger.Module
import dev.arkbuilders.arkshelf.data.LinkRepoImpl
import dev.arkbuilders.arkshelf.data.UserPreferencesImpl
import dev.arkbuilders.arkshelf.data.network.NetworkStatus
import dev.arkbuilders.arkshelf.data.network.NetworkStatusImpl
import dev.arkbuilders.arkshelf.domain.LinkRepo
import dev.arkbuilders.arkshelf.domain.UserPreferences
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