package space.taran.arkshelf.di.module

import dagger.Module
import dagger.Provides
import space.taran.arkshelf.data.network.OkHttpClientBuilder
import javax.inject.Singleton

@Module
class NetworkModule {
    @Singleton
    @Provides
    fun okHttpClient() = OkHttpClientBuilder.build()
}