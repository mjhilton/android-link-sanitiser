package id.hiltons.linksanitiser

import android.app.Activity
import android.os.Bundle
import android.widget.CheckBox
import id.hiltons.linksanitiser.databinding.ActivityLinkPickerBinding

/**
 * Shown when "Links to keep" is set to Choose and a share contains more
 * than one link. Lets the user pick which links survive; everything else
 * (surrounding text) is discarded, and the kept links are shared newline
 * separated - launched internally only, never as a share target itself.
 */
class LinkPickerActivity : Activity() {

    private lateinit var binding: ActivityLinkPickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val candidates = intent.getStringArrayExtra(EXTRA_CANDIDATES).orEmpty()
        val paramsRemoved = intent.getIntArrayExtra(EXTRA_PARAMS_REMOVED) ?: IntArray(candidates.size)

        if (candidates.isEmpty()) {
            finish()
            return
        }

        val checkboxes = candidates.map { link ->
            CheckBox(this).apply {
                text = link
                isChecked = false
            }
        }
        checkboxes.forEach { binding.linkCheckboxContainer.addView(it) }

        binding.confirmButton.setOnClickListener {
            val keptIndices = checkboxes.indices.filter { checkboxes[it].isChecked }
            val text = keptIndices.joinToString("\n") { candidates[it] }
            val totalParamsRemoved = keptIndices.sumOf { paramsRemoved[it] }
            reshareCleaned(text, totalParamsRemoved, Prefs(this))
        }
    }

    companion object {
        const val EXTRA_CANDIDATES = "id.hiltons.linksanitiser.extra.CANDIDATES"
        const val EXTRA_PARAMS_REMOVED = "id.hiltons.linksanitiser.extra.PARAMS_REMOVED"
    }
}
