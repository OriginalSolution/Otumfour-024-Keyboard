package gh.otumfour.o24.ui

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import gh.otumfour.o24.R

/** Full splash artwork, fitted without stretching, shown briefly after the system splash. */
object LaunchArt {
    private const val HOLD_MS = 1600L

    fun show(activity: AppCompatActivity) {
        val root = activity.window.decorView as? ViewGroup ?: return
        val image = ImageView(activity).apply {
            setBackgroundColor(Color.BLACK)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(R.drawable.splash_art)
            contentDescription = activity.getString(R.string.app_name)
            isClickable = true
            setOnClickListener { dismiss() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(image) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        root.addView(
            image,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            ),
        )
        // Start the hold after the first frame. The system splash covers everything
        // until then, so a timer started in onCreate can expire before the art is seen.
        ViewCompat.requestApplyInsets(image)
        image.post { image.postDelayed({ image.dismiss() }, HOLD_MS) }
    }

    private fun ImageView.dismiss() {
        (parent as? ViewGroup)?.removeView(this)
    }
}
