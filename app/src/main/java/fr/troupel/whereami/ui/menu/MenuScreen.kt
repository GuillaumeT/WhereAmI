package fr.troupel.whereami.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.troupel.whereami.navigation.Screen

@Composable
fun MenuScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Menu")
        Button(onClick = { onNavigate("") }) {
            Text("Nouvelle Partie")
        }
        Button(onClick = { onNavigate(Screen.Options.route) }) {
            Text("Options")
        }
    }
}

@Preview
@Composable
fun MenuScreenPreview() {
    MenuScreen { }
}