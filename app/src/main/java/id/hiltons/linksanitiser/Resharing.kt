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
 * Reports the cleaning done to [keptLinks] to the counter/domain stats/toast,
 * then re-opens the share sheet with [text] so the user can pick the real
 * destination, excluding our own share targets so they don't loop back into
 * themselves. Finishes the calling activity.
 */
fun Activity.reshareCleaned(text: String, keptLinks: List<FoundLink>, prefs: Prefs) {
    val paramsRemoved = keptLinks.sumOf { it.paramsRemoved }

    if (paramsRemoved > 0) {
        prefs.incrementCount(paramsRemoved)
        keptLinks.filter { it.paramsRemoved > 0 }
            .forEach { link -> LinkSanitiser.hostOf(link.original)?.let(prefs::recordDomainCleaned) }
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
                ComponentName(this@reshareCleaned, CustomShareActivity::class.java),
            ),
        )
    }

    startActivity(chooser)
    finish()
}
