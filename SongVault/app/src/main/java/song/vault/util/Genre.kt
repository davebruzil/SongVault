package song.vault.util

enum class Genre(val displayName: String) {
    ROCK("Rock"),
    METAL("Metal"),
    POP("Pop"),
    HIP_HOP("Hip-Hop"),
    INDIE("Indie"),
    ELECTRONIC("Electronic"),
    JAZZ("Jazz"),
    CLASSICAL("Classical"),
    R_AND_B("R&B"),
    COUNTRY("Country"),
    FOLK("Folk"),
    REGGAE("Reggae"),
    SOUL("Soul"),
    LATIN("Latin"),
    BLUES("Blues"),
    PUNK("Punk"),
    ALTERNATIVE("Alternative"),
    AMBIENT("Ambient"),
    EXPERIMENTAL("Experimental"),
    OTHER("Other");

    companion object {
        fun fromDisplayName(displayName: String?): Genre? {
            return values().find { it.displayName == displayName }
        }

        fun getAllGenres(): List<String> {
            return values().map { it.displayName }
        }
    }
}
