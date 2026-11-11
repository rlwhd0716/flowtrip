package com.github.flowtrip.core.di

import com.github.flowtrip.data.repository.MockRecommendationRepository
import com.github.flowtrip.data.repository.OfficialNavigationTestCatalog
import com.github.flowtrip.domain.repository.NavigationTestCatalog
import com.github.flowtrip.domain.repository.RecommendationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindNavigationTestCatalog(implementation: OfficialNavigationTestCatalog): NavigationTestCatalog

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(
        implementation: MockRecommendationRepository,
    ): RecommendationRepository
}
