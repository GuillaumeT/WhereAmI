package fr.troupel.whereami

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import fr.troupel.whereami.data.COUNTRIES
import fr.troupel.whereami.data.ID_CODE
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.domain.Difficulty
import fr.troupel.whereami.domain.GuessTheCountry
import fr.troupel.whereami.ui.theme.DisputedArea
import fr.troupel.whereami.ui.theme.Ocean
import fr.troupel.whereami.ui.theme.RiverAndLake
import fr.troupel.whereami.ui.theme.WhereAmITheme
import fr.troupel.whereami.util.jaroWinkler
import fr.troupel.whereami.util.stripAccents
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.gte
import org.maplibre.android.style.expressions.Expression.interpolate
import org.maplibre.android.style.expressions.Expression.linear
import org.maplibre.android.style.expressions.Expression.rgb
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
import kotlin.random.Random

private const val lakeSourceId = "lake-source"
private const val lakeLayerId = "lake-layer"
private const val rasterLandSourceId = "land-source"
private const val rasterLandLayerId = "land"
private const val riverSourceId = "river-source"
private const val riverLayerId = "river-layer"
private const val oceanSourceId = "ocean-source"
private const val oceanLayerId = "ocean-layer"
private const val disputedSourceId = "disputed-source"
private const val disputedLayerId = "disputed-layer"
private const val countriesSourceId = "countries-source"
private const val countriesLayerId = "countries-layer"
private const val shownCountriesSourceId = "shown-countries-source"
private const val shownDisputedSourceId = "shown-disputed-source"
private const val concernsProperty = "concerns"

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private lateinit var countriesFeatures: FeatureCollection
    private lateinit var disputedFeatures: FeatureCollection
    private val vm by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as WhereAmI
        val solution = (app.game as GuessTheCountry).solution
        Log.d("WAI", "Game: $solution")

        val landFilename = "HYP_HR_SR.pmtiles"
        val oceanFilename = "ne_10m_ocean.geojson"
        val riverFilename = "ne_10m_rivers_lake_centerlines_scale_rank.geojson"
        val lakeFilename = "ne_10m_lakes.geojson"
        val countriesFilename = "ne_10m_admin_0_countries.geojson"
        val disputedFilename = "ne_10m_admin_0_disputed_areas.geojson"


        copyAssetsIfNeeded(this, landFilename)
        MapLibre.getInstance(this, null, WellKnownTileServer.Mapbox)
        val landURI = Uri.fromFile(File(filesDir, landFilename))

        val rasterLandSource = RasterSource(rasterLandSourceId, "pmtiles://$landURI")
        val rasterLandLayer = RasterLayer(rasterLandLayerId, rasterLandSourceId)
            .withProperties(
                PropertyFactory.rasterOpacity(1.0f),
                PropertyFactory.visibility(Property.VISIBLE),
                PropertyFactory.rasterResampling(Property.RASTER_RESAMPLING_NEAREST),
                //PropertyFactory.backgroundColor(Ocean),
            )


        val riverSource = GeoJsonSource(riverSourceId, URI("asset://$riverFilename"))
        val riverLayer = LineLayer(riverLayerId, riverSourceId)
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

        val lakeSource = GeoJsonSource(lakeSourceId, URI("asset://$lakeFilename"))
        val lakeLayer = FillLayer(lakeLayerId, lakeSourceId).withProperties(
            PropertyFactory.fillColor(RiverAndLake),
            PropertyFactory.visibility(Property.VISIBLE),
        )
        lakeLayer.setFilter(gte(zoom(), get("min_zoom")))

        val oceanSource = GeoJsonSource(oceanSourceId, URI("asset://$oceanFilename"))
        val oceanLayer = FillLayer(oceanLayerId, oceanSourceId)
            .withProperties(
                PropertyFactory.fillColor(Ocean),
                PropertyFactory.visibility(Property.VISIBLE),
            )

        val countriesSource = GeoJsonSource(countriesSourceId, URI("asset://$countriesFilename"))
        val countriesJson =
            assets.open(countriesFilename).bufferedReader().use(BufferedReader::readText)
        countriesFeatures = FeatureCollection.fromJson(countriesJson)
        updateDistances()
        //countriesFeatures = countriesSource.querySourceFeatures(null)

        val disputedJson =
            assets.open(disputedFilename).bufferedReader().use(BufferedReader::readText)
        val _props = mutableSetOf<String>()
        disputedFeatures = FeatureCollection.fromJson(disputedJson)
        disputedFeatures.features()?.forEach {
            val properties = it.properties()!!
            if (_props.isEmpty()) {
                _props.addAll(properties.keySet().filter { key -> key.startsWith("ADM0_A3_") })
            }

            val concernedCountries = mutableSetOf<String>()
            _props.forEach { key ->
                val value = properties[key].asString
                if (value != "-99") concernedCountries.add(value)
                properties.addProperty(concernsProperty, concernedCountries.joinToString(","))
            }
        }


        val shownCountriesSource =
            GeoJsonSource(shownCountriesSourceId, FeatureCollection.fromFeatures(emptyArray()))
        val shownDisputedSource =
            GeoJsonSource(shownDisputedSourceId, FeatureCollection.fromFeatures(emptyArray()))

        val countriesLayer = FillLayer(countriesLayerId, shownCountriesSourceId)
            .withProperties(
                // PropertyFactory.fillColor("purple"),
                //PropertyFactory.fillColor(get("color")),
                PropertyFactory.fillColor(
                    interpolate(
                        linear(),
                        get("distance"),
                        stop(-1, rgb(0, 255, 20)),
                        stop(0, rgb(50, 200, 20)),
                        stop(2000, rgb(100, 150, 20)),
                        stop(5000, rgb(139, 100, 20)),
                        stop(15000, rgb(139, 0, 20))

                    )
                ),
                PropertyFactory.fillOutlineColor("black"),
                PropertyFactory.visibility(Property.VISIBLE),
            )

        val disputedSource = GeoJsonSource(disputedSourceId, URI("asset://$disputedFilename"))
        val disputedLayer = FillLayer(disputedLayerId, shownDisputedSourceId).withProperties(
            PropertyFactory.fillColor(DisputedArea),
            PropertyFactory.fillPattern("crosshatch"),
            PropertyFactory.fillOpacity(.75f),
            PropertyFactory.visibility(Property.VISIBLE)
        )

        val mapOptions = MapLibreMapOptions.createFromAttributes(this).apply {
            rotateGesturesEnabled(false)
            compassEnabled(false)
            logoEnabled(false)
            attributionEnabled(false)
        }
        mapView = MapView(this, mapOptions).apply {
            getMapAsync { map ->
                map.setMinZoomPreference(.5)
                map.setMaxZoomPreference(6.0)

                map.addOnMapClickListener {
                    val screenPoint = map.projection.toScreenLocation(it)
                    val features = map.queryRenderedFeatures(screenPoint, countriesLayerId)

                    if (features.isNotEmpty()) {
                        val feature = features[0]
                        val id = feature.getProperty(ID_CODE).asString
                        val distance = feature.getProperty("distance").asDouble
                        val country = COUNTRIES[id]!!
                        vm.guessNotification(country.name, distance)
                        // FixMe remove toast
                        //Toast.makeText(
                        //    context,
                        //    "${country.name}\ndistance: ${"%.0f".format(distance)}km",
                        //    Toast.LENGTH_SHORT
                        //).show()
                        return@addOnMapClickListener true
                    }
                    return@addOnMapClickListener false
                }

                // map.setStyle(Style.Builder().fromUri("https://raw.githubusercontent.com/wipfli/foursquare-os-places-pmtiles/refs/heads/main/style.json"))

                // map.addOnCameraMoveListener {
                //     Log.d("WAI", "camera: ${map.cameraPosition}")
                // }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(.0, Random.nextDouble(-180.0, 180.0)))
                    .zoom(1.2)
                    .build()

                map.setStyle(
                    Style.Builder()
                        .withSources(
                            rasterLandSource,
                            oceanSource,
                            riverSource,
                            lakeSource,
                            countriesSource,
                            disputedSource,
                            shownCountriesSource,
                            shownDisputedSource,
                        )
                        .withLayers(
                            BackgroundLayer("bg").withProperties(
                                PropertyFactory.backgroundColor(Ocean)
                            ),
                            rasterLandLayer,
                            countriesLayer,
                            disputedLayer,
                            oceanLayer,
                            riverLayer,
                            lakeLayer,
                        )
                        .withImage(
                            "crosshatch",
                            createCrossHatchBitmap()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CountryInput(
                        modifier = Modifier
                            .fillMaxWidth(0.9f),
                        onSubmit = { guessCountry(it) },
                        onWin = {
                            Log.d("WAI", "COUNTRY FOUND!")
                            val difficulty = (app.game as GuessTheCountry).difficulty
                            app.game = GuessTheCountry(difficulty)
                        },
                        onValidGuess = {
                            it.latLng?.let {
                                mapView.getMapAsync { map ->
                                    map.animateCamera(CameraUpdateFactory.newLatLng(it))
                                }
                            }
                        }
                    )
                    // Build a single nullable "notification" value
                    val notif: Pair<String, Double>? =
                        vm.guessNoficationCountryName?.let { name ->
                            vm.guessNoficationCountryDistance?.let { dist -> name to dist }
                        }

                    fun transitionEnter() =
                        slideInVertically { -it / 2 } + expandVertically(expandFrom = Alignment.Top) + fadeIn()

                    fun transitionExit() = slideOutVertically { it / 2 } + fadeOut()
                    //fun transitionEnter()  =scaleIn() + fadeIn()
                    //fun transitionExit() = scaleOut() + fadeOut()

                    AnimatedVisibility(
                        visible = vm.isGuessNotificationShown && notif != null,
                        enter = transitionEnter(),
                        exit = transitionExit()
                    ) {
                        // Animate when either name or distance changes
                        AnimatedContent(
                            targetState = notif,
                            transitionSpec = { transitionEnter() togetherWith transitionExit() },
                            label = "guess-notification"
                        ) { state ->
                            state?.let { (countryName, distance) ->
                                GuessDistanceNotification(
                                    countryName = countryName,
                                    distance = distance,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable(
                                            interactionSource = null,
                                            indication = null
                                        ) { vm.hide() }
                                )
                            }
                        }
                    }

                    /*
                    AnimatedVisibility(
                        visible = vm.isGuessNotificationShown,
                        enter = slideInVertically() + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = slideOutVertically() + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        vm.guessNoficationCountryName?.let { countryName ->
                            vm.guessNoficationCountryDistance?.let { distance ->

                                AnimatedContent(
                                    targetState = vm.guessNoficationCountryName,
                                    transitionSpec = {
                                        (slideInVertically() + expandVertically(expandFrom = Alignment.Top) + fadeIn()) togetherWith
                                        (slideOutVertically() + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut())
                                    }
                                ) { countryName ->
                                    countryName?.let {
                                        GuessDistanceNotification(
                                            countryName = countryName,
                                            distance = distance,
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .clickable(
                                                    interactionSource = null,
                                                    indication = null
                                                ) { vm.hide() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                     */
                }
            }
        }
    }

    private fun updateDistances() {
        countriesFeatures.features()?.forEach {
            val country = COUNTRIES[it.getProperty(ID_CODE).asString] ?: return@forEach
            val game = (application as WhereAmI).game as GuessTheCountry
            it.properties()?.addProperty("distance", country.distanceTo[game.solution] ?: -1)
        }
    }

    private fun showCountries() {
        val game = ((application as WhereAmI).game as GuessTheCountry)

        // show the country on the map
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                // update shown countries
                val shownCountriesSource = requireNotNull(
                    style.getSource(shownCountriesSourceId) as GeoJsonSource
                )
                val shownDisputedSource = requireNotNull(
                    style.getSource(shownDisputedSourceId) as GeoJsonSource
                )

                val shownCountries = game.guesses.toSet().map { g -> g.iso }

                if (shownCountries.size == 1) {
                    // first guess, update the distances. This way avoids having wrong distance shown from previous game
                    updateDistances()
                }

                val shownCountriesFeatures = countriesFeatures.features()?.filter { feat ->
                    feat.getProperty(ID_CODE).asString in shownCountries
                }?.toTypedArray() ?: emptyArray()
                val shownDisputedFeatures = disputedFeatures.features()?.filter { feat ->
                    val concerned = feat.getProperty(concernsProperty).asString
                    shownCountries.any {
                        it in concerned
                    }
                }?.toTypedArray() ?: emptyArray()
                Log.d(
                    "disputed",
                    "shownDisputedFeatures: ${shownDisputedFeatures.map { it.getProperty("NAME") }}"
                )

                if (shownCountriesFeatures.isNotEmpty()) {
                    shownCountriesSource.setGeoJson(
                        FeatureCollection.fromFeatures(
                            shownCountriesFeatures
                        )
                    )
                }
                shownDisputedSource.setGeoJson(
                    FeatureCollection.fromFeatures(
                        shownDisputedFeatures
                    )
                )
            }
        }
    }

    private fun guessCountry(countryName: String): CountryGuessResult {
        Log.d("Guess", "Guess is \"$countryName\"")
        val country = COUNTRIES.values.find {
            it.name.trim().stripAccents().lowercase() == countryName.trim().stripAccents()
                .lowercase()
                .trim()
        }
        val game = ((application as WhereAmI).game as GuessTheCountry)

        Log.d("Guess", "is it a valid country: $country")
        // is it the country we are looking for ?
        val isFound = if (country != null) game.guess(country) else false
        Log.d("Guess", "Was it the country to find ? $isFound")


        country?.let {
            showCountries()

            val distance = game.solution.distanceTo[country]
            if (!isFound) {
                vm.guessNotification(country.name, distance!!)
                // Fixme remove toast
                //Toast.makeText(
                //    this,
                //    "${country.name}\ndistance: ${"%.0f".format(distance)}km",
                //    Toast.LENGTH_SHORT
                //).show()
            } else {
                Toast.makeText(
                    this,
                    "Bravo!!!\nVous avez trouvé ${country.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val suggestions = if (country == null) COUNTRIES.values.filter {
            val score = jaroWinkler(
                countryName.trim().stripAccents().lowercase(),
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
        showCountries()
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
//                .offset(y=-2.dp)
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
//                    offset = Offset(0f,2f),
                    blurRadius = 5f
                )
            )
        )
    }
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

@Composable
fun ViewAnnotationContent(modifier: Modifier = Modifier, text: String) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        fontSize = 20.sp
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhereAmITheme {
        Greeting("Android")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryInput(
    modifier: Modifier = Modifier,
    label: String = "Trouve le pays mystère",
    onSubmit: (String) -> CountryGuessResult,
    onWin: () -> Unit,
    onValidGuess: (Country) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(listOf<Country>()) }
    val menuExpanded = LocalMenuExpanded.current

    fun submit(value: String) {
        text = value // most probably already the case but not in case of suggestion selection
        val result = onSubmit(text)
        if (result.isCountryGuessed) {
            onWin()
        }
        if (result.country != null) {
            text = ""
            expanded = false
            onValidGuess(result.country)
        } else {
            expanded = true
            searchResults = result.suggestions
        }
    }

    Box(modifier = modifier) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                // TODO custom InputField to disable keyboard suggestions and have access to TextField(keyboardOptions)
                //   keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrect = false),
                Row {
                    SearchBarDefaults.InputField(
                        modifier = Modifier.onFocusChanged { },
                        query = text,
                        onQueryChange = { newText: String -> text = newText },
                        onSearch = ::submit,
                        expanded = expanded,
                        onExpandedChange = {},
                        placeholder = { Text(label) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    menuExpanded.value = !menuExpanded.value
                                }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }

                        }
                    )
                    MainDropdownMenu()
                }
            },

            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            // Display search results in a scrollable column
            Column(
                Modifier
//                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (searchResults.isEmpty())
                    Text("Aucune correspondance trouvée")
                else {
                    Log.d("Guess", "$searchResults")
                    searchResults.forEach { result ->
                        ListItem(
                            headlineContent = { Text(result.name) },
                            modifier = Modifier
                                .clickable { submit(result.name) }
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }


        // TextField(
        //     value = text,
        //     singleLine = true,
        //     onValueChange = { newText: String -> text = newText },
        //     placeholder = { Text("Type a country") },
        //     keyboardOptions = KeyboardOptions.Default.copy(
        //         imeAction = ImeAction.Done
        //     ),
        //     keyboardActions = KeyboardActions(onDone = {
        //         submit(text)
        //         keyboardController?.hide()
        //     }),
        //     modifier = Modifier
        //         .fillMaxWidth()
        //         .padding(16.dp)
        // )

        // Button(
        //     onClick = { submit(text) },
        //     modifier = Modifier
        //         .align(Alignment.CenterEnd)
        //         .offset(x = (-32).dp)
        // ) {
        //     Text("Guess")
        // }
    }

}

val LocalMenuExpanded = compositionLocalOf { mutableStateOf(false) }

@Composable
fun MainDropdownMenu(modifier: Modifier = Modifier) {
    val expanded = LocalMenuExpanded.current
    val context = LocalContext.current
    val game = (context.applicationContext as WhereAmI).game as GuessTheCountry

    Box(
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier.width(0.dp),
            onClick = { expanded.value = !expanded.value }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            DropdownMenuItem(
                text = { Text("Niveau Facile") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.EASY) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game = GuessTheCountry(Difficulty.EASY)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Moyen") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.NORMAL) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game =
                        GuessTheCountry(Difficulty.NORMAL)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Difficile") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.DIFFICULT) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game =
                        GuessTheCountry(Difficulty.DIFFICULT)
                    expanded.value = false
                },
            )
            DropdownMenuItem(
                text = { Text("Niveau Impossible") },
                trailingIcon = {
                    if (game.difficulty == Difficulty.INSANE) {
                        Icon(Icons.Default.Check, contentDescription = "Checked")
                    }
                },
                onClick = {
                    (context.applicationContext as WhereAmI).game =
                        GuessTheCountry(Difficulty.INSANE)
                    expanded.value = false
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Afficher la solution") },
                onClick = {
                    val game = (context.applicationContext as WhereAmI).game as GuessTheCountry
                    Toast.makeText(
                        context,
                        "Le pays à deviner est \"${game.solution.name}\"",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

data class CountryGuessResult(
    val isCountryGuessed: Boolean,
    val country: Country?,
    val suggestions: List<Country> = emptyList(),
)

fun createCrossHatchBitmap(size: Int = 16): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint().apply {
        color = Color.Black.toArgb()    // line color
        strokeWidth = 1f                // line thickness
        isAntiAlias = true
    }
    // draw two diagonal lines (\/ and /\)
    canvas.drawLine(0f, size.toFloat() / 2, size.toFloat(), 0f, paint)
    canvas.drawLine(0f, size.toFloat(), size.toFloat(), size.toFloat() / 2, paint)
    return bmp
}

// TODO move that viewmodel elsewhere. It is here just to try it out for now.
class MainViewModel : ViewModel() {
    var isGuessNotificationShown by mutableStateOf(false)
        private set
    var guessNoficationCountryName: String? by mutableStateOf(null)
        private set
    var guessNoficationCountryDistance: Double? by mutableStateOf(null)
        private set

    fun guessNotification(countryName: String, distance: Double) {
        guessNoficationCountryName = countryName
        guessNoficationCountryDistance = distance
        isGuessNotificationShown = true
    }

    fun hide() {
        isGuessNotificationShown = false
    }

    fun show() {
        isGuessNotificationShown = true
    }

    fun toggle() {
        isGuessNotificationShown = !isGuessNotificationShown
    }
}