package fr.troupel.whereami.ui.guesscountry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import androidx.compose.ui.tooling.preview.Preview
import fr.troupel.whereami.ui.theme.WhereAmITheme

@Composable
fun GuessDistanceNotification(
    countryName: String,
    distance: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xDCFFFFFF), RoundedCornerShape(10.dp))
            .padding(horizontal = 30.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = countryName,
            modifier = Modifier
                .padding(bottom = 15.dp)
                .background(Color.Black, RoundedCornerShape(5.dp))
                .padding(horizontal = 14.dp),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        val (textIntro, textDistance) = if (distance > 0) (
            "Le pays mystère est à" to "${"%.0f".format(distance)} km"
        ) else (
            "Le pays mystère est" to "frontalier"
        )

        Text(
            text = textIntro,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = textDistance,
            modifier = Modifier
                .padding(top = 4.dp),
            style = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Green,
                textAlign = TextAlign.Center,
                shadow = Shadow(
                    color = Color.DarkGray,
                    blurRadius = 5f
                )
            )
        )
    }
}

@Composable
fun SolutionFoundCongrats(modifier: Modifier) {
    KonfettiView(
        modifier = Modifier.fillMaxSize(),
        parties = listOf(
            Party(
                position = Position.Relative(.5, .0),
                emitter = Emitter(duration = 5000, TimeUnit.MILLISECONDS).max(5000)
            )
        )
    )
}

@Preview(showBackground = false)
@Composable
fun GuessDistanceNotificationPreview() {
    WhereAmITheme {
        Column {
            GuessDistanceNotification("Nigeria", 3849.23)
            GuessDistanceNotification("France", 0.0)
        }
    }
}

