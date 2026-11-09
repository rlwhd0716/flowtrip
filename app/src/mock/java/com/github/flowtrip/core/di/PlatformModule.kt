package com.github.flowtrip.core.di

import android.content.Context
import android.content.pm.PackageManager
import com.github.flowtrip.data.repository.*
import com.github.flowtrip.domain.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlatformModule {
    @Provides @Singleton fun navigation(): NavigationRepository = MockNavigationRepository()
    @Provides @Singleton fun location(): LocationRepository = MockLocationRepository()
    @Provides @Singleton fun ux(@ApplicationContext context: Context): UxRestrictionsRepository =
        MockUxRestrictionsRepository(context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
}
