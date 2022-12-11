package space.taran.arkshelf.di.module

import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class ConfigModule {
    @Singleton
    @Provides
    @Named("PAGE_SIZE")
    fun pageSize() = 10
}