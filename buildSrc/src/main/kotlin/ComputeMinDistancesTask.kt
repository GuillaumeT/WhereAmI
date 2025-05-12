import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiPolygon
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.min

enum class CountryStatus {
    WAITING,
    RUNNING,
    FINISHED,
}

object CountryStatusTracker {
    val statuses = ConcurrentHashMap<Int, CountryStatus>()
}

@CacheableTask
abstract class ComputeMinDistancesTask @Inject constructor(
    private val workers: WorkerExecutor
) : DefaultTask() {

    @get: InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val countriesGeoJson: RegularFileProperty

    @get: OutputFile
    abstract val outputJson: RegularFileProperty

    @TaskAction
    fun execute() {
        // Read and parse the GeoJSON once
        val geoJsonFile = countriesGeoJson.asFile.get()
        val features = getFeaturesFromJson(geoJsonFile.readText())
        val totalCount = features.size

        // Prepare directory for partial outputs
        val partialDir = project.layout.buildDirectory.dir("distances/partials").get().asFile
        partialDir.mkdirs()

        // Submit one WorkAction per country index
        val workerQueue = workers.noIsolation()
        features.forEachIndexed { index, _ ->
            CountryStatusTracker.statuses[index] = CountryStatus.WAITING
            workerQueue.submit(
                DistanceWorkAction::class.java,
                object : Action<DistanceParameters> {
                    override fun execute(params: DistanceParameters) {
                        params.geoJsonFile.set(geoJsonFile)
                        params.countryIndex.set(index)
                        params.totalCount.set(totalCount)
                        params.partialOutput.set(partialDir.resolve("dist_$index.json"))
                    }
                }
            )
        }

        val numberOfLines = features.size / 10
        println()
        print("\n".repeat(numberOfLines))
        while (true) {
            var count = 0
            val statusLine = CountryStatusTracker.statuses.entries.sortedBy { it.key }
                .joinToString("") { (idx, state) ->
                    val iso = features[idx].getProperty("ADM0_A3").asString
                    val end = if ((idx + 1) % 10 == 0) "\n" else "  "
                    val status = when (state) {
                        CountryStatus.WAITING -> "\u2800"
                        CountryStatus.RUNNING -> WAIT_CHARS.random()
                        CountryStatus.FINISHED -> "\u2713"
                    }
                    "$iso: $status$end"
                }
            print("\r\u001b[${numberOfLines}A$statusLine")
            System.out.flush()
            if (CountryStatusTracker.statuses.values.all { it == CountryStatus.FINISHED }) {
                println()
                break
            }
            Thread.sleep(400)
        }
        workerQueue.await()

        // Merge partial JSON outputs
        val gson = Gson()
        val finalMap = mutableMapOf<String, Map<String, Double>>()
        partialDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            val slice: MutableMap<String, Map<String, Double>> = gson.fromJson(
                file.readText(), object : TypeToken<Map<String, Map<String, Double>>>() {}.type
            )
            finalMap.putAll(slice)
        }

        // Add reverse correspondences
        finalMap.forEach { (countryA, toB) ->
            toB.forEach { (countryB, distance) ->
                (finalMap[countryB] as MutableMap)[countryA] = distance
            }
        }

        // last check that we have perfect one to one
        require(finalMap.all {
            it.value.size + 1 == finalMap.size
        }) { "Oops something went wrong, we don't have all reciprocal distances" }

        // Write merged result
        val outputFile = outputJson.asFile.get()
        outputFile.writeText(
            gson.toJson(
                finalMap.toSortedMap().map {
                    mapOf(it.key to it.value.toSortedMap())
                })
        )

        // Log overall completion
        logger.lifecycle("Completed merging ${finalMap.size} country distance entries.")
    }
}

interface DistanceParameters : WorkParameters {
    val geoJsonFile: RegularFileProperty
    val countryIndex: Property<Int>
    val totalCount: Property<Int>
    val partialOutput: RegularFileProperty
}

abstract class DistanceWorkAction @Inject constructor(
    private val params: DistanceParameters
) : WorkAction<DistanceParameters> {

    override fun execute() {
        // Parse features inside each worker
        val json = params.geoJsonFile.get().asFile.readText()
        val features = getFeaturesFromJson(json)

        val i = params.countryIndex.get()
        CountryStatusTracker.statuses[i] = CountryStatus.RUNNING
        val countryA = features[i]
        val idA = countryA.getProperty("ADM0_A3").asString

        val total = params.totalCount.get()
        val workLogger = Logging.getLogger(DistanceWorkAction::class.java)

        // Compute distances
        val minDistances = mutableMapOf<String, Double>()
        for (j in (i + 1) until features.size) {
            val countryB = features[j]
            val idB = countryB.getProperty("ADM0_A3").asString
            val distance = minDistanceBetweenCountries(
                countryA.geometry() as MultiPolygon,
                countryB.geometry() as MultiPolygon
            )
            minDistances[idB] = distance
        }

        // Write partial JSON for this country
        val slice = mapOf(idA to minDistances)
        params.partialOutput.get().asFile.writeText(Gson().toJson(slice))
        CountryStatusTracker.statuses[i] = CountryStatus.FINISHED
    }
}

private fun getFeaturesFromJson(json: String): List<Feature> {
    return FeatureCollection.fromJson(json).features()!!.filter {
        it.getProperty("ADM0_A3").asString != "ATA" // skip Antarctica
    }
}

private fun minDistanceBetweenCountries(
    countryA: MultiPolygon,
    countryB: MultiPolygon
): Double {
    fun getEdges(polygon: MultiPolygon): List<LineString> {
        val edges = mutableListOf<LineString>()

        polygon.polygons().forEach {
            // adds outer perimeters
            it.outer()?.let(edges::add)

            // adds inner perimeters
            it.inner()?.forEach(edges::add)
        }

        return edges
    }

    fun minDistanceVerticesToEdges(vertices: List<Point>, edges: List<LineString>): Double {
        require(vertices.isNotEmpty()) { "vertices cannot be empty" }
        require(edges.isNotEmpty()) { "edges cannot be empty" }
        var minDistance = Double.MAX_VALUE

        vertices.forEach { vertice ->
            edges.forEach { edge ->
                val nearestPointOnEdge =
                    TurfMisc.nearestPointOnLine(
                        vertice,
                        edge.coordinates(),
                        TurfConstants.UNIT_KILOMETERS
                    ).geometry() as Point
                val distance = TurfMeasurement.distance(
                    vertice,
                    nearestPointOnEdge,
                    TurfConstants.UNIT_KILOMETERS
                )
                if (distance < 1.0) {
//                    if (distance > 0.0) logger.warn("Found distance between 0 and 1: $distance")
                    return 0.0
                }
                if (distance < minDistance) minDistance = distance
            }
        }
        assert(minDistance != Double.MAX_VALUE) { "Something went wrong, found min distance as Double.MAX_VALUE" }

        return minDistance
    }

    val verticesA = countryA.coordinates().flatten().flatten()
    val verticesB = countryB.coordinates().flatten().flatten()

    val edgesA = getEdges(countryA)
    val edgesB = getEdges(countryB)

    return min(
        minDistanceVerticesToEdges(verticesA, edgesB),
        minDistanceVerticesToEdges(verticesB, edgesA),
    )

}

val WAIT_CHARS = setOf(
    "\u2804",
    "\u2820",
    "\u2824",
    "\u2840",
    "\u2844",
    "\u2860",
    "\u2864",
    "\u2880",
    "\u2884",
    "\u28A0",
    "\u28A4",
    "\u28C0",
    "\u28C4",
    "\u28E0",
    "\u28E4",
)