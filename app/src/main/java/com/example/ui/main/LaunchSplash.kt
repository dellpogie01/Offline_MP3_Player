package com.example.ui.main

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.OnSurfaceVariantDark
import kotlinx.coroutines.delay

/** Branded splash lasts three seconds from first composition, including across rotation. */
@Composable
fun LaunchSplash(content: @Composable () -> Unit) {
    var finished by rememberSaveable { mutableStateOf(false) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        if (!finished) {
            if (deadline == 0L) deadline = SystemClock.elapsedRealtime() + 3_000L
            delay((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            finished = true
        }
    }
    if (finished) content() else {
        Column(Modifier.fillMaxSize().background(MidnightBlack).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Image(painterResource(R.drawable.ic_music_note), contentDescription = null,
                modifier = Modifier.size(144.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.splash_subtitle), color = OnSurfaceVariantDark)
        }
    }
}
