package fr.troupel.whereami.data.model

class Country(val iso: String, val name: String) {

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