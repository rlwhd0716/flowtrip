package com.github.flowtrip.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.github.flowtrip.R
import com.github.flowtrip.domain.model.PlaceImage

/** Only photographic source regions are displayed; no vehicle/system controls are rendered. */
enum class ReferenceImage { HOME_TRIP, HOME_DATE, TRIP_HEADER, DATE_HEADER }

@Composable
fun ReferencePhoto(image: ReferenceImage, modifier: Modifier = Modifier) {
    val source = when (image) {
        ReferenceImage.HOME_TRIP -> PhotoSource(R.drawable.demo_home_reference, 880, 304, 210, 271)
        ReferenceImage.HOME_DATE -> PhotoSource(R.drawable.demo_home_reference, 1410, 304, 231, 271)
        ReferenceImage.TRIP_HEADER -> PhotoSource(R.drawable.demo_trip_input_reference, 1110, 60, 557, 189)
        ReferenceImage.DATE_HEADER -> PhotoSource(R.drawable.demo_date_input_reference, 1220, 60, 446, 179)
    }
    ReferenceRegion(source, modifier)
}

@Composable
fun PlacePhoto(image: PlaceImage, modifier: Modifier = Modifier) {
    val source = when (image) {
        PlaceImage.COAST -> PhotoSource(R.drawable.demo_travel_reference, 1082, 301, 565, 127)
        PlaceImage.TEMPLE -> PhotoSource(R.drawable.demo_travel_reference, 594, 679, 140, 115)
        PlaceImage.FOREST -> PhotoSource(R.drawable.demo_travel_reference, 1110, 60, 558, 96)
        PlaceImage.RESTAURANT -> PhotoSource(R.drawable.demo_date_reference, 1402, 314, 240, 104)
        PlaceImage.NIGHT -> PhotoSource(R.drawable.demo_date_reference, 1100, 62, 566, 95)
        PlaceImage.CAFE -> PhotoSource(R.drawable.demo_date_reference, 594, 528, 126, 98)
    }
    ReferenceRegion(source, modifier)
}

private data class PhotoSource(val resource: Int, val x: Int, val y: Int, val width: Int, val height: Int)

@Composable
private fun ReferenceRegion(source: PhotoSource, modifier: Modifier) {
    val bitmap = ImageBitmap.imageResource(source.resource)
    Image(painter = BitmapPainter(bitmap, IntOffset(source.x, source.y), IntSize(source.width, source.height)),
        contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
}
