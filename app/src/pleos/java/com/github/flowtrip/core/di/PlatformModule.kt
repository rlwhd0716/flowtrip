package com.github.flowtrip.core.di

import android.content.Context
import android.content.pm.PackageManager
import com.github.flowtrip.core.platform.CarUxRestrictionsRepository
import com.github.flowtrip.core.platform.UnavailableUxRestrictionsRepository
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
    @Provides @Singleton fun pleosNavigation(@ApplicationContext context: Context, ux: UxRestrictionsRepository) =
        PleosNavigationRepository(context, ux)
    @Provides fun navigation(implementation: PleosNavigationRepository): NavigationRepository = implementation
    @Provides @Singleton fun location(@ApplicationContext context: Context, navigation: PleosNavigationRepository): LocationRepository =
        PleosLocationRepository(context, navigation)
    @Provides @Singleton fun ux(@ApplicationContext context: Context): UxRestrictionsRepository =
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) CarUxRestrictionsRepository(context)
        else UnavailableUxRestrictionsRepository()
}
