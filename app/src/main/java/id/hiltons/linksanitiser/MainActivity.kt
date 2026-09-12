package id.hiltons.linksanitiser

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
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
