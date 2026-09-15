package io.github.whoxamxl.aalyrics

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            TextView(this).apply {
                text = "AALyrics\nFoundation build"
                gravity = Gravity.CENTER
                textSize = 22f
            }
        )
    }
}
