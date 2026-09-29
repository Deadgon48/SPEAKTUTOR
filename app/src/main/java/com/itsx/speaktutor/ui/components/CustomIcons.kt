package com.itsx.speaktutor.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val ArrowLeftCircleIcon: ImageVector
    get() = ImageVector.Builder(
        name = "ArrowLeftCircle",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black), // El componente Icon() de Compose le aplicará el tinte correcto automáticamente
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Contorno del círculo
            moveTo(21f, 12f)
            curveToRelative(0f, 4.97f, -4.03f, 9f, -9f, 9f)
            curveToRelative(-4.97f, 0f, -9f, -4.03f, -9f, -9f)
            curveToRelative(0f, -4.97f, 4.03f, -9f, 9f, -9f)
            curveToRelative(4.97f, 0f, 9f, 4.03f, 9f, 9f)
            close()

            // Línea de la flecha
            moveTo(17f, 12f)
            horizontalLineToRelative(-9.5f)

            // Punta de la flecha
            moveTo(7f, 12f)
            lineToRelative(4f, 4f)
            moveTo(7f, 12f)
            lineToRelative(4f, -4f)
        }
    }.build()

val HomeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // Base de la casa (M4.5 21.5h15)
            moveTo(4.5f, 21.5f)
            horizontalLineToRelative(15f)

            // Paredes laterales (M4.5 21.5v-13.5M19.5 21.5v-13.5)
            moveTo(4.5f, 21.5f)
            verticalLineToRelative(-13.5f)
            moveTo(19.5f, 21.5f)
            verticalLineToRelative(-13.5f)

            // Techo (M2 10l10 -8l10 8)
            moveTo(2f, 10f)
            lineToRelative(10f, -8f)
            lineToRelative(10f, 8f)

            // Puerta (M9.5 21.5v-9h5v9)
            moveTo(9.5f, 21.5f)
            verticalLineToRelative(-9f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(9f)
        }
    }.build()