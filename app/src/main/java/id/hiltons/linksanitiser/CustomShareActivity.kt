package id.hiltons.linksanitiser

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import id.hiltons.linksanitiser.databinding.ActivityCustomShareBinding

/**
 * Registered as an `ACTION_SEND` target, labelled "Custom" in the share
 * sheet. Starts from the same defaults as "Quick" (strip surrounding text,
 * keep only the first link) but lets you override any setting for this
 * share, with a live preview of the result. Changes are remembered for next
 * time, same as the "Quick" defaults they both draw from.
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
        binding.switchCleanSurroundingText.isChecked = prefs.cleanSurroundingText
        binding.switchShowToast.isChecked = prefs.showToast
        binding.customParamsInput.setText(prefs.customParamsRaw)

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
        binding.customParamsInput.doAfterTextChanged { updatePreview() }

        updatePreview()

        binding.shareButton.setOnClickListener {
            persistSettings()
            val config = currentConfig()
            val freshFound = LinkSanitiser.findLinks(sharedText, config)
            val kept = keepIndices()
            val result = LinkSanitiser.buildResult(sharedText, config, freshFound, kept)
            reshareCleaned(result.text, kept.map { freshFound[it] }, prefs)
        }
    }

    private fun customParamsRaw(): String = binding.customParamsInput.text?.toString().orEmpty()

    private fun currentConfig(): SanitiserConfig = prefs.toConfig().copy(
        stripUtm = binding.switchStripUtm.isChecked,
        stripClickIds = binding.switchStripClickIds.isChecked,
        stripReferral = binding.switchStripReferral.isChecked,
        cleanSurroundingText = binding.switchCleanSurroundingText.isChecked,
        customParams = customParamsRaw().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
    )

    private fun keepIndices(): Set<Int> = when {
        linkCheckboxes.isNotEmpty() -> linkCheckboxes.indices.filter { linkCheckboxes[it].isChecked }.toSet()
        found.isNotEmpty() -> setOf(0)
        else -> emptySet()
    }

    /** Re-finds links under the current switch states, since strip settings affect cleaning. */
    private fun computeFreshFound(): List<FoundLink> = LinkSanitiser.findLinks(sharedText, currentConfig())

    private fun computeResult(): SanitiseResult {
        val config = currentConfig()
        return LinkSanitiser.buildResult(sharedText, config, computeFreshFound(), keepIndices())
    }

    private fun updatePreview() {
        binding.previewText.text = computeResult().text.ifBlank { getString(R.string.custom_preview_empty) }
    }

    /** Remembers this share's settings as the starting point for next time. */
    private fun persistSettings() {
        prefs.stripUtm = binding.switchStripUtm.isChecked
        prefs.stripClickIds = binding.switchStripClickIds.isChecked
        prefs.stripReferral = binding.switchStripReferral.isChecked
        prefs.cleanSurroundingText = binding.switchCleanSurroundingText.isChecked
        prefs.showToast = binding.switchShowToast.isChecked
        prefs.customParamsRaw = customParamsRaw()
    }

    /** Tapping anywhere outside the focused text field clears its focus and dismisses the keyboard. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focused = currentFocus
            if (focused is EditText) {
                val bounds = Rect()
                focused.getGlobalVisibleRect(bounds)
                if (!bounds.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focused.clearFocus()
                    val imm = getSystemService(InputMethodManager::class.java)
                    imm?.hideSoftInputFromWindow(focused.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
