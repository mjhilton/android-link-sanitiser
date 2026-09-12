package id.hiltons.linksanitiser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import id.hiltons.linksanitiser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        // Ensure the scroll view can push its content above the keyboard (and, on
        // edge-to-edge devices, above the nav bar) rather than letting either cover it.
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollRoot) { view, insets ->
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.updatePadding(bottom = maxOf(imeBottom, systemBarsBottom))
            insets
        }

        binding.switchStripUtm.isChecked = prefs.stripUtm
        binding.switchStripClickIds.isChecked = prefs.stripClickIds
        binding.switchStripReferral.isChecked = prefs.stripReferral
        binding.switchShowToast.isChecked = prefs.showToast
        binding.customParamsInput.setText(prefs.customParamsRaw)

        binding.switchStripUtm.setOnCheckedChangeListener { _, checked -> prefs.stripUtm = checked }
        binding.switchStripClickIds.setOnCheckedChangeListener { _, checked -> prefs.stripClickIds = checked }
        binding.switchStripReferral.setOnCheckedChangeListener { _, checked -> prefs.stripReferral = checked }
        binding.switchShowToast.setOnCheckedChangeListener { _, checked -> prefs.showToast = checked }

        binding.customParamsInput.doAfterTextChanged { text ->
            prefs.customParamsRaw = text?.toString().orEmpty()
        }

        binding.resetButton.setOnClickListener {
            prefs.linksCleanedCount = 0
            updateCounterDisplay()
        }

        updateCounterDisplay()
    }

    override fun onResume() {
        super.onResume()
        // The counter may have changed via ShareReceiverActivity while we were backgrounded.
        updateCounterDisplay()
    }

    private fun updateCounterDisplay() {
        binding.counterValue.text = prefs.linksCleanedCount.toString()
    }
}
