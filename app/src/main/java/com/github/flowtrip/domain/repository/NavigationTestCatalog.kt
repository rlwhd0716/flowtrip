package com.github.flowtrip.domain.repository

import com.github.flowtrip.domain.model.Destination

/** Explicit real-navigation test destinations, separate from recommendations. */
interface NavigationTestCatalog {
    val destinations: List<Destination>
}
