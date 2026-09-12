package id.hiltons.linksanitiser

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity registered as an `ACTION_SEND` target, labelled
 * "Quick" in the share sheet. Not configurable at all: always applies the
 * default tracking-parameter rules, strips surrounding text, and keeps only
 * the first link if there's more than one. No UI, ever - that's the point.
 */
class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText()
        if (sharedText == null) {
            finish()
            return
        }

        val prefs = Prefs(this)
        val config = prefs.toConfig().copy(cleanSurroundingText = true)
        val found = LinkSanitiser.findLinks(sharedText, config)
        val keepIndices = if (found.isNotEmpty()) setOf(0) else emptySet()
        val result = LinkSanitiser.buildResult(sharedText, config, found, keepIndices)

        reshareCleaned(result.text, result.paramsRemoved, prefs)
    }
}
