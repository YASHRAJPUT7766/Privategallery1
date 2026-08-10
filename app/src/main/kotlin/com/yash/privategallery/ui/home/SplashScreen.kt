package com.yash.privategallery.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yash.privategallery.R
import kotlinx.coroutines.delay

/**
 * Section 1 / Section 54's first step: "Show a clean splash screen. Show the
 * app logo and app name." A subtle scale-in animation gives it a premium
 * feel without becoming a distraction (Section 56: "Animations should not
 * cause lag" — this is a single Animatable, negligible cost).
 *
 * [onSplashComplete] fires after a short, fixed delay; the caller (nav graph)
 * decides where to route next based on first-launch/permission state, which
 * this screen deliberately has no knowledge of — it only announces "I'm done".
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = tween(durationMillis = 450))
        delay(650)
        onSplashComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier.fillMaxSize().scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}
