package com.github.flowtrip.core.di

import com.github.flowtrip.domain.repository.RecommendationRepository
import com.github.flowtrip.domain.usecase.GetRecommendationsUseCase
import com.github.flowtrip.domain.usecase.RequestNavigationUseCase
import com.github.flowtrip.domain.usecase.ObserveLocationUseCase
import com.github.flowtrip.domain.repository.NavigationRepository
import com.github.flowtrip.domain.repository.LocationRepository
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Domain types stay independent of Hilt and Android annotations.
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideRequestNavigation(navigation: NavigationRepository, ux: UxRestrictionsRepository) =
        RequestNavigationUseCase(navigation, ux)

    @Provides
    fun provideObserveLocation(location: LocationRepository) = ObserveLocationUseCase(location)

    @Provides
    fun provideGetRecommendationsUseCase(repository: RecommendationRepository) =
        GetRecommendationsUseCase(repository)
}
