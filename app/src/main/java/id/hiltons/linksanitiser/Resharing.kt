package id.hiltons.linksanitiser

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast

/**
 * Extracts the text a share-target activity was invoked with, or null if
 * this isn't the plain-text share we expect.
 */
fun Activity.extractSharedText(): String? =
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    } else {
        null
    }

/**
 * Reports [paramsRemoved] to the counter/toast, then re-opens the share
 * sheet with [text] so the user can pick the real destination, excluding
 * our own share targets so they don't loop back into themselves. Finishes
 * the calling activity.
 */
fun Activity.reshareCleaned(text: String, paramsRemoved: Int, prefs: Prefs) {
    if (paramsRemoved > 0) {
        prefs.incrementCount(paramsRemoved)
        if (prefs.showToast) {
            val paramWord = if (paramsRemoved == 1) "parameter" else "parameters"
            Toast.makeText(this, getString(R.string.toast_cleaned, paramsRemoved, paramWord), Toast.LENGTH_SHORT).show()
        }
    } else if (prefs.showToast) {
        Toast.makeText(this, R.string.toast_nothing_to_clean, Toast.LENGTH_SHORT).show()
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    val chooser = Intent.createChooser(sendIntent, null).apply {
        putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(
                ComponentName(this@reshareCleaned, ShareReceiverActivity::class.java),
                ComponentName(this@reshareCleaned, AdvancedShareActivity::class.java),
            ),
        )
    }

    startActivity(chooser)
    finish()
}

/**
 * Cleans [text] under [config] and either reshares it immediately, or - if
 * [SanitiserConfig.linksToKeep] is [LinksToKeep.CHOOSE] and there's more than
 * one link to choose between - hands off to [LinkPickerActivity] to let the
 * user pick first. Finishes the calling activity either way.
 */
fun Activity.processAndReshare(text: String, config: SanitiserConfig, prefs: Prefs) {
    val found = LinkSanitiser.findLinks(text, config)

    if (config.linksToKeep == LinksToKeep.CHOOSE && found.size > 1) {
        val intent = Intent(this, LinkPickerActivity::class.java).apply {
            putExtra(LinkPickerActivity.EXTRA_CANDIDATES, found.map { it.cleaned }.toTypedArray())
            putExtra(LinkPickerActivity.EXTRA_PARAMS_REMOVED, found.map { it.paramsRemoved }.toIntArray())
        }
        startActivity(intent)
        finish()
    } else {
        val result = LinkSanitiser.buildResult(text, config, found)
        reshareCleaned(result.text, result.paramsRemoved, prefs)
    }
}
