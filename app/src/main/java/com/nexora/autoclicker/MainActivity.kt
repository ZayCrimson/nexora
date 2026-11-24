package com.nexora.autoclicker

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private val navy = Color.rgb(10, 15, 31)
    private val card = Color.rgb(23, 32, 57)
    private val cyan = Color.rgb(70, 225, 238)
    private val white = Color.WHITE
    private lateinit var content: LinearLayout
    private val targets by lazy { TargetStore.load(this) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(colors: IntArray, radius: Int = 20) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = dp(radius).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        val scroll = ScrollView(this).apply { setBackgroundColor(navy) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(30))
        }
        scroll.addView(content)
        setContentView(scroll)
        render()
    }

    private fun render() {
        content.removeAllViews()
        content.addView(label("NEXORA", 30f, cyan, true))
        content.addView(label("PRECISION TAP CONTROL", 11f, 0xFF9AAAC7.toInt(), true).apply {
            letterSpacing = .18f
            setPadding(0, dp(4), 0, dp(22))
        })
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = shape(intArrayOf(0xFF203B62.toInt(), 0xFF30275D.toInt()), 24)
        }
        hero.addView(label("Your control center", 21f, white, true))
        hero.addView(label("Four independent targets. Custom timing. Floating control.", 13f, 0xFFD0D9EA.toInt(), false).apply {
            setPadding(0, dp(8), 0, dp(16))
        })
        hero.addView(actionButton("OPEN FLOATING CONTROLLER", cyan) { openController() })
        content.addView(hero, marginBottom(20))

        content.addView(label("PERMISSIONS", 15f, white, true), marginBottom(10))
        content.addView(permissionCard("01", "Floating window", "Display NEXORA over other apps", "GRANT / CHECK") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else toast("Floating window permission is already enabled")
        }, marginBottom(10))
        content.addView(permissionCard("02", "Accessibility service", "Allow NEXORA to dispatch tap gestures", "OPEN SETTINGS") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, marginBottom(22))

        content.addView(label("TARGET CONFIGURATION", 15f, white, true), marginBottom(6))
        content.addView(label("Timing values are saved automatically.", 12f, 0xFF98A5BD.toInt(), false), marginBottom(12))
        targets.forEach { t -> content.addView(targetCard(t), marginBottom(12)) }
        content.addView(label("HIT DURATION PRESET", 15f, white, true), marginBottom(8))
        val preset = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = shape(intArrayOf(card, 0xFF1A2948.toInt()), 18)
        }
        preset.addView(label("450 ms per hit", 17f, cyan, true))
        preset.addView(label("2 hits = 900 ms  •  4 hits = 1800 ms", 12f, 0xFFB7C3D8.toInt(), false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        content.addView(preset)
        content.addView(label("NEXORA only taps when START is pressed. Use STOP to end the loop. Minimize hides the panel and target markers while the click loop continues.", 12f, 0xFF8998B4.toInt(), false).apply {
            setPadding(0, dp(20), 0, 0)
        })
    }

    private fun targetCard(t: TargetConfig): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = shape(intArrayOf(card, 0xFF1A2948.toInt()), 18)
        }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(label("TARGET ${t.id}", 15f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        val enabled = Switch(this).apply {
            text = "Enabled"; setTextColor(white); isChecked = t.enabled
            setOnCheckedChangeListener { _, checked -> t.enabled = checked; saveTargets() }
        }
        head.addView(enabled)
        box.addView(head)
        val fields = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val start = numberField("Start delay", t.startDelay)
        val end = numberField("End delay", t.endDelay)
        val duration = numberField("Duration", t.duration)
        fields.addView(start, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        fields.addView(end, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(6) })
        fields.addView(duration, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(fields)
        val apply = actionButton("SAVE TARGET ${t.id}", 0xFF354C76.toInt()) {
            t.startDelay = start.tag.toString().toLongOrNull() ?: t.startDelay
            t.endDelay = end.tag.toString().toLongOrNull() ?: t.endDelay
            t.duration = duration.tag.toString().toLongOrNull() ?: t.duration
            saveTargets()
            toast("Target ${t.id} saved")
        }
        box.addView(apply, marginTop(10))
        if (t.id == 3) {
            val hits = EditText(this).apply {
                hint = "Hit count (e.g. 2 or 4)"
                setSingleLine()
                inputType = 2
                setTextColor(white); setHintTextColor(0xFF8998B4.toInt())
                setPadding(dp(10), 0, dp(10), 0)
                background = shape(intArrayOf(0xFF101A30.toInt(), 0xFF101A30.toInt()), 10)
            }
            val presetBtn = actionButton("APPLY HIT PRESET", 0xFF4C3C85.toInt()) {
                val count = hits.text.toString().toIntOrNull()
                if (count == null || count !in 1..100) toast("Enter hit count from 1 to 100")
                else {
                    t.duration = count * 450L
                    duration.setText(t.duration.toString())
                    duration.tag = t.duration
                    saveTargets()
                    toast("$count hits = ${t.duration} ms")
                }
            }
            box.addView(hits, marginTop(10))
            box.addView(presetBtn, marginTop(6))
        }
        return box
    }

    private fun numberField(title: String, value: Long): EditText {
        val wrap = LinearLayout(this)
        val edit = EditText(this).apply {
            setText(value.toString()); tag = value
            setTextColor(white); setHintTextColor(0xFF8998B4.toInt())
            textSize = 14f; inputType = 2; setSingleLine()
            background = shape(intArrayOf(0xFF101A30.toInt(), 0xFF101A30.toInt()), 10)
            setPadding(dp(8), 0, dp(4), 0)
        }
        // The label is kept in the EditText hint to keep the compact three-column editor readable.
        edit.hint = title
        edit.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) v.tag = (v as EditText).text.toString().toLongOrNull() ?: value
        }
        return edit
    }

    private fun saveTargets() {
        targets.forEach { t ->
            // Values are read from the editor on Save; target position and toggles persist immediately.
        }
        TargetStore.save(this, targets)
    }

    private fun permissionCard(number: String, title: String, subtitle: String, action: String, click: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(14), dp(12), dp(14))
            background = shape(intArrayOf(card, 0xFF1B2A49.toInt()), 16)
        }
        val textCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textCol.addView(label(number + "  " + title, 14f, white, true))
        textCol.addView(label(subtitle, 11f, 0xFF9EABC2.toInt(), false).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(textCol, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = action; textSize = 10f; setTextColor(cyan); gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8)); setOnClickListener { click() }
        })
        return row
    }

    private fun actionButton(textValue: String, color: Int, click: () -> Unit) = TextView(this).apply {
        text = textValue; gravity = Gravity.CENTER; textSize = 12f
        typeface = Typeface.DEFAULT_BOLD; setTextColor(if (color == cyan) navyColor() else white)
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = shape(intArrayOf(color, color), 13)
        setOnClickListener { click() }
    }

    private fun label(s: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun navyColor() = Color.rgb(10, 15, 31)
    private fun marginBottom(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(v) }
    private fun marginTop(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(v) }
    private fun openController() {
        if (!Settings.canDrawOverlays(this)) {
            toast("Grant floating window permission first")
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (ClickAccessibilityService.current == null) {
            toast("Enable NEXORA in Accessibility settings first")
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }
        startService(Intent(this, FloatingService::class.java))
        finish()
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun onResume() { super.onResume(); if (::content.isInitialized) render() }
}
