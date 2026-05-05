package fr.troupel.whereami.ui.guesscountry.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Stable
class KonfettiController {
    var parties by mutableStateOf(emptyList<Party>())
        private set

    fun launch(
        party: Party = Party(
            spread = 360,
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 5000, TimeUnit.MILLISECONDS).max(5000)
        )
    ) {
        // ensure a restart even if called twice in the same frame
        parties = emptyList()
        parties = listOf(party)
    }
}

@Composable
fun rememberKonfettiController() = remember { KonfettiController() }

@Composable
fun KonfettiHost(controller: KonfettiController, modifier: Modifier = Modifier) {
    KonfettiView(modifier = modifier, parties = controller.parties)
}