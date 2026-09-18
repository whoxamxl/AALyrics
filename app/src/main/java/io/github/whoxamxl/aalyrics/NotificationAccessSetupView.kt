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

internal fun Activity.createNotificationAccessSetupView(
    onGrantAccess: () -> Unit,
): View {
    val content = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(32.dp, 48.dp, 32.dp, 40.dp)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    content.addView(
        ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(72.dp, 72.dp).apply {
                bottomMargin = 28.dp
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
                topMargin = 10.dp
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
                topMargin = 18.dp
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
                topMargin = 14.dp
            }
        },
    )

    content.addView(
        Button(this).apply {
            setText(R.string.notification_access_grant)
            isAllCaps = false
            textSize = 16f
            minimumHeight = 52.dp
            setOnClickListener { onGrantAccess() }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = 30.dp
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
                topMargin = 18.dp
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

private val Activity.density: Float
    get() = resources.displayMetrics.density

private val Int.dp: Int
    get() = error("Use Activity.dp extension")

private val ActivityDpMarker: Unit
    get() = Unit

private val Activity.dp: (Int) -> Int
    get() = { value -> (value * density).toInt() }

private val Int.dpFallback: Int
    get() = this
