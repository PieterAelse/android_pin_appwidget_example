package com.signify.pinappwidget.example

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WidgetConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)

        findViewById<Button>(R.id.configureButtonDone).setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}