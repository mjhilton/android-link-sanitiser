package id.hiltons.linksanitiser

import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import id.hiltons.linksanitiser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Opt into edge-to-edge ourselves rather than relying on whatever the device's
        // own OS version defaults to: Android 15+ forces this regardless, but on older
        // versions it's still off unless requested, which would otherwise make the
        // insets handling below behave differently (or not fire at all) depending on
        // which OS the app happens to be running on.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        // Now that we're edge-to-edge on every OS version, the system no longer pads
        // the root view for us - not for the status/nav bars, and not for the
        // keyboard - so both have to be applied by hand from the dispatched insets.
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollRoot) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(top = bars.top, bottom = maxOf(imeBottom, bars.bottom))
            if (imeBottom > 0) {
                // updatePadding() only requests a layout; it hasn't happened yet, so
                // asking for the focused view's rect right now would use its
                // pre-keyboard position and undershoot. Wait for that layout to
                // actually land before asking again.
                view.doOnLayout {
                    view.findFocus()?.let { focused ->
                        focused.requestRectangleOnScreen(Rect(0, 0, focused.width, focused.height), true)
                    }
                }
            }
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
