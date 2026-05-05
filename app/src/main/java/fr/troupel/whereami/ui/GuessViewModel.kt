package fr.troupel.whereami.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.ID_CODE
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.domain.Difficulty
import fr.troupel.whereami.domain.GuessTheCountry
import fr.troupel.whereami.util.jaroWinkler
import fr.troupel.whereami.util.stripAccents
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.maplibre.geojson.FeatureCollection

/**
 * ViewModel holding the current [GuessTheCountry] game state.
 */
class GuessViewModel : ViewModel() {
    private var game: GuessTheCountry = GuessTheCountry()

    val _confetti = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val confetti = _confetti.asSharedFlow()

    private val _guesses = MutableLiveData(game.guesses)

    /** Exposes the list of guessed countries. */
    val guesses: LiveData<List<Country>> = _guesses

    /** Current game difficulty. */
    val difficulty: Difficulty
        get() = game.difficulty

    /** Current solution. */
    val solution: Country
        get() = game.solution

    /** Start a new game keeping the provided [difficulty]. */
    fun newGame(difficulty: Difficulty = game.difficulty) {
        game = GuessTheCountry(difficulty)
        _guesses.value = game.guesses
        Log.d("WAI", "Game: ${game.solution}")
    }

    /**
     * Try to guess a country from its [name].
     * Returns a [CountryGuessResult] describing the result.
     */
    fun guessCountry(name: String): CountryGuessResult {
        val country = COUNTRIES.values.find {
            it.name.trim().stripAccents().lowercase() == name.trim().stripAccents()
                .lowercase()
                .trim()
        }

        val isFound = if (country != null) game.guess(country) else false
        if (isFound) launchKonfetties()

        country?.let {
            // update guesses list whenever a country is guessed
            _guesses.value = game.guesses
        }

        val suggestions = if (country == null) COUNTRIES.values.filter {
            val score = jaroWinkler(
                name.trim().stripAccents().lowercase(),
                it.name.trim().stripAccents().lowercase()
            )
            score > .85
        } else emptyList()

        return CountryGuessResult(
            isCountryGuessed = isFound,
            country = country,
            suggestions = suggestions
        )
    }

    /**
     * Update all features in [collection] with their distance to the solution.
     */
    fun updateDistances(collection: FeatureCollection) {
        collection.features()?.forEach { feature ->
            val country = COUNTRIES[feature.getProperty(ID_CODE).asString] ?: return@forEach
            feature.properties()?.addProperty("distance", country.distanceTo[game.solution] ?: -1)
        }
    }

    fun launchKonfetties() {
        _confetti.tryEmit(Unit)
    }
}

/** Result returned when trying to guess a country. */
data class CountryGuessResult(
    val isCountryGuessed: Boolean,
    val country: Country?,
    val suggestions: List<Country> = emptyList(),
)
