package id.hiltons.linksanitiser

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import id.hiltons.linksanitiser.databinding.ActivityCustomShareBinding

/**
 * Registered as an `ACTION_SEND` target, labelled "Custom" in the share
 * sheet. Starts from the same defaults as "Quick" (strip surrounding text,
 * keep only the first link) but lets you override any of that for this
 * share only, with a live preview of the result. Nothing here is saved.
 */
class CustomShareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomShareBinding
    private lateinit var prefs: Prefs
    private lateinit var sharedText: String
    private lateinit var found: List<FoundLink>
    private var linkCheckboxes: List<CheckBox> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingText = extractSharedText()
        if (incomingText == null) {
            finish()
            return
        }
        sharedText = incomingText
        prefs = Prefs(this)

        binding = ActivityCustomShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.switchStripUtm.isChecked = prefs.stripUtm
        binding.switchStripClickIds.isChecked = prefs.stripClickIds
        binding.switchStripReferral.isChecked = prefs.stripReferral
        // Same starting point as "Quick": strip surrounding text, first link only.
        binding.switchCleanSurroundingText.isChecked = true

        // Finding links doesn't depend on which tracker categories are enabled, so this
        // stays valid however the switches below get toggled afterwards.
        found = LinkSanitiser.findLinks(sharedText, prefs.toConfig())

        if (found.size > 1) {
            binding.linksSection.visibility = View.VISIBLE
            val spacingPx = (12 * resources.displayMetrics.density).toInt()
            linkCheckboxes = found.mapIndexed { index, link ->
                CheckBox(this).apply {
                    text = link.original
                    isChecked = index == 0
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = spacingPx }
                }
            }
            linkCheckboxes.forEach { binding.linksContainer.addView(it) }
        }

        val onChanged = CompoundButton.OnCheckedChangeListener { _, _ -> updatePreview() }
        binding.switchStripUtm.setOnCheckedChangeListener(onChanged)
        binding.switchStripClickIds.setOnCheckedChangeListener(onChanged)
        binding.switchStripReferral.setOnCheckedChangeListener(onChanged)
        binding.switchCleanSurroundingText.setOnCheckedChangeListener(onChanged)
        linkCheckboxes.forEach { it.setOnCheckedChangeListener(onChanged) }

        updatePreview()

        binding.shareButton.setOnClickListener {
            val result = computeResult()
            reshareCleaned(result.text, result.paramsRemoved, prefs)
        }
    }

    private fun currentConfig(): SanitiserConfig = prefs.toConfig().copy(
        stripUtm = binding.switchStripUtm.isChecked,
        stripClickIds = binding.switchStripClickIds.isChecked,
        stripReferral = binding.switchStripReferral.isChecked,
        cleanSurroundingText = binding.switchCleanSurroundingText.isChecked,
    )

    private fun keepIndices(): Set<Int> = when {
        linkCheckboxes.isNotEmpty() -> linkCheckboxes.indices.filter { linkCheckboxes[it].isChecked }.toSet()
        found.isNotEmpty() -> setOf(0)
        else -> emptySet()
    }

    /** Re-finds links under the current switch states, since strip settings affect cleaning. */
    private fun computeResult(): SanitiseResult {
        val config = currentConfig()
        val freshFound = LinkSanitiser.findLinks(sharedText, config)
        return LinkSanitiser.buildResult(sharedText, config, freshFound, keepIndices())
    }

    private fun updatePreview() {
        binding.previewText.text = computeResult().text.ifBlank { getString(R.string.custom_preview_empty) }
    }
}
