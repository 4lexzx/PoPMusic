package com.popmusic.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.Ink
import com.popmusic.app.ui.theme.Surface3

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    BottomDestination(Routes.HOME, "Inicio", Icons.Rounded.Home),
    BottomDestination(Routes.SEARCH, "Buscar", Icons.Rounded.Search),
    BottomDestination(Routes.RECOGNIZE, "Reconocer", Icons.Rounded.Mic),
    BottomDestination(Routes.LIBRARY, "Biblioteca", Icons.Rounded.LibraryMusic),
)

@Composable
fun PoPBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink,
                    selectedTextColor = Ink,
                    indicatorColor = Surface3,
                    unselectedIconColor = Faint,
                    unselectedTextColor = Faint,
                ),
            )
        }
    }
}
