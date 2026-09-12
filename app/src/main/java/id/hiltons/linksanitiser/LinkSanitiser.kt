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
    val cleanSurroundingText: Boolean = false,
)

data class SanitiseResult(
    val text: String,
    val urlsChanged: Int,
    val paramsRemoved: Int,
)

/** One URL found in a shared text, before deciding whether it's kept. */
data class FoundLink(
    val range: IntRange,
    val original: String,
    val cleaned: String,
    val paramsRemoved: Int,
)

object LinkSanitiser {

    // Campaign-tag families like utm_* (Google), mtm_* (Matomo) and itm_*
    // (various CMSes) all follow the same "prefix_field" shape, so they're
    // matched by prefix rather than being spelled out individually.
    private val CAMPAIGN_TAG_PREFIXES = listOf("utm_", "mtm_", "otm_", "itm_", "hmb_")

    private val UTM_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_referrer", "utm_source_platform",
        "utm_creative_format", "utm_marketing_tactic",
    )

    // Ad-network / platform click identifiers and analytics tags. Safe to
    // strip everywhere: they only feed attribution systems, never page
    // behaviour. Curated from the global (site-agnostic) rules in the
    // ClearURLs project (github.com/ClearURLs/Rules) plus a few extras.
    private val CLICK_ID_PARAMS = setOf(
        "gclid", "gclsrc", "dclid", "gbraid", "wbraid",
        "fbclid", "igshid", "igsh",
        "msclkid", "twclid", "ttclid", "yclid", "rdt_cid",
        "mc_eid", "mc_cid", "mc_tc",
        "vero_id", "vero_conv",
        "epik", "si", "srsltid",
        "_hsenc", "_hsmi", "mkt_tok", "__hsfp", "__hssc", "__hstc", "hsctatracking",
        "li_fat_id", "s_kwcid", "spm", "trackingid",
        "_openstat", "fb_action_types", "fb_action_ids", "fb_source", "fb_ref",
        "action_object_map", "action_type_map", "action_ref_map",
        "cmpid", "os_ehash", "_ga", "_gl", "__twitter_impression",
        "xtor", "wtmc", "wt_mc", "wtzmc", "wt_zmc", "wtrid",
        "ml_subscriber", "ml_subscriber_hash", "oly_anon_id", "oly_enc_id",
        "rb_clickid", "s_cid", "wickedid", "tracking_source", "ceneo_spo",
    )

    // Generic referral / attribution params. These occasionally do affect
    // page behaviour (e.g. a real query param a site happens to call "ref"),
    // so they're opt-in.
    private val REFERRAL_PARAMS = setOf(
        "ref", "ref_src", "ref_url", "referrer", "source", "from", "trk", "trkCampaign",
    )

    // Some tracking parameter names are too generic to strip site-wide (e.g.
    // The Guardian's "CMP" campaign tag, or "sh" on Forbes) without risking a
    // false match on some other site that happens to use the same short
    // name for something meaningful. These are only stripped when the link's
    // host contains a matching label, keyed here by that label (e.g.
    // "theguardian" matches theguardian.com, www.theguardian.com, ...).
    // Sourced from ClearURLs' per-site rules.
    private val DOMAIN_SPECIFIC_PARAMS: Map<String, Set<String>> = mapOf(
        "theguardian" to setOf("cmp"),
        "nytimes" to setOf("smid"),
        "linkedin" to setOf("refid"),
        "reddit" to setOf("correlation_id", "share_id", "rdt"),
        "forbes" to setOf("sh"),
        "amazon" to setOf("tag", "linkcode", "ascsubtag"),
    )

    private val URL_REGEX = Regex("""https?://[^\s<>"]+""")
    private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '\'', '"')

    /** Convenience entry point for the common case: clean every link in place, keep them all. */
    fun sanitise(input: String, config: SanitiserConfig): SanitiseResult {
        val found = findLinks(input, config)
        return buildResult(input, config, found, found.indices.toSet())
    }

    /** Locates every link in [text] and cleans each one, without yet deciding which survive. */
    fun findLinks(text: String, config: SanitiserConfig): List<FoundLink> =
        URL_REGEX.findAll(text).map { match ->
            val (core, _) = splitTrailingPunctuation(match.value)
            val (cleaned, removed) = sanitiseUrl(core, config)
            val start = match.range.first
            FoundLink(IntRange(start, start + core.length - 1), core, cleaned, removed)
        }.toList()

    /**
     * Builds the final shared text: only the links at [keepIndices] (into
     * [found]) survive. With [SanitiserConfig.cleanSurroundingText], the
     * result is just those links, newline separated; otherwise they're
     * cleaned in place and any dropped links are removed, leaving the rest
     * of the text as-is.
     */
    fun buildResult(text: String, config: SanitiserConfig, found: List<FoundLink>, keepIndices: Set<Int>): SanitiseResult {
        if (found.isEmpty()) return SanitiseResult(text, 0, 0)

        val paramsRemoved = keepIndices.sumOf { found[it].paramsRemoved }
        val urlsChanged = keepIndices.count { found[it].cleaned != found[it].original }

        val text2 = if (config.cleanSurroundingText) {
            keepIndices.sorted().joinToString("\n") { found[it].cleaned }
        } else {
            buildString {
                var cursor = 0
                for ((i, link) in found.withIndex()) {
                    append(text, cursor, link.range.first)
                    if (i in keepIndices) append(link.cleaned)
                    cursor = link.range.last + 1
                }
                append(text, cursor, text.length)
            }
        }

        return SanitiseResult(text2, urlsChanged, paramsRemoved)
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
        val domainParams = domainSpecificParams(base)
        var removed = 0
        val keptPairs = query.split('&').filter { pair ->
            if (pair.isEmpty()) return@filter false
            val name = pair.substringBefore('=').lowercase()
            val shouldStrip = name in customLower ||
                name in domainParams ||
                (config.stripUtm && (name in UTM_PARAMS || CAMPAIGN_TAG_PREFIXES.any { name.startsWith(it) })) ||
                (config.stripClickIds && name in CLICK_ID_PARAMS) ||
                (config.stripReferral && name in REFERRAL_PARAMS)
            if (shouldStrip) removed++
            !shouldStrip
        }

        val rebuilt = if (keptPairs.isEmpty()) base else base + "?" + keptPairs.joinToString("&")
        return rebuilt + fragment to removed
    }

    /**
     * Looks up [DOMAIN_SPECIFIC_PARAMS] by matching a whole hostname label
     * (e.g. "amazon" in "smile.amazon.co.uk") rather than a substring, so a
     * domain like "notamazon.com" is never mistaken for "amazon.*".
     */
    private fun domainSpecificParams(urlBeforeQuery: String): Set<String> {
        val labels = hostOf(urlBeforeQuery)?.split('.')?.toSet() ?: return emptySet()
        return DOMAIN_SPECIFIC_PARAMS.entries
            .firstOrNull { (label, _) -> label in labels }
            ?.value
            ?: emptySet()
    }

    /** The lowercased host of [url] with any leading "www." dropped, or null if it can't be parsed. */
    fun hostOf(url: String): String? {
        val afterScheme = url.substringAfter("://", url)
        if (afterScheme == url && !url.contains("://")) return null
        val host = afterScheme.substringBefore('/').substringAfterLast('@').substringBefore(':').lowercase()
        return host.removePrefix("www.").takeIf { it.isNotEmpty() }
    }
}
