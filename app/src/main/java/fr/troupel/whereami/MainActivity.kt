package fr.troupel.whereami

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.domain.GuessTheCountry
import fr.troupel.whereami.ui.theme.DisputedArea
import fr.troupel.whereami.ui.theme.Ocean
import fr.troupel.whereami.ui.theme.RiverAndLake
import fr.troupel.whereami.ui.theme.WhereAmITheme
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.gte
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.stop
import org.maplibre.android.style.expressions.Expression.zoom
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.sources.RasterSource
import org.maplibre.geojson.FeatureCollection
import java.io.BufferedReader
import java.io.File
import java.net.URI


class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private lateinit var countriesFeatures: FeatureCollection
    private var guesses: List<Country> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WhereAmI
        Log.d("WAI", "Game: ${(app.game as GuessTheCountry).solution}")

        val landFilename = "HYP_HR_SR.pmtiles"
        val oceanFilename = "ne_10m_ocean.geojson"
        val riverFilename = "ne_10m_rivers_lake_centerlines_scale_rank.geojson"
        val lakeFilename = "ne_10m_lakes.geojson"
        val countriesFilename = "ne_10m_admin_0_countries.geojson"
        val disputedFilename = "ne_10m_admin_0_disputed_areas.geojson"


        copyAssetsIfNeeded(this, landFilename)
        MapLibre.getInstance(this, null, WellKnownTileServer.Mapbox)
        val landURI = Uri.fromFile(File(filesDir, landFilename))

        val rasterLandSource = RasterSource("land-source", "pmtiles://$landURI")
        val rasterLandLayer = RasterLayer("land", "land-source")
            .withProperties(
                PropertyFactory.rasterOpacity(1.0f),
                PropertyFactory.visibility(Property.VISIBLE),
                PropertyFactory.rasterResampling(Property.RASTER_RESAMPLING_NEAREST),
                //PropertyFactory.backgroundColor(Ocean),
            )


        val riverSource = GeoJsonSource("river-source", URI("asset://$riverFilename"))
        val riverLayer = LineLayer("river-layer", "river-source")
            .withProperties(
                PropertyFactory.lineColor(RiverAndLake),
                PropertyFactory.lineWidth(
                    interpolate(
                        linear(),
                        get("strokeweig"),
                        stop(0.5, 1.0),
                        stop(1.0, 2.0),
                        stop(3.0, 3.0)
                    )
                )

            )
        riverLayer.setFilter(gte(zoom(), get("min_zoom")))

        val lakeSource = GeoJsonSource("lake-source", URI("asset://$lakeFilename"))
        val lakeLayer = FillLayer("lake-layer", "lake-source").withProperties(
            PropertyFactory.fillColor(RiverAndLake),
            PropertyFactory.visibility(Property.VISIBLE),
        )
        lakeLayer.setFilter(gte(zoom(), get("min_zoom")))

        //val oceanSource = GeoJsonSource("ocean-source",URI("asset://$oceanFileName"))
        val oceanSource = GeoJsonSource("ocean-source", URI("asset://$oceanFilename"))
        val oceanLayer = FillLayer("ocean-layer", "ocean-source")
            .withProperties(
                PropertyFactory.fillColor(Ocean),
                PropertyFactory.visibility(Property.VISIBLE),
            )

        val countriesSource = GeoJsonSource("countries-source", URI("asset://$countriesFilename"))
        val json = assets.open(countriesFilename).bufferedReader().use(BufferedReader::readText)
        countriesFeatures = FeatureCollection.fromJson(json)
        //countriesFeatures = countriesSource.querySourceFeatures(null)
        val shownCountriesSource =
            GeoJsonSource("shown-countries-source", FeatureCollection.fromFeatures(emptyArray()))

        val countriesLayer = FillLayer("countries-layer", "shown-countries-source")
            .withProperties(
                PropertyFactory.fillColor("purple"),
                PropertyFactory.visibility(Property.VISIBLE)
            )

        val disputedSource = GeoJsonSource("disputed-source", URI("asset://$disputedFilename"))
        val disputedLayer = FillLayer("disputed-layer", "disputed-source").withProperties(
            PropertyFactory.fillColor(DisputedArea),
            PropertyFactory.visibility(Property.VISIBLE)
        )

        mapView = MapView(this).apply {
            getMapAsync { map ->
                map.setMinZoomPreference(.5)
                map.setMaxZoomPreference(6.0)

                map.addOnCameraMoveListener {
                    Log.d("WAI", "zoom: ${map.cameraPosition.zoom}")
                }
//                map.setStyle(
//                    Style.Builder()
//                        .fromUri("https://raw.githubusercontent.com/wipfli/foursquare-os-places-pmtiles/refs/heads/main/style.json")
//                )

                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(20.0, 0.0))
                    .zoom(1.0)
                    .build()


                map.setStyle(
                    Style.Builder()
                        .withSources(
                            rasterLandSource,
                            oceanSource,
                            riverSource,
                            lakeSource,
                            //countriesSource,
                            shownCountriesSource,
                            disputedSource
                        )
                        .withLayers(
                            BackgroundLayer("bg").withProperties(
                                PropertyFactory.backgroundColor(Ocean)
                            ),
                            rasterLandLayer,
                            oceanLayer,
                            riverLayer,
                            lakeLayer,
                            countriesLayer,
//                            disputedLayer
                        )
                )

            }
        }
        //setContentView(mapView)
        setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                )

                CountryInput(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 16.dp)
                        .fillMaxWidth(0.9f),
                    onSubmit = { guessCountry(it) }
                )
            }
        }

    }

    private fun guessCountry(countryName: String) {
        Log.d("Guess", "Guess is \"$countryName\"")
        val country = COUNTRIES.find { it.name.lowercase() == countryName.lowercase() }
        Log.d("Guess", "is it a valid country: $country")

        country?.let {
            // add guess
            guesses += country

            // is it the country we are looking for ?
            val isFound = ((application as WhereAmI).game as GuessTheCountry).guess(it)
            Log.d("Guess", "Was it the country to find ? $isFound")

            // show the country on the map
            mapView.getMapAsync { map ->
                map.getStyle { style ->

                    val shownCountriesSource = requireNotNull(
                        style.getSource("shown-countries-source") as GeoJsonSource
                    )
                    val shownCountries = guesses.toSet().map { g -> g.iso }

                    Log.d("Guess", "shownCountries $shownCountries")
                    Log.d("Guess", "countriesFeatures ${countriesFeatures.features()?.size}")
                    val shownFeatures = countriesFeatures.features()?.filter { feat ->
                        feat.getProperty("ISO_A2_EH").asString in shownCountries
                    }?.toTypedArray() ?: emptyArray()
                    Log.d("Guess", "shownCountriesFeatures ${shownFeatures.size}")

                    if (shownFeatures.isNotEmpty()) {
                        shownCountriesSource.setGeoJson(FeatureCollection.fromFeatures(shownFeatures))
                    }

//                    shownCountriesSource.setGeoJson(FeatureCollection.fromJson(shownFeatures.toString()))
                    //    FeatureCollection.fromFeatures(
                    //        countriesFeatures.features()?.filter { feat ->
                    //            // shownCountries.contains(feat.getProperty("ISO_A2_EH"))
                    //            Log.d("Guess", "feature: ${feat.getProperty("ISO_A2_EH")}")
                    //            false
                    //        }?.toTypedArray()
                    //    )
                    //)

                    Log.d("Guess", "Setting the filter to show the country on map")
                    val countryLayer = style.getLayer("countries-layer") as? FillLayer
                    //        countryLayer?.setFilter(
                    //            `in`(
                    //                get("ISO_A2_EH"),
                    //                literal(guesses.map { c -> c.iso }.toSet())
                    //            )
                    //        )
                }
            }
        }
    }

    private fun copyAssetsIfNeeded(context: Context, assetName: String) {
        val outputFile = File(context.filesDir, assetName)
        if (!outputFile.exists()) {
            context.assets.open(assetName).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhereAmITheme {
        Greeting("Android")
    }
}


@Composable
fun CountryInput(
    modifier: Modifier = Modifier,
    label: String = "Enter country",
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(modifier = modifier) {

        fun submit(value: String) {
            onSubmit(value)
            Log.d("WAI", "submitted text: $text")
        }

        TextField(
            value = text,
            singleLine = true,
            onValueChange = { newText: String -> text = newText },
            placeholder = { Text("Type a country") },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                submit(text)
                keyboardController?.hide()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Button(
            onClick = { submit(text) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-32).dp)
        ) {
            Text("Guess")
        }
    }

}