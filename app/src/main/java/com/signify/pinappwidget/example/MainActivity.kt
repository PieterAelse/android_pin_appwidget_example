package com.signify.pinappwidget.example

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MainActivity : AppCompatActivity(), Observer {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.buttonCheckSupported).setOnClickListener {
            checkLauncherAndIsRequestAppWidgetPinningSupported()
        }

        findViewById<Button>(R.id.buttonRequest).setOnClickListener {
            requestPinAppWidget()
        }

        // Simple/lazy way to get data from a BroadcastReceiver to the Activity.
        ObservableObject.instance.addObserver(this)
    }

    override fun onResume() {
        super.onResume()

        // Do also check in onResume so the status is updated when app is brought to the front,
        // a user could have changed the default Launcher while the app was in the background.
        checkLauncherAndIsRequestAppWidgetPinningSupported()
    }

    // Override of [Observer]. Simple/lazy way to get data from a BroadcastReceiver to the Activity.
    override fun update(observable: Observable, data: Any) {
        findViewById<TextView>(R.id.pinSuccessResult).text =
            buildSpannedString {
                append("Pin")
                bold { append(" success ") }
                append("result:\n")
                bold { append("Widget with ID $data successfully placed!") }
            }
    }

    // See: https://developer.android.com/reference/android/appwidget/AppWidgetManager#isRequestPinAppWidgetSupported()
    @SuppressLint("SetTextI18n")
    fun checkLauncherAndIsRequestAppWidgetPinningSupported() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncherPackage = resolveInfo!!.activityInfo.packageName
        val currentLauncherName =
            packageManager.getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
        val packageInfo = packageManager.getPackageInfo(currentLauncherPackage, 0)
        val currentLauncherVersion = packageInfo.versionName
        @Suppress("DEPRECATION") val currentLauncherVersionCode = packageInfo.versionCode

        val appWidgetManager = AppWidgetManager.getInstance(this)
        val result = appWidgetManager.isRequestPinAppWidgetSupported

        val launcherNameAndVersion =
            "$currentLauncherName (version: $currentLauncherVersion, $currentLauncherVersionCode)"
        findViewById<TextView>(R.id.supportResult).text = buildSpannedString {
            append("Current")
            bold { append(" default ") }
            append("launcher:\n")
            bold { append(launcherNameAndVersion) }
            append("\n\nIs Pin AppWidget ")
            bold { append("supported: ${result.toString().uppercase()}") }
        }
        findViewById<TextView>(R.id.supportResult).setOnClickListener {
            val clipboard: ClipboardManager =
                getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PinAppWidgetTester", launcherNameAndVersion)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(
                findViewById(android.R.id.content),
                "Launcher information copied to clipboard!",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // See: https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,%20android.os.Bundle,%20android.app.PendingIntent)
    @SuppressLint("SetTextI18n")
    fun requestPinAppWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)

        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            findViewById<TextView>(R.id.pinRequestResult).text =
                buildSpannedString {
                    append("Pin")
                    bold { append(" request ") }
                    append("result: ")
                    bold { append("NOT SUPPORTED") }
                }
            return
        }

        resetTexts()

        try {
            val componentName = ComponentName(applicationContext, SimpleWidgetProvider::class.java)
            val intent = Intent(applicationContext, WidgetPinnedReceiver::class.java)

            val successCallback = PendingIntent.getBroadcast(
                applicationContext,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )

            val preview = RemoteViews(applicationContext.packageName, R.layout.some_1x1_widget)
            val bundle = Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview)
            }

            val pinResult =
                appWidgetManager.requestPinAppWidget(componentName, bundle, successCallback)

            findViewById<TextView>(R.id.pinRequestResult).text =
                buildSpannedString {
                    append("Pin")
                    bold { append(" request ") }
                    append("result: ")
                    bold { append("RETURNED: ${pinResult.toString().uppercase()}") }
                }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.pinRequestResult).text =
                buildSpannedString {
                    append("Pin")
                    bold { append(" request ") }
                    append("result: ")
                    bold { append("EXCEPTION: $e") }
                }
        }
    }

    private fun resetTexts() {
        findViewById<TextView>(R.id.pinRequestResult).text =
            buildSpannedString {
                append("Pin")
                bold { append(" request ") }
                append("result: ...")
            }
        findViewById<TextView>(R.id.pinSuccessResult).text =
            buildSpannedString {
                append("Pin")
                bold { append(" success ") }
                append("result: ...")
            }
    }
}

// This receiver will be called when the request to pin a widget was successful.
// This receive WILL NOT be called when the request failed, was cancelled by the user or when the widget was not placed
// on the home screen. That is unfortunately not part of the available [AppWidgetManager.requestPinAppWidget] API.
//
// See: https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,%20android.os.Bundle,%20android.app.PendingIntent)
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        ObservableObject.instance.updateValue(widgetId)
    }
}

// Simple/lazy way to get data from a BroadcastReceiver to the Activity.
class ObservableObject private constructor() : Observable() {
    companion object {
        val instance = ObservableObject()
    }

    fun updateValue(data: Any?) {
        synchronized(this) {
            setChanged()
            notifyObservers(data)
        }
    }
}