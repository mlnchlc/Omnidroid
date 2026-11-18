package com.omnidroid.app.mobile.shared.compose.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The line icons drawn for the Home and menu wireframes (24 x 24, round caps), so the app uses
 * the same set. Tint them with Icon like any other vector.
 */
object WireframeIcons {
    val Search: ImageVector by lazy {
        icon("Search") {
            stroke("M4,11a7,7 0 1 0 14,0a7,7 0 1 0 -14,0Z", 2.0f)
            stroke("M20,20l-3.5,-3.5", 2.0f)
        }
    }

    val ViewCarousel: ImageVector by lazy {
        icon("ViewCarousel") {
            stroke("M4.0,6H6.5A1.5,1.5 0 0 1 8.0,7.5V16.5A1.5,1.5 0 0 1 6.5,18H4.0A1.5,1.5 0 0 1 2.5,16.5V7.5A1.5,1.5 0 0 1 4.0,6Z", 2.0f)
            stroke("M10.75,5H13.25A1.5,1.5 0 0 1 14.75,6.5V17.5A1.5,1.5 0 0 1 13.25,19H10.75A1.5,1.5 0 0 1 9.25,17.5V6.5A1.5,1.5 0 0 1 10.75,5Z", 2.0f)
            stroke("M17.5,6H20.0A1.5,1.5 0 0 1 21.5,7.5V16.5A1.5,1.5 0 0 1 20.0,18H17.5A1.5,1.5 0 0 1 16,16.5V7.5A1.5,1.5 0 0 1 17.5,6Z", 2.0f)
        }
    }

    val ViewGrid: ImageVector by lazy {
        icon("ViewGrid") {
            stroke("M5.5,4H9.0A1.5,1.5 0 0 1 10.5,5.5V9.0A1.5,1.5 0 0 1 9.0,10.5H5.5A1.5,1.5 0 0 1 4,9.0V5.5A1.5,1.5 0 0 1 5.5,4Z", 2.0f)
            stroke("M15.0,4H18.5A1.5,1.5 0 0 1 20.0,5.5V9.0A1.5,1.5 0 0 1 18.5,10.5H15.0A1.5,1.5 0 0 1 13.5,9.0V5.5A1.5,1.5 0 0 1 15.0,4Z", 2.0f)
            stroke("M5.5,13.5H9.0A1.5,1.5 0 0 1 10.5,15.0V18.5A1.5,1.5 0 0 1 9.0,20.0H5.5A1.5,1.5 0 0 1 4,18.5V15.0A1.5,1.5 0 0 1 5.5,13.5Z", 2.0f)
            stroke("M15.0,13.5H18.5A1.5,1.5 0 0 1 20.0,15.0V18.5A1.5,1.5 0 0 1 18.5,20.0H15.0A1.5,1.5 0 0 1 13.5,18.5V15.0A1.5,1.5 0 0 1 15.0,13.5Z", 2.0f)
        }
    }

    val ViewList: ImageVector by lazy {
        icon("ViewList") {
            stroke("M4.5,4.5H7.5A1,1 0 0 1 8.5,5.5V8.5A1,1 0 0 1 7.5,9.5H4.5A1,1 0 0 1 3.5,8.5V5.5A1,1 0 0 1 4.5,4.5Z", 2.0f)
            stroke("M4.5,14.5H7.5A1,1 0 0 1 8.5,15.5V18.5A1,1 0 0 1 7.5,19.5H4.5A1,1 0 0 1 3.5,18.5V15.5A1,1 0 0 1 4.5,14.5Z", 2.0f)
            stroke("M12,7h8.5M12,17h8.5", 2.0f)
        }
    }

    val Dice: ImageVector by lazy {
        icon("Dice") {
            stroke("M7.0,3.5H17.0A3.5,3.5 0 0 1 20.5,7.0V17.0A3.5,3.5 0 0 1 17.0,20.5H7.0A3.5,3.5 0 0 1 3.5,17.0V7.0A3.5,3.5 0 0 1 7.0,3.5Z", 1.8f)
            fill("M7.2,8.5a1.3,1.3 0 1 0 2.6,0a1.3,1.3 0 1 0 -2.6,0Z")
            fill("M10.7,12a1.3,1.3 0 1 0 2.6,0a1.3,1.3 0 1 0 -2.6,0Z")
            fill("M14.2,15.5a1.3,1.3 0 1 0 2.6,0a1.3,1.3 0 1 0 -2.6,0Z")
        }
    }

    val Cast: ImageVector by lazy {
        icon("Cast") {
            stroke("M3,8V6.5A2.5,2.5 0 0 1 5.5,4h13A2.5,2.5 0 0 1 21,6.5v11a2.5,2.5 0 0 1 -2.5,2.5H14", 1.8f)
            stroke("M3,12.5a7.5,7.5 0 0 1 7.5,7.5", 1.8f)
            stroke("M3,16.5a3.5,3.5 0 0 1 3.5,3.5", 1.8f)
            fill("M2.7,19.6a0.9,0.9 0 1 0 1.8,0a0.9,0.9 0 1 0 -1.8,0Z")
        }
    }

