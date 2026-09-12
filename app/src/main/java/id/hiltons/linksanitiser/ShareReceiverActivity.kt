package id.hiltons.linksanitiser

import android.app.Activity
import android.os.Bundle

/**
 * Invisible activity registered as an `ACTION_SEND` target, labelled
 * "Quick" in the share sheet. Applies the configured default settings with
 * no further input - unless those defaults are "Choose" links to keep and
 * there's more than one link, which still needs the picker UI.
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
        processAndReshare(sharedText, prefs.toConfig(), prefs)
    }
}
