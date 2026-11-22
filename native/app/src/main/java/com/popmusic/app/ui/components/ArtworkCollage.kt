package com.popmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.theme.Surface2
import com.popmusic.app.ui.theme.Surface3

/**
 * Portada en collage de 4 imágenes (estilo Echo Music). Si la lista tiene
 * menos portadas se adapta (1, 2 o 3) y rellena huecos con un degradado
 * suave para que nunca se vea roto.
 */
@Composable
fun ArtworkCollage(
    urls: List<String?>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    iconSize: Dp = 30.dp,
    gap: Dp = 2.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val covers = urls.filterNotNull().distinct().take(4)

    Box(
        modifier = modifier
            .clip(shape)
            .background(Surface2)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        when (covers.size) {
            0 -> PlaceholderTile(Modifier.fillMaxSize(), iconSize)
            1 -> Artwork(covers[0], Modifier.fillMaxSize(), cornerRadius = 0.dp, iconSize = iconSize)
            2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                CollageTile(covers[0], iconSize, Modifier.weight(1f).fillMaxHeight())
                CollageTile(covers[1], iconSize, Modifier.weight(1f).fillMaxHeight())
            }

            3 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                CollageTile(covers[0], iconSize, Modifier.weight(1.15f).fillMaxHeight())
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    CollageTile(covers[1], iconSize, Modifier.fillMaxWidth().weight(1f))
                    CollageTile(covers[2], iconSize, Modifier.fillMaxWidth().weight(1f))
                }
            }

            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                CollageRow(
                    left = covers[0],
                    right = covers[1],
                    iconSize = iconSize,
                    gap = gap,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                CollageRow(
                    left = covers[2],
                    right = covers[3],
                    iconSize = iconSize,
                    gap = gap,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        // Brillo superior muy sutil, como las portadas de Echo.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun CollageRow(
    left: String,
    right: String,
    iconSize: Dp,
    gap: Dp,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(gap)) {
        CollageTile(left, iconSize, Modifier.weight(1f).fillMaxHeight())
        CollageTile(right, iconSize, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun RowScope.CollageTile(
    url: String?,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Artwork(url, modifier, cornerRadius = 0.dp, iconSize = iconSize)
}

@Composable
private fun ColumnScope.CollageTile(
    url: String?,
    iconSize: Dp,
    modifier: Modifier = Modifier,
) {
    Artwork(url, modifier, cornerRadius = 0.dp, iconSize = iconSize)
}

@Composable
private fun PlaceholderTile(modifier: Modifier = Modifier, iconSize: Dp) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Surface3, Surface2)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Artwork(url = null, modifier = Modifier.fillMaxSize(), cornerRadius = 0.dp, iconSize = iconSize)
    }
}
