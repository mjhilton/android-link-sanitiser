package id.hiltons.linksanitiser

import android.app.Activity
import android.os.Bundle
import id.hiltons.linksanitiser.databinding.ActivityAdvancedShareBinding

/**
 * Registered as an `ACTION_SEND` target, labelled "Advanced" in the share
 * sheet. Shows a small form - pre-filled from the configured defaults -
 * letting the user override settings for this share only; nothing here is
 * saved back to the defaults.
 */
class AdvancedShareActivity : Activity() {

    private lateinit var binding: ActivityAdvancedShareBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText()
        if (sharedText == null) {
            finish()
            return
        }

        binding = ActivityAdvancedShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = Prefs(this)
        binding.switchStripUtm.isChecked = prefs.stripUtm
        binding.switchStripClickIds.isChecked = prefs.stripClickIds
        binding.switchStripReferral.isChecked = prefs.stripReferral
        binding.switchCleanSurroundingText.isChecked = prefs.cleanSurroundingText
        binding.linksToKeepGroup.check(
            when (prefs.linksToKeep) {
                LinksToKeep.ALL -> binding.linksToKeepAll.id
                LinksToKeep.FIRST -> binding.linksToKeepFirst.id
                LinksToKeep.CHOOSE -> binding.linksToKeepChoose.id
            },
        )

        binding.shareButton.setOnClickListener {
            val linksToKeep = when (binding.linksToKeepGroup.checkedRadioButtonId) {
                binding.linksToKeepFirst.id -> LinksToKeep.FIRST
                binding.linksToKeepChoose.id -> LinksToKeep.CHOOSE
                else -> LinksToKeep.ALL
            }
            val config = prefs.toConfig().copy(
                stripUtm = binding.switchStripUtm.isChecked,
                stripClickIds = binding.switchStripClickIds.isChecked,
                stripReferral = binding.switchStripReferral.isChecked,
                cleanSurroundingText = binding.switchCleanSurroundingText.isChecked,
                linksToKeep = linksToKeep,
            )
            processAndReshare(sharedText, config, prefs)
        }
    }
}
