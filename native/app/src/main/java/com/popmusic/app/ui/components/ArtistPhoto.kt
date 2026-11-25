package com.popmusic.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.popmusic.app.data.info.ArtistPhotoStore
import kotlinx.coroutines.CancellationException

data class ArtistPhotoState(val url: String?, val loading: Boolean)

/** Foto del artista: la de caché al instante o la de red cuando llegue. */
@Composable
fun rememberArtistPhoto(name: String): ArtistPhotoState {
    val context = LocalContext.current
    var state by remember(name) {
        val cached = ArtistPhotoStore.cached(context, name)
        mutableStateOf(
            ArtistPhotoState(cached, loading = cached == null && !ArtistPhotoStore.isMissing(name)),
        )
    }
    LaunchedEffect(name) {
        if (!state.loading) return@LaunchedEffect
        val url = try {
            ArtistPhotoStore.photo(context, name)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        state = ArtistPhotoState(url, loading = false)
    }
    return state
}
