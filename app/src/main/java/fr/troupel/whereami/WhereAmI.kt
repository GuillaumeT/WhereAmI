package fr.troupel.whereami

import android.app.Application
import fr.troupel.whereami.data.initCountriesFromAssets
import fr.troupel.whereami.data.model.Game
import fr.troupel.whereami.domain.GuessTheCountry

class WhereAmI: Application() {
    lateinit var game: Game

    override fun onCreate() {
        super.onCreate()

        initializeDomain()
    }

    private fun initializeDomain() {
        initCountriesFromAssets(this)

        game = GuessTheCountry()
    }

}
