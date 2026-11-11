package com.github.flowtrip.data.repository

import com.github.flowtrip.domain.model.Coordinates
import com.github.flowtrip.domain.model.DataSource
import com.github.flowtrip.domain.model.Destination
import com.github.flowtrip.domain.repository.NavigationTestCatalog
import javax.inject.Inject

/**
 * Exact request payloads from Pleos' NaviHelper 3.0.5 sample, checked 2026-11-11:
 * https://github.com/Pleos-Playground/pleos-sample-navi-app/blob/main/app/src/main/java/ai/pleos/playground/navidevapp/data/sample/NaviTestData.kt
 * These are SDK test destinations, not dining/date recommendations or live POI search results.
 * Availability in the installed navigation app must still be confirmed by SDK callbacks.
 */
class OfficialNavigationTestCatalog @Inject constructor() : NavigationTestCatalog {
    override val destinations = listOf(
        Destination(
            id = "pleos-sample-42dot", name = "42dot",
            coordinates = Coordinates(37.48544722, 127.03636666), source = DataSource.PLEOS,
            address = "서울시 강남구 남부순환로 2621",
            poiId = "36khp37hh8gqcm5nqhe5", poiSubId = "0", poiProvider = "",
        ),
        Destination(
            id = "pleos-sample-software-dream-center", name = "소프트웨어드림센터",
            coordinates = Coordinates(37.41275833, 127.09481944), source = DataSource.PLEOS,
            address = "경기도 성남시 수정구 창업로40번길 6",
            poiId = "36khp2hdj2cfcm5nqhe3", poiSubId = "0", poiProvider = "",
        ),
    )
}
