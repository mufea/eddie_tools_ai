package com.example.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Asymmetric two-line menu icon matching the top-left button in the ChatGPT mobile UI.
 */
@Composable
fun ChatGptMenuIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.115f
        val leftX = this.size.width * 0.16f
        val topY = this.size.height * 0.36f
        val bottomY = this.size.height * 0.64f

        drawLine(
            color = tint,
            start = Offset(leftX, topY),
            end = Offset(this.size.width * 0.84f, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(leftX, bottomY),
            end = Offset(this.size.width * 0.56f, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Dashed speech-bubble icon matching the top-right button in the ChatGPT mobile UI.
 */
@Composable
fun ChatGptTempChatIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.105f
        val pad = this.size.minDimension * 0.14f
        val diameter = this.size.minDimension - pad * 2f

        val dashLen = this.size.minDimension * 0.16f
        val gapLen = this.size.minDimension * 0.12f

        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 295f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = Size(diameter, diameter),
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLen, gapLen), 0f)
            )
        )

        // Small speech bubble tail at bottom-left
        val tailPath = Path().apply {
            moveTo(this@Canvas.size.width * 0.25f, this@Canvas.size.height * 0.72f)
            lineTo(this@Canvas.size.width * 0.16f, this@Canvas.size.height * 0.86f)
            lineTo(this@Canvas.size.width * 0.35f, this@Canvas.size.height * 0.80f)
        }
        drawPath(
            path = tailPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * New chat compose icon (rounded square + diagonal pencil) for the ChatGPT sidebar.
 */
@Composable
fun ChatGptNewChatComposeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.095f

        val boxPath = Path().apply {
            moveTo(w * 0.52f, h * 0.18f)
            lineTo(w * 0.26f, h * 0.18f)
            quadraticTo(w * 0.16f, h * 0.18f, w * 0.16f, h * 0.28f)
            lineTo(w * 0.16f, h * 0.74f)
            quadraticTo(w * 0.16f, h * 0.84f, w * 0.26f, h * 0.84f)
            lineTo(w * 0.72f, h * 0.84f)
            quadraticTo(w * 0.82f, h * 0.84f, w * 0.82f, h * 0.74f)
            lineTo(w * 0.82f, h * 0.48f)
        }
        drawPath(
            path = boxPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Diagonal pen
        drawLine(
            color = tint,
            start = Offset(w * 0.46f, h * 0.54f),
            end = Offset(w * 0.84f, h * 0.16f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Photo/gallery frame icon matching "Create an image or sticker" in the ChatGPT UI.
 */
@Composable
fun ChatGptImageGalleryIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 21.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.09f
        val pad = w * 0.14f

        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad),
            size = Size(w - pad * 2f, h - pad * 2f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = stroke)
        )

        // Sun circle
        drawCircle(
            color = tint,
            radius = w * 0.075f,
            center = Offset(w * 0.36f, h * 0.38f),
            style = Stroke(width = stroke * 0.85f)
        )

        // Mountain polyline
        val mountainPath = Path().apply {
            moveTo(w * 0.20f, h * 0.76f)
            lineTo(w * 0.48f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.65f)
            lineTo(w * 0.74f, h * 0.55f)
            lineTo(w * 0.84f, h * 0.69f)
        }
        drawPath(
            path = mountainPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Minimalist pencil outline icon matching "Write or edit" in the ChatGPT UI.
 */
@Composable
fun ChatGptPencilEditIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 21.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.09f

        val pencilPath = Path().apply {
            moveTo(w * 0.66f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.34f)
            lineTo(w * 0.36f, h * 0.80f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.20f, h * 0.64f)
            close()
        }
        drawPath(
            path = pencilPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.56f, h * 0.28f),
            end = Offset(w * 0.72f, h * 0.44f),
            strokeWidth = stroke * 0.9f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Clean plus (+) stroke icon inside the composer pill.
 */
@Composable
fun ChatGptPlusIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.105f
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.18f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.5f),
            end = Offset(w * 0.82f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Clean microphone outline icon inside the composer pill.
 */
@Composable
fun ChatGptMicIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.095f

        // Mic capsule
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.36f, h * 0.14f),
            size = Size(w * 0.28f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = stroke)
        )

        // Outer U cradle
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.24f, h * 0.32f),
            size = Size(w * 0.52f, h * 0.38f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )

        // Stem & base
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.70f),
            end = Offset(w * 0.5f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.85f),
            end = Offset(w * 0.64f, h * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 4-bar voice waveform icon inside the vibrant blue circular button on the composer pill.
 */
@Composable
fun ChatGptVoiceWaveIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.125f

        val bars = listOf(
            Triple(0.22f, 0.36f, 0.64f),
            Triple(0.41f, 0.20f, 0.80f),
            Triple(0.59f, 0.28f, 0.72f),
            Triple(0.78f, 0.38f, 0.62f)
        )

        bars.forEach { (xFrac, topFrac, bottomFrac) ->
            drawLine(
                color = tint,
                start = Offset(w * xFrac, h * topFrac),
                end = Offset(w * xFrac, h * bottomFrac),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Upward arrow icon for sending text messages.
 */
@Composable
fun ChatGptUpArrowSendIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.125f

        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.80f),
            end = Offset(w * 0.5f, h * 0.22f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val headPath = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.20f)
            lineTo(w * 0.74f, h * 0.44f)
        }
        drawPath(
            path = headPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Clean magnifying glass search icon for the ChatGPT sidebar search bar.
 */
@Composable
fun ChatGptSearchIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.105f

        drawCircle(
            color = tint,
            radius = w * 0.27f,
            center = Offset(w * 0.43f, h * 0.43f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.63f, h * 0.63f),
            end = Offset(w * 0.84f, h * 0.84f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Clean camera outline icon for the '+' attachment sheet.
 */
@Composable
fun ChatGptCameraIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.09f

        val bodyPath = Path().apply {
            moveTo(w * 0.14f, h * 0.34f)
            lineTo(w * 0.30f, h * 0.34f)
            lineTo(w * 0.37f, h * 0.22f)
            lineTo(w * 0.63f, h * 0.22f)
            lineTo(w * 0.70f, h * 0.34f)
            lineTo(w * 0.86f, h * 0.34f)
            quadraticTo(w * 0.90f, h * 0.34f, w * 0.90f, h * 0.39f)
            lineTo(w * 0.90f, h * 0.75f)
            quadraticTo(w * 0.90f, h * 0.81f, w * 0.84f, h * 0.81f)
            lineTo(w * 0.16f, h * 0.81f)
            quadraticTo(w * 0.10f, h * 0.81f, w * 0.10f, h * 0.75f)
            lineTo(w * 0.10f, h * 0.39f)
            quadraticTo(w * 0.10f, h * 0.34f, w * 0.14f, h * 0.34f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawCircle(
            color = tint,
            radius = this.size.minDimension * 0.145f,
            center = Offset(w * 0.50f, h * 0.56f),
            style = Stroke(width = stroke)
        )
    }
}

/**
 * Clean document/file outline icon for the '+' attachment sheet.
 */
@Composable
fun ChatGptFileIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = this.size.minDimension * 0.09f

        val docPath = Path().apply {
            moveTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.58f, h * 0.14f)
            lineTo(w * 0.78f, h * 0.34f)
            lineTo(w * 0.78f, h * 0.82f)
            quadraticTo(w * 0.78f, h * 0.86f, w * 0.74f, h * 0.86f)
            lineTo(w * 0.24f, h * 0.86f)
            quadraticTo(w * 0.20f, h * 0.86f, w * 0.20f, h * 0.82f)
            lineTo(w * 0.20f, h * 0.18f)
            quadraticTo(w * 0.20f, h * 0.14f, w * 0.24f, h * 0.14f)
            close()
        }
        drawPath(
            path = docPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val foldPath = Path().apply {
            moveTo(w * 0.58f, h * 0.14f)
            lineTo(w * 0.58f, h * 0.34f)
            lineTo(w * 0.78f, h * 0.34f)
        }
        drawPath(
            path = foldPath,
            color = tint,
            style = Stroke(width = stroke * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.52f),
            end = Offset(w * 0.64f, h * 0.52f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.67f),
            end = Offset(w * 0.56f, h * 0.67f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
    }
}
