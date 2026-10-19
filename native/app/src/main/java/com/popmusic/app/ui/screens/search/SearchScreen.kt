package com.popmusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.popmusic.app.ui.components.EmptyState
import com.popmusic.app.ui.theme.Faint
import com.popmusic.app.ui.theme.LineStrong
import com.popmusic.app.ui.theme.Surface2

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onOpenPlayer: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Buscar",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Canciones, artistas, álbumes…") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Faint,
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface2,
                unfocusedContainerColor = Surface2,
                focusedBorderColor = LineStrong,
                unfocusedBorderColor = LineStrong,
            ),
        )

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Pop", "Rock", "Chill", "Latino").forEach { tag ->
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(Surface2, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        EmptyState(
            title = "Busca en YouTube Music",
            subtitle = "Encuentra canciones completas, sin anuncios, y ábrelas al instante.",
            modifier = Modifier.weight(1f),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Faint,
                )
            },
        )
    }
}
