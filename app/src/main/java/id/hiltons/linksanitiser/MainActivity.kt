package id.hiltons.linksanitiser

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import id.hiltons.linksanitiser.databinding.ActivityMainBinding

/**
 * The launcher screen. Deliberately not configurable - all cleaning
 * settings live in the "Custom" share target now - just a running tally
 * and a look at which domains you've cleaned the most.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        binding.resetButton.setOnClickListener {
            prefs.resetStats()
            updateDisplay()
        }

        updateDisplay()
    }

    override fun onResume() {
        super.onResume()
        // Stats may have changed via a share target while we were backgrounded.
        updateDisplay()
    }

    private fun updateDisplay() {
        binding.counterValue.text = prefs.linksCleanedCount.toString()

        val topDomains = prefs.topDomains(limit = 5)
        if (topDomains.isEmpty()) {
            binding.topDomainsText.text = getString(R.string.top_domains_empty)
        } else {
            binding.topDomainsText.text = topDomains.joinToString("\n") { (domain, count) ->
                val displayDomain = domain.replaceFirstChar { it.uppercase() }
                val cleanWord = if (count == 1) getString(R.string.clean_singular) else getString(R.string.clean_plural)
                getString(R.string.top_domains_row, displayDomain, count, cleanWord)
            }
        }
    }
}
