package gh.otumfour.o24.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import gh.otumfour.o24.R
import gh.otumfour.o24.theme.ThemeId

fun AppCompatActivity.showThemeHistory(theme: ThemeId) {
    val density = resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(8))
        setBackgroundColor(Color.parseColor("#1A1408"))
    }
    column.addView(TextView(this).apply {
        text = getString(theme.titleRes)
        setTextColor(Color.parseColor("#F3D37A"))
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
    })
    column.addView(TextView(this).apply {
        text = getString(theme.historyRes)
        setTextColor(Color.parseColor("#F6F1E4"))
        textSize = 15f
        setLineSpacing(dp(2).toFloat(), 1.15f)
        setPadding(0, dp(10), 0, dp(10))
    })
    column.addView(TextView(this).apply {
        text = getString(R.string.history_disclaimer)
        setTextColor(Color.parseColor("#CDBF98"))
        textSize = 12f
    })
    val scroll = ScrollView(this).apply { addView(column) }
    AlertDialog.Builder(this)
        .setView(scroll)
        .setPositiveButton(R.string.close, null)
        .create()
        .also { dialog ->
            dialog.setOnShowListener {
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#1A1408")))
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.parseColor("#F3D37A"))
            }
            dialog.show()
            scroll.layoutParams = scroll.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
}
