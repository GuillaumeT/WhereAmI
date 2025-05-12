import com.mapbox.geojson.FeatureCollection
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class CheckCountriesUniqueIDTask : DefaultTask() {

    @get: InputFile
    abstract val countriesGeoJsonFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val countriesGeoJsonFile = countriesGeoJsonFile.asFile.get()
        val countriesGeoJson = countriesGeoJsonFile.readText()
        val featureCollection = FeatureCollection.fromJson(countriesGeoJson)
        val features = featureCollection.features()

        val codesToCheck = setOf(
            "FIPS_10",
            "ISO_A2",
            "ISO_A2_EH",
            "ISO_A3",
            "ISO_A3_EH",
            "ISO_N3",
            "ISO_N3_EH",
            "UN_A3",
            "WB_A2",
            "WB_A3",
            "WOE_ID",
            "WOE_ID_EH",
            "ADM0_ISO",
            "NE_ID",
            "WIKIDATAID",
            "ADM0_A3",
            "GU_A3",
            "SU_A3",
            "BRK_A3"
        )

        codesToCheck.forEach { code ->
            val list = features?.map { it.getProperty(code).asString } ?: emptyList<String>()
            var example: String
            do {
                example = list.random()
            } while (example == "-99")
            logger.lifecycle(
                "$code:"
                        + "\n\tsize: ${features?.size}"
                        + "\n\tduplicates: ${(features?.size ?: 0) - list.toSet().size}"
                        + (if ("-99" in list) "\n\tcontains -99" else "")
                        + "\n\texample: $example"
            )
        }

    }
}