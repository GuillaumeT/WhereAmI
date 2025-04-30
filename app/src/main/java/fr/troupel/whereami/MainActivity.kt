package fr.troupel.whereami

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import fr.troupel.whereami.data.model.Country
import fr.troupel.whereami.domain.GuessTheCountry
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
import java.io.File
import java.net.URI

private const val OCEAN_COLOR = "#83acce"
private const val RIVER_AND_LAKE_COLOR = "#75acd9"
private const val DISPUTED_AREA_COLOR = "#c5ced9"

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
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
                //PropertyFactory.backgroundColor(OCEAN_COLOR),
            )


        val riverSource = GeoJsonSource("river-source", URI("asset://$riverFilename"))
        val riverLayer = LineLayer("river-layer", "river-source")
            .withProperties(
                PropertyFactory.lineColor(RIVER_AND_LAKE_COLOR),
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
            PropertyFactory.fillColor(RIVER_AND_LAKE_COLOR),
            PropertyFactory.visibility(Property.VISIBLE),
        )
        lakeLayer.setFilter(gte(zoom(), get("min_zoom")))

        //val oceanSource = GeoJsonSource("ocean-source",URI("asset://$oceanFileName"))
        val oceanSource = GeoJsonSource("ocean-source", URI("asset://$oceanFilename"))
        val oceanLayer = FillLayer("ocean-layer", "ocean-source")
            .withProperties(
                PropertyFactory.fillColor(OCEAN_COLOR),
                PropertyFactory.visibility(Property.VISIBLE),
            )

        val countriesSource = GeoJsonSource("countries-source", URI("asset://$countriesFilename"))
        val countriesLayer = FillLayer("countries-layer", "countries-source").withProperties(
            PropertyFactory.fillColor("green"),
            PropertyFactory.visibility(Property.VISIBLE)
        )

        val disputedSource = GeoJsonSource("disputed-source", URI("asset://$disputedFilename"))
        val disputedLayer = FillLayer("disputed-layer", "disputed-source").withProperties(
            PropertyFactory.fillColor(DISPUTED_AREA_COLOR),
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
                            countriesSource,
                            disputedSource
                        )
                        .withLayers(
                            BackgroundLayer("bg").withProperties(
                                PropertyFactory.backgroundColor(
                                    OCEAN_COLOR
                                )
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
        setContentView(mapView)
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