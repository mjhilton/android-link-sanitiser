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

    fun toConfig(): SanitiserConfig = SanitiserConfig(
        stripUtm = stripUtm,
        stripClickIds = stripClickIds,
        stripReferral = stripReferral,
        customParams = customParams,
    )

    fun incrementCount(by: Int) {
        linksCleanedCount += by
    }

    private companion object {
        const val KEY_COUNT = "links_cleaned_count"
        const val KEY_STRIP_UTM = "strip_utm"
        const val KEY_STRIP_CLICK_IDS = "strip_click_ids"
        const val KEY_STRIP_REFERRAL = "strip_referral"
        const val KEY_SHOW_TOAST = "show_toast"
        const val KEY_CUSTOM_PARAMS = "custom_params"
    }
}
