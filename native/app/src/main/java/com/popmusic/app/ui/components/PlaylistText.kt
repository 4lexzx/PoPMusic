package com.popmusic.app.ui.components

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private const val OPEN_QUOTE = '\u00AB'
private const val CLOSE_QUOTE = '\u00BB'

/**
 * Quita las comillas latinas de un texto y deja lo que había dentro en
 * negrita. Así «Mi lista» se ve como **Mi lista**, sin corchetes ni comillas.
 * Se usa para los avisos (Toast) que llegan como `CharSequence`.
 */
fun CharSequence.boldQuoted(): CharSequence {
    val text = toString()
    val start = text.indexOf(OPEN_QUOTE)
    if (start < 0) return this
    val end = text.indexOf(CLOSE_QUOTE, start + 1)
    if (end < 0) return this
    val plain = text.substring(0, start) + text.substring(start + 1, end) + text.substring(end + 1)
    return SpannableString(plain).apply {
        setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            start + (end - start - 1),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
}

/** Versión Compose del mismo formato, para `Text`. */
fun CharSequence.boldQuotedAnnotated(): AnnotatedString {
    val text = toString()
    val start = text.indexOf(OPEN_QUOTE)
    if (start < 0) return AnnotatedString(text)
    val end = text.indexOf(CLOSE_QUOTE, start + 1)
    if (end < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(text.substring(start + 1, end))
        }
        append(text.substring(end + 1))
    }
}

/** Texto con el nombre de la playlist en negrita y sin comillas. */
fun boldName(prefix: String, name: String, suffix: String = ""): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(name)
        }
        append(suffix)
    }

@Composable
fun BoldQuotedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text.boldQuotedAnnotated(),
        modifier = modifier,
        style = style,
        color = color,
    )
}
