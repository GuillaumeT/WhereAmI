package fr.troupel.whereami.data.model

import org.maplibre.android.geometry.LatLng

class Country(
    val iso: String,
    val name: String,
    val latLng: LatLng? = null,
) {

    override fun toString(): String {
        return this.name
    }

    override fun hashCode(): Int {
        return iso.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other.hashCode() == this.hashCode()
    }
}