    /** Gear: an outline cog around a centre hole. */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            stroke("M12.22,2h-0.44a2,2 0 0 0 -2,2v0.18a2,2 0 0 1 -1,1.73l-0.43,0.25a2,2 0 0 1 -2,0l-0.15,-0.08a2,2 0 0 0 -2.73,0.73l-0.22,0.38a2,2 0 0 0 0.73,2.73l0.15,0.1a2,2 0 0 1 1,1.72v0.51a2,2 0 0 1 -1,1.74l-0.15,0.09a2,2 0 0 0 -0.73,2.73l0.22,0.38a2,2 0 0 0 2.73,0.73l0.15,-0.08a2,2 0 0 1 2,0l0.43,0.25a2,2 0 0 1 1,1.73V20a2,2 0 0 0 2,2h0.44a2,2 0 0 0 2,-2v-0.18a2,2 0 0 1 1,-1.73l0.43,-0.25a2,2 0 0 1 2,0l0.15,0.08a2,2 0 0 0 2.73,-0.73l0.22,-0.39a2,2 0 0 0 -0.73,-2.73l-0.15,-0.08a2,2 0 0 1 -1,-1.74v-0.5a2,2 0 0 1 1,-1.74l0.15,-0.09a2,2 0 0 0 0.73,-2.73l-0.22,-0.38a2,2 0 0 0 -2.73,-0.73l-0.15,0.08a2,2 0 0 1 -2,0l-0.43,-0.25a2,2 0 0 1 -1,-1.73V4a2,2 0 0 0 -2,-2z", 1.8f)
            stroke("M9,12a3,3 0 1 0 6,0a3,3 0 1 0 -6,0Z", 1.8f)
        }
    }

    val Heart: ImageVector by lazy {
        icon("Heart") {
            stroke("M12,20s-7,-4.4 -7,-10a4,4 0 0 1 7,-2.6A4,4 0 0 1 19,10c0,5.6 -7,10 -7,10z", 2.0f)
        }
    }

    val HeartFilled: ImageVector by lazy {
        icon("HeartFilled") {
            fill("M12,20s-7,-4.4 -7,-10a4,4 0 0 1 7,-2.6A4,4 0 0 1 19,10c0,5.6 -7,10 -7,10z")
        }
    }

    val AllGames: ImageVector by lazy {
        icon("AllGames") {
            stroke("M5.5,4H9.0A1.5,1.5 0 0 1 10.5,5.5V9.0A1.5,1.5 0 0 1 9.0,10.5H5.5A1.5,1.5 0 0 1 4,9.0V5.5A1.5,1.5 0 0 1 5.5,4Z", 2.0f)
            stroke("M15.0,4H18.5A1.5,1.5 0 0 1 20.0,5.5V9.0A1.5,1.5 0 0 1 18.5,10.5H15.0A1.5,1.5 0 0 1 13.5,9.0V5.5A1.5,1.5 0 0 1 15.0,4Z", 2.0f)
            stroke("M5.5,13.5H9.0A1.5,1.5 0 0 1 10.5,15.0V18.5A1.5,1.5 0 0 1 9.0,20.0H5.5A1.5,1.5 0 0 1 4,18.5V15.0A1.5,1.5 0 0 1 5.5,13.5Z", 2.0f)
            stroke("M15.0,13.5H18.5A1.5,1.5 0 0 1 20.0,15.0V18.5A1.5,1.5 0 0 1 18.5,20.0H15.0A1.5,1.5 0 0 1 13.5,18.5V15.0A1.5,1.5 0 0 1 15.0,13.5Z", 2.0f)
        }
    }

    val More: ImageVector by lazy {
        icon("More") {
            fill("M3,12a2,2 0 1 0 4,0a2,2 0 1 0 -4,0Z")
            fill("M10,12a2,2 0 1 0 4,0a2,2 0 1 0 -4,0Z")
            fill("M17,12a2,2 0 1 0 4,0a2,2 0 1 0 -4,0Z")
        }
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            fill("M7,4.5v15a1,1 0 0 0 1.5,0.9l12,-7.5a1,1 0 0 0 0,-1.8l-12,-7.5A1,1 0 0 0 7,4.5z")
        }
    }

    private class Builder(val builder: ImageVector.Builder) {
        fun stroke(pathData: String, width: Float) {
            builder.addPath(
                pathData = addPathNodes(pathData),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }

        fun fill(pathData: String) {
            builder.addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        }
    }

    private fun icon(name: String, block: Builder.() -> Unit): ImageVector {
        val builder = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        Builder(builder).block()
        return builder.build()
    }
}
