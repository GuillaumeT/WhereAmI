package fr.troupel.whereami.domain

import android.util.Log
import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.data.model.Game

class GuessTheCountry : Game("Guess the Country") {
    val solution: Country
    private var _guesses: Array<Country> = emptyArray<Country>()

    val guesses: List<Country>
        get() = _guesses.toList()

    init {
        require(COUNTRIES.isNotEmpty()) {
            "Country list was not initialized. Could not initialize random country to guess."
        }
        solution = COUNTRIES.values.random()
        Log.i("GuessTheCountry", "Game initialized with country to guess: $solution")
    }

    fun guess(country: Country): Boolean {
        _guesses += country
        return country == solution
    }

}

