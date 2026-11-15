package gh.otumfour.o24.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

fun slideBackButton(context: Context, onClick: () -> Unit): Button = Button(context).apply {
    text = "‹"
    textSize = 28f
    isAllCaps = false
    setTextColor(Color.parseColor("#E8B52D"))
    background = null
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )
}

fun goldPill(context: Context, label: String, onClick: () -> Unit): Button = Button(context).apply {
    text = label
    isAllCaps = false
    textSize = 16f
    setTextColor(Color.parseColor("#1A1408"))
    backgroundTintList = null
    background = GradientDrawable().apply {
        cornerRadius = 28f * resources.displayMetrics.density
        setColor(Color.parseColor("#E8B52D"))
    }
    setOnClickListener { onClick() }
}

fun outlinePill(context: Context, label: String, onClick: () -> Unit): Button = Button(context).apply {
    text = label
    isAllCaps = false
    textSize = 16f
    setTextColor(Color.parseColor("#E8B52D"))
    backgroundTintList = null
    background = GradientDrawable().apply {
        cornerRadius = 28f * resources.displayMetrics.density
        setColor(Color.TRANSPARENT)
        setStroke((1.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#E8B52D"))
    }
    setOnClickListener { onClick() }
}

fun slideTitle(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    textSize = 26f
    setTextColor(Color.parseColor("#F3D37A"))
    gravity = Gravity.CENTER
}

fun slideBody(context: Context, text: String): TextView = TextView(context).apply {
    this.text = text
    textSize = 16f
    setTextColor(Color.parseColor("#F6F1E4"))
    setLineSpacing(4f, 1.1f)
}
