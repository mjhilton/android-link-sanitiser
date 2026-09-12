package id.hiltons.linksanitiser

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.TypefaceSpan
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

        val paramsCount = prefs.paramsStrippedCount
        val paramWord = if (paramsCount == 1) getString(R.string.param_singular) else getString(R.string.param_plural)
        binding.counterSublabel.text = getString(R.string.counter_sublabel, paramsCount, paramWord)

        val topDomains = prefs.topDomains(limit = 5)
        if (topDomains.isEmpty()) {
            binding.topDomainsText.text = getString(R.string.top_domains_empty)
        } else {
            val rows = SpannableStringBuilder()
            topDomains.forEachIndexed { index, (domain, count) ->
                if (index > 0) rows.append("\n")
                val domainStart = rows.length
                rows.append(domain)
                rows.setSpan(TypefaceSpan("monospace"), domainStart, rows.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val cleanWord = if (count == 1) getString(R.string.clean_singular) else getString(R.string.clean_plural)
                rows.append(getString(R.string.top_domains_row_suffix, count, cleanWord))
            }
            binding.topDomainsText.text = rows
        }
    }
}
