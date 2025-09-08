package fr.troupel.whereami.ui.guesscountry.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.troupel.whereami.WhereAmI
import fr.troupel.whereami.domain.Difficulty
import fr.troupel.whereami.domain.GuessTheCountry

val LocalMenuExpanded = compositionLocalOf { mutableStateOf(false) }

@Composable
fun MainDropdownMenu(modifier: Modifier = Modifier) {
    val expanded = LocalMenuExpanded.current
    val context = LocalContext.current
    val game = (context.applicationContext as WhereAmI).game as GuessTheCountry

    Box(
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier.width(0.dp),
            onClick = { expanded.value = !expanded.value }
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            DropdownMenuItem(
                text = { Text("Niveau Facile") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.EASY) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game = GuessTheCountry(Difficulty.EASY)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Moyen") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.NORMAL) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game = GuessTheCountry(Difficulty.NORMAL)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Difficile") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.DIFFICULT) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game = GuessTheCountry(Difficulty.DIFFICULT)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Impossible") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.INSANE) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game = GuessTheCountry(Difficulty.INSANE)
                    expanded.value = false
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Afficher la solution") },
                onClick = {
                    val game = (context.applicationContext as WhereAmI).game as GuessTheCountry
                    Toast.makeText(
                        context,
                        "Le pays à deviner est \"${game.solution.name}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

