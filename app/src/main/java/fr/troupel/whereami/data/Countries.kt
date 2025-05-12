package fr.troupel.whereami.data

import android.content.Context
import fr.troupel.whereami.data.model.Country
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.geometry.LatLng
import java.util.Locale

public const val ID_CODE = "ADM0_A3"

var COUNTRIES: HashSet<Country> = HashSet()

fun initCountriesFromLocale() {
    COUNTRIES = countriesFromLocale()
}

fun initCountriesFromAssets(context: Context) {
    COUNTRIES = countriesFromAssets(context)

    // val cL = countriesFromLocale()
    // val cA = countriesFromAssets(context)
    // Log.d("WAI", "locale:${cL.size} assets:${cA.size}")
    // Log.d("WAI", "locale not in assets:${cL.filterNot { cA.contains(it) }.map { it.iso + ":" + it.name }}")
    // Log.d("WAI", "assets not in locale:${cA.filterNot { cL.contains(it) }.map { it.iso + ":" + it.name }}")
}

private fun countriesFromLocale(): HashSet<Country> {
    return Locale.getISOCountries().map {
        val locale = Locale("", it)
        Country(locale.country, locale.displayName)
    }.toHashSet()
}


private fun countriesFromAssets(context: Context): HashSet<Country> {
    val json = Json { ignoreUnknownKeys = true }
    val raw = context.assets.open("ne_10m_admin_0_countries.geojson")
        .bufferedReader()
        .use { it.readText() }

    return json.decodeFromString<FeatureCollection>(raw).features.map {
        Country(
            it.properties.ADM0_A3,
            it.properties.NAME_FR,
            LatLng(it.properties.LABEL_Y, it.properties.LABEL_X)
        )
    }.filter { it.iso != "-99" }.toHashSet()
}

@Serializable
private data class FeatureCollection(
    val type: String,
    val features: List<Feature>
)

@Serializable
private data class Feature(
    val type: String,
    val properties: FeatureProperties
)

@Serializable
private data class FeatureProperties(
    val scalerank: Int,
    val NAME_FR: String,
    val ISO_A2_EH: String,
    val ADM0_A3: String,
    val LABEL_X: Double,
    val LABEL_Y: Double,
)
