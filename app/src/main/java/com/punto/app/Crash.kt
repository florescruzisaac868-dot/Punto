package com.punto.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Guarda el error si la app se cierra y lo muestra la siguiente vez que la abres.
fun Activity.installCrashGuard(): Boolean {
    val prefs = getSharedPreferences("punto", Context.MODE_PRIVATE)
    val saved = prefs.getString("crash", null)
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        prefs.edit().putString("crash", Log.getStackTraceString(e)).commit()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
    if (saved == null) return false

    val box = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 120, 40, 40)
    }
    val copy = Button(this).apply {
        text = "Copiar error"
        setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("error", saved))
        }
    }
    val retry = Button(this).apply {
        text = "Borrar error y reintentar"
        setOnClickListener {
            prefs.edit().remove("crash").apply()
            recreate()
        }
    }
    val tv = TextView(this).apply {
        text = saved
        textSize = 11f
        setTextColor(Color.WHITE)
        setTextIsSelectable(true)
    }
    box.addView(copy)
    box.addView(retry)
    box.addView(tv)
    setContentView(ScrollView(this).apply {
        setBackgroundColor(Color.BLACK)
        addView(box)
    })
    return true
}
