package id.hiltons.linksanitiser

/**
 * Which categories of tracking parameters to remove, plus any user-supplied
 * extras. Kept as plain data so [LinkSanitiser] has no Android dependency
 * and can be unit-tested on the plain JVM.
 */
data class SanitiserConfig(
    val stripUtm: Boolean = true,
    val stripClickIds: Boolean = true,
    val stripReferral: Boolean = false,
    val customParams: Set<String> = emptySet(),
)

data class SanitiseResult(
    val text: String,
    val urlsChanged: Int,
    val paramsRemoved: Int,
)

object LinkSanitiser {

    // utm_* is matched by prefix below, these are the well-known exact names.
    private val UTM_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_referrer", "utm_source_platform",
        "utm_creative_format", "utm_marketing_tactic",
    )

    // Ad-network / platform click identifiers. Safe to strip: they only
    // feed attribution systems, never page behaviour.
    private val CLICK_ID_PARAMS = setOf(
        "gclid", "gclsrc", "dclid", "gbraid", "wbraid",
        "fbclid", "igshid", "igsh",
        "msclkid", "twclid", "ttclid", "yclid", "rdt_cid",
        "mc_eid", "mc_cid",
        "vero_id", "vero_conv",
        "epik", "si", "srsltid",
        "_hsenc", "_hsmi", "mkt_tok",
        "li_fat_id", "s_kwcid", "spm",
    )

    // Generic referral / attribution params. These occasionally do affect
    // page behaviour (e.g. a real query param a site happens to call "ref"),
    // so they're opt-in.
    private val REFERRAL_PARAMS = setOf(
        "ref", "ref_src", "ref_url", "referrer", "source", "from", "trk", "trkCampaign",
    )

    private val URL_REGEX = Regex("""https?://[^\s<>"]+""")
    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

    fun sanitise(input: String, config: SanitiserConfig): SanitiseResult {
        var urlsChanged = 0
        var paramsRemoved = 0

        val output = URL_REGEX.replace(input) { match ->
            val (core, trailer) = splitTrailingPunctuation(match.value)
            val (cleaned, removed) = sanitiseUrl(core, config)
            if (removed > 0) {
                urlsChanged++
                paramsRemoved += removed
            }
            cleaned + trailer
        }

        return SanitiseResult(output, urlsChanged, paramsRemoved)
    }

    /**
     * URLs picked up by a greedy regex often drag along trailing punctuation
     * from the surrounding sentence (e.g. "check this out: https://x.com/a."),
     * so peel it off before parsing and reattach it afterwards.
     */
    private fun splitTrailingPunctuation(url: String): Pair<String, String> {
        var end = url.length
        while (end > 0 && url[end - 1] in TRAILING_PUNCTUATION) {
            end--
        }
        // Keep a closing bracket/paren if there's a matching opener earlier in the URL.
        if (end < url.length) {
            val trimmedChar = url[end]
            if ((trimmedChar == ')' && url.take(end).count { it == '(' } > url.take(end).count { it == ')' }) ||
                (trimmedChar == ']' && url.take(end).count { it == '[' } > url.take(end).count { it == ']' })
            ) {
                end++
            }
        }
        return url.substring(0, end) to url.substring(end)
    }

    private fun sanitiseUrl(url: String, config: SanitiserConfig): Pair<String, Int> {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url to 0

        val fragmentStart = url.indexOf('#', queryStart)
        val query = if (fragmentStart >= 0) url.substring(queryStart + 1, fragmentStart) else url.substring(queryStart + 1)
        val fragment = if (fragmentStart >= 0) url.substring(fragmentStart) else ""
        val base = url.substring(0, queryStart)

        if (query.isEmpty()) return url to 0

        val customLower = config.customParams.map { it.lowercase() }.toSet()
        var removed = 0
        val keptPairs = query.split('&').filter { pair ->
            if (pair.isEmpty()) return@filter false
            val name = pair.substringBefore('=').lowercase()
            val shouldStrip = name in customLower ||
                (config.stripUtm && (name in UTM_PARAMS || name.startsWith("utm_"))) ||
                (config.stripClickIds && name in CLICK_ID_PARAMS) ||
                (config.stripReferral && name in REFERRAL_PARAMS)
            if (shouldStrip) removed++
            !shouldStrip
        }

        val rebuilt = if (keptPairs.isEmpty()) base else base + "?" + keptPairs.joinToString("&")
        return rebuilt + fragment to removed
    }
}
