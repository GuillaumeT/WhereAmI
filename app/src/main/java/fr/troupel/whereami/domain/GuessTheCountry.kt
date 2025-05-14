package fr.troupel.whereami.domain

import android.util.Log
import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.data.model.Game

enum class Difficulty {
    EASY, NORMAL, DIFFICULT, INSANE
}

class GuessTheCountry(val difficulty: Difficulty = Difficulty.NORMAL) : Game("Guess the Country") {
    val solution: Country
    private var _guesses: Array<Country> = emptyArray<Country>()

    val guesses: List<Country>
        get() = _guesses.toList()

    init {
        require(COUNTRIES.isNotEmpty()) {
            "Country list was not initialized. Could not initialize random country to guess."
        }
        var candidateSolution: Country
        do {
            candidateSolution = COUNTRIES.values.random()
        } while (
                when (difficulty) {
                    Difficulty.EASY -> candidateSolution.popRank!! < 17
                    Difficulty.NORMAL -> candidateSolution.popRank!! < 11
                    Difficulty.DIFFICULT -> false // any country is accepted
                    Difficulty.INSANE -> candidateSolution.popRank!! > 10
                }
        )
        solution = candidateSolution

        Log.i("GuessTheCountry", "Game initialized with country to guess: $solution")
    }

    fun guess(country: Country): Boolean {
        _guesses += country
        return country == solution
    }

}

