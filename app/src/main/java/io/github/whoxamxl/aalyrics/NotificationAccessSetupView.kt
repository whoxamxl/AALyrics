package io.github.whoxamxl.aalyrics

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

internal fun Activity.createNotificationAccessSetupView(
    onGrantAccess: () -> Unit,
): View {
    val density = resources.displayMetrics.density
    fun dp(value: Int): Int = (value * density).roundToInt()

    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(32), dp(48), dp(32), dp(40))
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    content.addView(
        ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply {
                bottomMargin = dp(28)
            }
        },
    )

    content.addView(
        TextView(this).apply {
            setText(R.string.notification_access_eyebrow)
            setTextColor(Color.rgb(155, 164, 178))
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            letterSpacing = 0.08f
        },
    )

    content.addView(
        TextView(this).apply {
            setText(R.string.notification_access_title)
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(10)
            }
        },
    )

    content.addView(
        TextView(this).apply {
            setText(R.string.notification_access_body)
            setTextColor(Color.rgb(201, 207, 217))
            textSize = 16f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            }
        },
    )

    content.addView(
        TextView(this).apply {
            setText(R.string.notification_access_reason)
            setTextColor(Color.rgb(155, 164, 178))
            textSize = 14f
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.15f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(14)
            }
        },
    )

    content.addView(
        Button(this).apply {
            setText(R.string.notification_access_grant)
            isAllCaps = false
            textSize = 16f
            minimumHeight = dp(52)
            setOnClickListener { onGrantAccess() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(30)
            }
        },
    )

    content.addView(
        TextView(this).apply {
            setText(R.string.notification_access_footer)
            setTextColor(Color.rgb(128, 138, 153))
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(18)
            }
        },
    )

    return ScrollView(this).apply {
        setBackgroundColor(Color.rgb(11, 13, 17))
        isFillViewport = true
        addView(
            LinearLayout(this@createNotificationAccessSetupView).apply {
                gravity = Gravity.CENTER
                addView(content)
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }
}
