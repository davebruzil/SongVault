package song.vault.util

enum class Genre(val displayName: String) {
    ROCK("Rock"),
    METAL("Metal"),
    POP("Pop"),
    HIPHOP("Hip-Hop"),
    INDIE("Indie"),
    ELECTRONIC("Electronic"),
    JAZZ("Jazz"),
    CLASSICAL("Classical"),
    COUNTRY("Country"),
    BLUES("Blues"),
    REGGAE("Reggae"),
    FOLK("Folk"),
    SOUL("Soul"),
    FUNK("Funk"),
    LATIN("Latin"),
    EDM("EDM"),
    AMBIENT("Ambient"),
    LOFI("Lo-Fi"),
    PUNK("Punk"),
    GRUNGE("Grunge");

    companion object {
        fun fromString(value: String?): Genre? {
            return if (value == null) null
            else try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
