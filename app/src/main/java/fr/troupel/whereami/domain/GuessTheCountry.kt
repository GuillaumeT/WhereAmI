package fr.troupel.whereami.domain

import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.data.model.Game

class GuessTheCountry : Game("Guess the Country") {
    val solution: Country

    init {
        require(COUNTRIES.isNotEmpty()) {
            "Country list was not initialized. Could not initialize random country to guess."
        }
        solution = COUNTRIES.random()
    }

    fun guess(country: Country): Boolean {
        return country == solution
    }
}
