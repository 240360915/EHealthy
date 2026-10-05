package ehealthy.connect.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

/**
 * Shows a Toast with bold, large text so important messages (like
 * "you haven't registered yet") are clearly visible to the user.
 */
fun showBoldToast(context: Context, message: String) {
    val textView = TextView(context).apply {
        text = message
        setTextColor(Color.WHITE)
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(48, 32, 48, 32)
        setBackgroundColor(Color.parseColor("#CC000000"))
        gravity = Gravity.CENTER
    }

    Toast(context).apply {
        duration = Toast.LENGTH_LONG
        view = textView
        show()
    }
}