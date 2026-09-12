package id.hiltons.linksanitiser

import android.content.Context

class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("link_sanitiser_prefs", Context.MODE_PRIVATE)

    var linksCleanedCount: Int
        get() = sp.getInt(KEY_COUNT, 0)
        set(value) = sp.edit().putInt(KEY_COUNT, value).apply()

    var stripUtm: Boolean
        get() = sp.getBoolean(KEY_STRIP_UTM, true)
        set(value) = sp.edit().putBoolean(KEY_STRIP_UTM, value).apply()

    var stripClickIds: Boolean
        get() = sp.getBoolean(KEY_STRIP_CLICK_IDS, true)
        set(value) = sp.edit().putBoolean(KEY_STRIP_CLICK_IDS, value).apply()

    var stripReferral: Boolean
        get() = sp.getBoolean(KEY_STRIP_REFERRAL, false)
        set(value) = sp.edit().putBoolean(KEY_STRIP_REFERRAL, value).apply()

    var showToast: Boolean
        get() = sp.getBoolean(KEY_SHOW_TOAST, true)
        set(value) = sp.edit().putBoolean(KEY_SHOW_TOAST, value).apply()

    /** Raw comma-separated text as typed by the user, preserved verbatim for editing. */
    var customParamsRaw: String
        get() = sp.getString(KEY_CUSTOM_PARAMS, "") ?: ""
        set(value) = sp.edit().putString(KEY_CUSTOM_PARAMS, value).apply()

    val customParams: Set<String>
        get() = customParamsRaw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    /** Remembered from the last time the Custom share target was used. */
    var cleanSurroundingText: Boolean
        get() = sp.getBoolean(KEY_CLEAN_SURROUNDING_TEXT, true)
        set(value) = sp.edit().putBoolean(KEY_CLEAN_SURROUNDING_TEXT, value).apply()

    fun toConfig(): SanitiserConfig = SanitiserConfig(
        stripUtm = stripUtm,
        stripClickIds = stripClickIds,
        stripReferral = stripReferral,
        customParams = customParams,
        cleanSurroundingText = cleanSurroundingText,
    )

    fun incrementCount(by: Int) {
        linksCleanedCount += by
    }

    /** How many times a link from [domain] has been cleaned and actually shared. */
    fun recordDomainCleaned(domain: String) {
        val counts = domainCounts.toMutableMap()
        counts[domain] = (counts[domain] ?: 0) + 1
        saveDomainCounts(counts)
    }

    /** The [limit] most-cleaned domains, highest count first. */
    fun topDomains(limit: Int): List<Pair<String, Int>> =
        domainCounts.entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }

    /** Clears the overall counter and the per-domain breakdown (not the settings). */
    fun resetStats() {
        linksCleanedCount = 0
        sp.edit().remove(KEY_DOMAIN_COUNTS).apply()
    }

    private val domainCounts: Map<String, Int>
        get() = (sp.getString(KEY_DOMAIN_COUNTS, "") ?: "").split(',')
            .mapNotNull { entry ->
                val domain = entry.substringBefore('=', "")
                val count = entry.substringAfter('=', "").toIntOrNull()
                if (domain.isNotEmpty() && count != null) domain to count else null
            }
            .toMap()

    private fun saveDomainCounts(counts: Map<String, Int>) {
        sp.edit().putString(KEY_DOMAIN_COUNTS, counts.entries.joinToString(",") { "${it.key}=${it.value}" }).apply()
    }

    private companion object {
        const val KEY_COUNT = "links_cleaned_count"
        const val KEY_STRIP_UTM = "strip_utm"
        const val KEY_STRIP_CLICK_IDS = "strip_click_ids"
        const val KEY_STRIP_REFERRAL = "strip_referral"
        const val KEY_SHOW_TOAST = "show_toast"
        const val KEY_CUSTOM_PARAMS = "custom_params"
        const val KEY_CLEAN_SURROUNDING_TEXT = "clean_surrounding_text"
        const val KEY_DOMAIN_COUNTS = "domain_counts"
    }
}
