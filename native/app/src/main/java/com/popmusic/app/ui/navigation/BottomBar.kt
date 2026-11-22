package com.popmusic.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.LiquidGlass
import com.popmusic.app.ui.theme.LocalLiquidGlassEnabled
import com.popmusic.app.ui.theme.LocalReduceMotion
import com.popmusic.app.ui.theme.Surface3
import com.popmusic.app.ui.theme.pressScale

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

// Pestañas dentro de la píldora, al estilo Echo: Buscar queda FUERA del grupo
// y se renderiza como el círculo independiente de la derecha.
private val groupDestinations = listOf(
    BottomDestination(Routes.HOME, "Inicio", Icons.Rounded.Home),
    BottomDestination(Routes.RECOGNIZE, "Reconocer", Icons.Rounded.Mic),
    BottomDestination(Routes.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
)
private val standaloneDestination = BottomDestination(Routes.SEARCH, "Buscar", Icons.Rounded.Search)

private val PillShape = RoundedCornerShape(percent = 100)

/**
 * Ítem 6: true cuando la barra de navegación está colapsada (modo inline
 * por scroll). El FAB de Biblioteca lo usa para bajar junto con la barra
 * en vez de tener su propio hide-on-scroll.
 */
val LocalNavCollapsed = staticCompositionLocalOf { false }

/**
 * Barra de navegación flotante estilo Echo Music (iOS 26): píldora con el
 * grupo de pestañas (indicador blanco deslizante bajo la activa) + círculo
 * independiente para Buscar. Al bajar el scroll cambia al modo inline con
 * UN solo botón a la izquierda (círculo de la pestaña activa + mini
 * reproductor) en lugar del viejo botón de menú.
 */
@Composable
fun PoPBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    inline: Boolean = false,
    onExpand: () -> Unit = {},
    miniVisible: Boolean = false,
    miniContent: (@Composable (Modifier) -> Unit)? = null,
) {
    val reduceMotion = LocalReduceMotion.current
    val showMini = miniVisible && miniContent != null

    AnimatedContent(
        targetState = inline,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            } else {
                fadeIn(tween(220)) togetherWith fadeOut(tween(160))
            }
        },
        contentAlignment = Alignment.BottomCenter,
        label = "navMode",
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) { isInline ->
        if (isInline) {
            InlineBar(
                currentRoute = currentRoute,
                reduceMotion = reduceMotion,
                showMini = showMini,
                miniContent = miniContent,
                onExpand = onExpand,
                onNavigate = onNavigate,
            )
        } else {
            ExpandedBar(
                currentRoute = currentRoute,
                reduceMotion = reduceMotion,
                showMini = showMini,
                miniContent = miniContent,
                onNavigate = onNavigate,
            )
        }
    }
}

// Entrada/salida del mini reproductor dentro de la barra (los mismos specs
// que tenía el AnimatedVisibility suelto en PopMusicApp).
private val miniEnter =
    expandVertically(expandFrom = Alignment.Top) +
        slideInVertically(initialOffsetY = { it / 2 }) +
        fadeIn(tween(220))
private val miniExit =
    shrinkVertically(shrinkTowards = Alignment.Top) +
        slideOutVertically(targetOffsetY = { it / 2 }) +
        fadeOut(tween(160))

@Composable
private fun ExpandedBar(
    currentRoute: String?,
    reduceMotion: Boolean,
    showMini: Boolean,
    miniContent: (@Composable (Modifier) -> Unit)?,
    onNavigate: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = showMini,
            enter = miniEnter,
            exit = miniExit,
        ) {
            miniContent?.invoke(Modifier.fillMaxWidth())
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(56.dp),
        ) {
            TabGroup(
                destinations = groupDestinations,
                currentRoute = currentRoute,
                reduceMotion = reduceMotion,
                onNavigate = onNavigate,
            )
            StandaloneTab(
                destination = standaloneDestination,
                selected = currentRoute == standaloneDestination.route,
                elevation = 12.dp,
                onNavigate = onNavigate,
            )
        }
    }
}

/**
 * Superficie de la barra de navegación (ítem 13 de la tanda 3): con
 * "Cristal líquido" ACTIVADO usa liquid glass (lente líquida, como Echo);
 * DESACTIVADO queda sólida y opaca con borde fino, al estilo de la nav de
 * Echo sin glass (`surfaceContainerHigh` + outline 1dp).
 */
@Composable
private fun NavGlass(
    shape: Shape,
    modifier: Modifier = Modifier,
    shine: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    if (LocalLiquidGlassEnabled.current) {
        LiquidGlass(
            modifier = modifier,
            shape = shape,
            liquid = true,
            shine = shine,
            content = content,
        )
    } else {
        Box(
            modifier = modifier
                .background(Surface3, shape)
                .border(1.dp, Faint.copy(alpha = 0.30f), shape),
            content = content,
        )
    }
}

/**
 * Píldora con el grupo de pestañas: fondo de cristal, indicador blanco (25%)
 * que se desliza bajo la pestaña activa y etiqueta que solo se despliega en
 * la activa (expandHorizontally + fadeIn), igual que FloatingTabBar de Echo.
 */
