package com.example.wavetune.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.wavetune.ui.theme.CyanAccent
import com.example.wavetune.ui.theme.PinkAccent
import com.example.wavetune.ui.theme.PurpleAccent
import com.example.wavetune.ui.theme.VioletAccent
import kotlin.math.abs

@Composable
fun WaveArtwork(
    artworkUri: String?,
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    showInitials: Boolean = true
) {
    val gradientBrushes = remember(title, artist) {
        val hash = abs((title + artist).hashCode())
        val palettes = listOf(
            listOf(Color(0xFF4F378B), Color(0xFFD0BCFF)),
            listOf(Color(0xFF354E40), Color(0xFFB1D18A)),
            listOf(Color(0xFF2D2F31), Color(0xFF4A4458)),
            listOf(Color(0xFF633B48), Color(0xFFEFB8C8)),
            listOf(Color(0xFF332D41), Color(0xFFCCC2DC)),
            listOf(Color(0xFF243447), Color(0xFF90A4AE))
        )
        val selected = palettes[hash % palettes.size]
        Brush.linearGradient(selected)
    }

    val context = LocalContext.current
    val imageRequest = remember(artworkUri) {
        if (!artworkUri.isNullOrBlank()) {
            ImageRequest.Builder(context)
                .data(Uri.parse(artworkUri))
                .crossfade(true)
                .build()
        } else null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(gradientBrushes),
        contentAlignment = Alignment.Center
    ) {
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "$title artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    ArtworkFallback(title = title, artist = artist, showInitials = showInitials, size = size)
                }
            )
        } else {
            ArtworkFallback(title = title, artist = artist, showInitials = showInitials, size = size)
        }
    }
}

@Composable
private fun ArtworkFallback(
    title: String,
    artist: String,
    showInitials: Boolean,
    size: Dp
) {
    val initial = remember(title) {
        title.trim().take(1).uppercase().ifBlank { "M" }
    }

    if (showInitials && size >= 40.dp) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    } else {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
