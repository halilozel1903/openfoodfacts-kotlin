package io.github.halilozel1903.openfoodfacts

/**
 * The `User-Agent` sent with every request. Open Food Facts requires a custom one that names your app and gives a
 * way to contact you, in the form `AppName/Version (ContactEmail)`; requests with a generic agent may be blocked.
 *
 * ```kotlin
 * UserAgent(appName = "FoodScanner", version = "2.1", contact = "dev@example.com")
 * // FoodScanner/2.1 (dev@example.com) openfoodfacts-kotlin/1.0.0
 * ```
 */
public class UserAgent private constructor(
    /** The full header value. */
    public val value: String,
) {
    public constructor(appName: String, version: String, contact: String) : this(build(appName, version, contact))

    override fun toString(): String = value
    override fun equals(other: Any?): Boolean = other is UserAgent && other.value == value
    override fun hashCode(): Int = value.hashCode()

    public companion object {
        /** This library's own product token, appended to every agent built from parts. */
        public const val LIBRARY_TOKEN: String = "openfoodfacts-kotlin/1.0.0"

        /**
         * Uses [value] as is, for an agent you already format yourself. It must still name your app and a contact,
         * so it may not be blank or a bare library or HTTP client name.
         */
        public fun custom(value: String): UserAgent {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "The User-Agent may not be blank." }
            val generic = listOf("ktor", "okhttp", "java", "curl", "dalvik", "openfoodfacts-kotlin")
            require(generic.none { trimmed.lowercase().startsWith(it) }) {
                "Open Food Facts asks for a User-Agent that names your app, like \"MyApp/1.0 (me@example.com)\"; got \"$value\"."
            }
            return UserAgent(trimmed)
        }

        private fun build(appName: String, version: String, contact: String): String {
            require(appName.isNotBlank()) { "appName may not be blank." }
            require(version.isNotBlank()) { "version may not be blank." }
            require(contact.isNotBlank()) { "contact may not be blank: Open Food Facts asks for an email or url to reach you." }
            val name = appName.trim().replace(Regex("\\s+"), "-")
            return "$name/${version.trim()} (${contact.trim()}) $LIBRARY_TOKEN"
        }
    }
}