@Composable
private fun TabGroup(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    reduceMotion: Boolean,
    onNavigate: (String) -> Unit,
) {
    val density = LocalDensity.current
    val tabWidths = remember { mutableStateMapOf<Int, Dp>() }
    val tabOffsets = remember { mutableStateMapOf<Int, Dp>() }

    val selectedIndex = destinations
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)
    val targetWidth = tabWidths[selectedIndex] ?: 0.dp
    val targetOffset = tabOffsets[selectedIndex] ?: 0.dp
    val indicatorSpec = spring<Dp>(dampingRatio = 0.9f, stiffness = 500f)
    val animatedWidth by animateDpAsState(targetWidth, indicatorSpec, label = "navIndicatorWidth")
    val animatedOffset by animateDpAsState(targetOffset, indicatorSpec, label = "navIndicatorOffset")

    NavGlass(
        modifier = Modifier.shadow(12.dp, PillShape),
        shape = PillShape,
        shine = true,
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .then(
                    if (reduceMotion) {
                        Modifier
                    } else {
                        Modifier.animateContentSize(
                            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                        )
                    },
                ),
        ) {
            Box(
                Modifier
                    .offset(x = animatedOffset)
                    .width(animatedWidth)
                    .height(52.dp)
                    .clip(PillShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                destinations.forEachIndexed { index, destination ->
                    val selected = currentRoute == destination.route
                    val tint = if (selected) Ink else Ink.copy(alpha = 0.65f)
                    val interaction = remember { MutableInteractionSource() }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                tabWidths[index] = with(density) { coords.size.width.toDp() }
                                tabOffsets[index] = with(density) { coords.positionInParent().x.toDp() }
                            }
                            .clip(PillShape)
                            .clickable(
                                interactionSource = interaction,
                                indication = ripple(bounded = true),
                                onClick = { onNavigate(destination.route) },
                            )
                            .pressScale(interaction, pressedScale = 0.88f)
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = tint,
                            modifier = Modifier.size(24.dp),
                        )
                        AnimatedVisibility(
                            visible = selected,
                            enter = if (reduceMotion) {
                                fadeIn(tween(140))
                            } else {
                                expandHorizontally(expandFrom = Alignment.Start) + fadeIn(tween(140))
                            },
                            exit = if (reduceMotion) {
                                fadeOut(tween(100))
                            } else {
                                shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(tween(100))
                            },
                        ) {
                            Box(Modifier.padding(start = 4.dp)) {
                                Text(text = destination.label, color = tint, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandaloneTab(
    destination: BottomDestination,
    selected: Boolean,
    elevation: Dp,
    onNavigate: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    NavGlass(
        modifier = Modifier
            .size(56.dp)
            .shadow(elevation, CircleShape),
        shape = CircleShape,
        shine = true,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(bounded = false, radius = 28.dp),
                    onClick = { onNavigate(destination.route) },
                )
                .pressScale(interaction, pressedScale = 0.90f),
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = if (selected) Ink else Ink.copy(alpha = 0.65f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Modo inline (al bajar el scroll): UN SOLO botón a la izquierda (el
 * círculo de la pestaña activa, o el primero si la ruta actual no es del
 * grupo, igual que `getInlineTab` de Echo) + el mini reproductor ocupando
 * el resto de la fila. Sin círculo de Buscar: para llegar a Buscar se
 * toca el círculo, que vuelve a expandir la barra.
 */
@Composable
private fun InlineBar(
    currentRoute: String?,
    reduceMotion: Boolean,
    showMini: Boolean,
    miniContent: (@Composable (Modifier) -> Unit)?,
    onExpand: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // Echo nunca deja la fila sin círculo: si la ruta actual no es una
    // pestaña del grupo (Ajustes, detalle de playlist...), usa la primera.
    val activeTab = groupDestinations.firstOrNull { it.route == currentRoute }
        ?: groupDestinations.first()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            // Altura fija 56dp (igual que ExpandedBar): SIN ella, el
            // fillMaxHeight/weight del mini estira la fila a toda la
            // pantalla y la barra "se sube a media pantalla".
            .height(56.dp)
            .fillMaxWidth(),
    ) {
        val interaction = remember { MutableInteractionSource() }
        NavGlass(
            modifier = Modifier
                .size(56.dp)
                .shadow(6.dp, CircleShape),
            shape = CircleShape,
            shine = true,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = false, radius = 28.dp),
                        onClick = {
                            onExpand()
                            onNavigate(activeTab.route)
                        },
                    )
                    .pressScale(interaction, pressedScale = 0.90f),
            ) {
                Icon(
                    imageVector = activeTab.icon,
                    contentDescription = activeTab.label,
                    tint = Ink,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = showMini,
            enter = miniEnter,
            exit = miniExit,
            modifier = if (showMini) {
                Modifier.weight(1f)
            } else {
                Modifier
            },
        ) {
            // fillMaxSize dentro de la Row de 56dp: el mini ocupa el alto
            // de la fila (si heredara maxHeight ilimitado, rompería el layout).
            miniContent?.invoke(Modifier.fillMaxSize())
        }
    }
}
