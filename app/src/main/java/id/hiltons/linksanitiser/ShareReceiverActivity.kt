package id.hiltons.linksanitiser

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

/**
 * Invisible activity registered as an `ACTION_SEND` target so it shows up
 * in the system share sheet. Strips tracking parameters from any link in
 * the shared text and immediately re-opens the share sheet with the
 * cleaned text so the user can pick the real destination.
 */
class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }

        if (sharedText.isNullOrBlank()) {
            finish()
            return
        }

        val prefs = Prefs(this)
        val result = LinkSanitiser.sanitise(sharedText, prefs.toConfig())

        if (result.paramsRemoved > 0) {
            prefs.incrementCount(result.paramsRemoved)
            if (prefs.showToast) {
                val paramWord = if (result.paramsRemoved == 1) "parameter" else "parameters"
                Toast.makeText(
                    this,
                    getString(R.string.toast_cleaned, result.paramsRemoved, paramWord),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        } else if (prefs.showToast) {
            Toast.makeText(this, R.string.toast_nothing_to_clean, Toast.LENGTH_SHORT).show()
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, result.text)
        }

        val chooser = Intent.createChooser(sendIntent, null).apply {
            // Don't offer ourselves again as a destination for the already-cleaned link.
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(this@ShareReceiverActivity, ShareReceiverActivity::class.java)))
        }

        startActivity(chooser)
        finish()
    }
}
