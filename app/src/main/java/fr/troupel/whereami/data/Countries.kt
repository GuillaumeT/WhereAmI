package fr.troupel.whereami.data

import android.content.Context
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.troupel.whereami.data.model.Country
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.maplibre.android.geometry.LatLng
import java.io.File
import java.util.Locale

const val ID_CODE = "ADM0_A3"
val COUNTRIES: HashMap<String, Country> = HashMap()

@Deprecated("Use initCountriesFromAssets")
fun initCountriesFromLocale() {
    COUNTRIES.clear()
    COUNTRIES.putAll(countriesFromLocale().map { Pair(it.iso, it) })
}

fun initCountriesFromAssets(context: Context) {
    COUNTRIES.clear()
    COUNTRIES.putAll(countriesFromAssets(context).map { Pair(it.iso, it) })
    setCountriesDistances(context)
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
            LatLng(it.properties.LABEL_Y, it.properties.LABEL_X),
            it.properties.POP_RANK,
        )
    }.filter { it.iso != "ATA" }.toHashSet()
}

private fun setCountriesDistances(context: Context) {
    val gson = Gson()
    val distances: Map<String, Map<String, Double>> =
        gson.fromJson(
            context.assets.open("countries_distances.json").reader(),
            object : TypeToken<Map<String, Map<String, Double>>>() {}.type
        )

    distances.forEach { (isoA, distanceAtoB) ->
        val countryA = COUNTRIES[isoA]!!
        countryA.distanceTo.clear()
        countryA.distanceTo.putAll(distanceAtoB.mapKeys { (isoB, _) ->
            COUNTRIES[isoB]!!
        })
    }
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
    val POP_RANK: Int,
)
