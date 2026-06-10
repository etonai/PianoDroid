package com.pseddev.pianodroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pseddev.pianodroid.ui.theme.OnBackground
import com.pseddev.pianodroid.ui.theme.PrimaryColor

@Composable
fun MainScreen(onListen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onListen,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                contentColor = OnBackground,
            ),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Listen",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
