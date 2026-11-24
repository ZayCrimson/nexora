package com.nexora.autoclicker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 9, 25)
    private val card = 0xE8111D3B.toInt()
    private val cyan = 0xFF20DDF5.toInt()
    private val blue = 0xFF168CFF.toInt()
    private val violet = 0xFF873CFF.toInt()
    private val pink = 0xFFFF55C8.toInt()
    private val white = Color.WHITE
    private val muted = 0xFF9EAFD3.toInt()
    private lateinit var root: LinearLayout
    private lateinit var page: LinearLayout
    private lateinit var navBar: LinearLayout
    private var currentTab = "HOME"
    private val targets by lazy { TargetStore.load(this) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun gradient(a: Int, b: Int, radius: Int = 18, stroke: Int? = null, strokeWidth: Int = 1) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply {
            cornerRadius = dp(radius).toFloat()
            stroke?.let { setStroke(dp(strokeWidth), it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildShell()
        renderPage()
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            isFillViewport = true
            setBackgroundColor(bg)
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(22))
        }
        scroll.addView(page)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        navBar = bottomNav()
        root.addView(navBar)
        setContentView(root)
    }

    private fun renderPage() {
        page.removeAllViews()
        when (currentTab) {
            "HOME" -> renderHome()
            "STATS" -> renderStats()
            else -> renderAbout()
        }
        refreshNav()
    }

    private fun renderHome() {
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.nexora_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(62), dp(62)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        brandText.addView(label("NEXORA", 25f, white, true))
        brandText.addView(label("PRECISION TAP CONTROL", 10f, cyan, true).apply { letterSpacing = .17f })
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brand.addView(roundIcon("⚙", violet) { showAppInfo() })
        brand.addView(space(8))
        brand.addView(roundIcon("i", blue) { currentTab = "ABOUT"; renderPage() })
        page.addView(brand, marginBottom(22))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(19), dp(18), dp(19), dp(17))
            background = gradient(0xF0103157.toInt(), 0xF027145B.toInt(), 23, 0xFF087FD6.toInt(), 1)
        }
        val heroTop = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        heroTop.addView(label("⌖", 42f, cyan, true), LinearLayout.LayoutParams(dp(65), dp(58)))
        val heroCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        heroCopy.addView(label("Your control center", 18f, white, true))
        heroCopy.addView(label("Tap. Automate. Dominate.", 12f, muted, false).apply { setPadding(0, dp(4), 0, 0) })
        heroTop.addView(heroCopy)
        hero.addView(heroTop)
        hero.addView(actionButton("▶   OPEN FLOATING CONTROLLER     ›", cyan, 0xFF07142A.toInt(), true) { openController() }, marginTop(14))
        page.addView(hero, marginBottom(22))

        val permissionHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        permissionHead.addView(label("╱╱  Permissions", 17f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        val overlayGranted = Settings.canDrawOverlays(this)
        val accessGranted = isAccessibilityEnabled()
        val allGranted = overlayGranted && accessGranted
        permissionHead.addView(pill(if (allGranted) "● Granted" else "● Setup required", if (allGranted) 0xFF16D98B.toInt() else cyan))
        page.addView(permissionHead, marginBottom(10))
        page.addView(permissionCard(
            "▱", "01  Floating window", "Display NEXORA over other apps",
            if (overlayGranted) "GRANTED" else "CHECK",
            {
                if (!overlayGranted) startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                else toast("Floating window permission is already enabled")
            }
        ), marginBottom(8))
        page.addView(permissionCard(
            "♙", "02  Accessibility service", "Allow NEXORA to dispatch tap gestures",
            if (accessGranted) "GRANTED" else "SETTINGS",
            { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        ), marginBottom(23))

        val targetHead = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        targetHead.addView(label("⌖", 30f, cyan, true))
        val thCopy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        thCopy.addView(label("Target Configuration", 17f, white, true))
        thCopy.addView(label("Adjust timing values for each target", 11f, muted, false).apply { setPadding(0, dp(3), 0, 0) })
        targetHead.addView(thCopy)
        page.addView(targetHead, marginBottom(13))

        targets.forEach { target -> page.addView(targetCard(target), marginBottom(9)) }
        page.addView(label("NEXORA  •  PRECISION TAP CONTROL", 9f, 0xFF63769D.toInt(), true).apply {
            gravity = Gravity.CENTER
            letterSpacing = .15f
            setPadding(0, dp(19), 0, 0)
        })
    }

    private fun targetCard(t: TargetConfig): View {
        val accent = when (t.id) { 3 -> pink; 2 -> violet; 4 -> blue; else -> cyan }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(10), dp(12))
            background = gradient(0xE9112043.toInt(), 0xE91A1644.toInt(), 20, if (t.id == 3) 0xFFFF5ACD.toInt() else 0xFF174B9B.toInt())
        }
        val number = TextView(this).apply {
            text = t.id.toString(); gravity = Gravity.CENTER; textSize = 19f
            typeface = Typeface.DEFAULT_BOLD; setTextColor(0xFF061126.toInt())
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(accent) }
        }
        box.addView(number, LinearLayout.LayoutParams(dp(42), dp(42)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(4), 0) }
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(label("Target ${t.id}", 14f, white, true))
        titleRow.addView(space(8))
        titleRow.addView(label("●", 10f, if (t.enabled) 0xFF16E68D.toInt() else 0xFF77839A.toInt(), true))
        titleRow.addView(label(if (t.enabled) " Enabled" else " Disabled", 10f, muted, false))
        info.addView(titleRow)
        info.addView(label("◷ Start: ${t.startDelay}ms   |   End: ${t.endDelay}ms   |   Hit: ${t.duration}ms", 9f, 0xFFB6C7E9.toInt(), false).apply {
            setPadding(0, dp(5), 0, 0)
        })
        box.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        val toggle = Switch(this).apply {
            isChecked = t.enabled
            showText = false
            scaleX = .82f; scaleY = .82f
            buttonTintList = android.content.res.ColorStateList.valueOf(accent)
            setOnCheckedChangeListener { _, checked ->
                t.enabled = checked
                TargetStore.save(this@MainActivity, targets)
                renderPage()
            }
        }
        box.addView(toggle)
        val action = TextView(this).apply {
            text = if (t.id == 3) "⚙" else "›"
            textSize = if (t.id == 3) 21f else 27f
            gravity = Gravity.CENTER
            setTextColor(if (t.id == 3) pink else 0xFF9EAFFF.toInt())
            setPadding(dp(5), 0, dp(2), 0)
            setOnClickListener { if (t.id == 3) showHitDialog(t) else showTargetDialog(t) }
        }
        box.addView(action, LinearLayout.LayoutParams(dp(29), dp(42)))
        box.setOnClickListener { showTargetDialog(t) }
        return box
    }

    private fun showHitDialog(t: TargetConfig) {
        val input = numericInput(t.duration.toString(), "Hit duration in milliseconds")
        val wrap = dialogWrap(input)
        AlertDialog.Builder(this)
            .setTitle("TARGET 3  •  HIT SETTINGS")
            .setMessage("Set the hit duration for Target 3.")
            .setView(wrap)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val value = input.text.toString().toLongOrNull()
                if (value == null || value !in 1L..60000L) toast("Enter a value from 1 to 60000 ms")
                else { t.duration = value; TargetStore.save(this, targets); renderPage(); toast("Target 3 hit saved: ${value}ms") }
            }.show()
    }

    private fun showTargetDialog(t: TargetConfig) {
        val start = numericInput(t.startDelay.toString(), "Start delay (ms)")
        val end = numericInput(t.endDelay.toString(), "End delay (ms)")
        val duration = numericInput(t.duration.toString(), if (t.id == 3) "Hit duration (ms)" else "Duration (ms)")
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(5), dp(20), 0) }
        fields.addView(dialogField("START DELAY", start), marginBottom(9))
        fields.addView(dialogField("END DELAY", end), marginBottom(9))
        fields.addView(dialogField(if (t.id == 3) "HIT DURATION" else "DURATION", duration))
        AlertDialog.Builder(this)
            .setTitle("TARGET ${t.id} SETTINGS")
            .setView(fields)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("SAVE") { _, _ ->
                val a = start.text.toString().toLongOrNull()
                val b = end.text.toString().toLongOrNull()
                val c = duration.text.toString().toLongOrNull()
                if (a == null || b == null || c == null || a < 0 || b < 0 || c !in 1L..60000L) toast("Check the timing values")
                else {
                    t.startDelay = a; t.endDelay = b; t.duration = c
                    TargetStore.save(this, targets); renderPage(); toast("Target ${t.id} saved")
                }
            }.show()
    }

    private fun renderStats() {
        page.addView(label("NEXORA STATS", 25f, white, true), marginBottom(5))
        page.addView(label("Your current tap configuration", 12f, muted, false), marginBottom(22))
        val enabled = targets.count { it.enabled }
        val stats = listOf("TOTAL TARGETS" to "4", "ACTIVE TARGETS" to enabled.toString(), "DISABLED TARGETS" to (4 - enabled).toString())
        stats.forEach { (title, value) ->
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(17), dp(18), dp(17))
                background = gradient(0xE9112043.toInt(), 0xE91B1646.toInt(), 18, 0xFF174B9B.toInt())
            }
            cardView.addView(label(title, 10f, muted, true))
            cardView.addView(label(value, 30f, cyan, true).apply { setPadding(0, dp(4), 0, 0) })
            page.addView(cardView, marginBottom(12))
        }
        page.addView(label("TARGET TIMINGS", 14f, white, true), marginTop(10))
        targets.forEach { t ->
            page.addView(label("Target ${t.id}  •  Start ${t.startDelay}ms  •  End ${t.endDelay}ms  •  Hit ${t.duration}ms", 12f, muted, false).apply {
                setPadding(dp(4), dp(11), dp(4), dp(11))
            })
        }
    }

    private fun renderAbout() {
        val logo = ImageView(this).apply { setImageResource(R.drawable.nexora_logo); scaleType = ImageView.ScaleType.FIT_CENTER }
        page.addView(logo, LinearLayout.LayoutParams(dp(130), dp(130)).apply { gravity = Gravity.CENTER; bottomMargin = dp(15) })
        page.addView(label("NEXORA", 27f, white, true).apply { gravity = Gravity.CENTER }, marginBottom(4))
        page.addView(label("PRECISION TAP CONTROL", 11f, cyan, true).apply { gravity = Gravity.CENTER; letterSpacing = .15f }, marginBottom(22))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            background = gradient(0xE9112043.toInt(), 0xE91A1644.toInt(), 20, 0xFF174B9B.toInt())
        }
        info.addView(label("About NEXORA", 17f, white, true), marginBottom(9))
        info.addView(label("NEXORA is a precision tap control app with four independent targets, configurable timing, and a floating controller.", 13f, muted, false))
        info.addView(label("Version 1.0.0", 11f, cyan, true), marginTop(17))
        page.addView(info)
    }

    private fun bottomNav(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = gradient(0xF5091530.toInt(), 0xF5161744.toInt(), 28, 0xFF253A78.toInt())
        }
        listOf("HOME" to "⌂", "STATS" to "▥", "ABOUT" to "ⓘ").forEach { (tab, icon) ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, dp(4))
                setOnClickListener { currentTab = tab; renderPage() }
            }
            item.tag = tab
            item.addView(label(icon, 22f, if (currentTab == tab) cyan else 0xFF9AA8D1.toInt(), true).apply { gravity = Gravity.CENTER })
            item.addView(label(tab.lowercase().replaceFirstChar { it.uppercase() }, 10f, if (currentTab == tab) cyan else 0xFF9AA8D1.toInt(), true).apply { gravity = Gravity.CENTER; setPadding(0, dp(2), 0, 0) })
            bar.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return bar
    }

    private fun refreshNav() {
        for (i in 0 until navBar.childCount) {
            val item = navBar.getChildAt(i) as? LinearLayout ?: continue
            val selected = item.tag == currentTab
            for (j in 0 until item.childCount) {
                val text = item.getChildAt(j) as? TextView ?: continue
                text.setTextColor(if (selected) cyan else 0xFF9AA8D1.toInt())
            }
        }
    }

    private fun permissionCard(icon: String, title: String, subtitle: String, action: String, click: () -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(12), dp(10), dp(12))
            background = gradient(0xE9112043.toInt(), 0xE91B1646.toInt(), 18, 0xFF1B3D79.toInt())
        }
        val iconBox = TextView(this).apply {
            text = icon; textSize = 25f; gravity = Gravity.CENTER; setTextColor(cyan)
            background = gradient(0xFF092B5A.toInt(), 0xFF17245C.toInt(), 16, 0xFF174B9B.toInt())
        }
        row.addView(iconBox, LinearLayout.LayoutParams(dp(48), dp(48)))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(5), 0) }
        copy.addView(label(title, 12f, white, true))
        copy.addView(label(subtitle, 10f, muted, false).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(copy, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(TextView(this).apply {
            text = action + "  ›"; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (action == "GRANTED") 0xFF24E59A.toInt() else cyan)
            gravity = Gravity.CENTER; background = gradient(0xFF123B78.toInt(), 0xFF31206D.toInt(), 20, 0xFF2857A5.toInt())
            setPadding(dp(10), dp(10), dp(10), dp(10)); setOnClickListener { click() }
        })
        return row
    }

    private fun roundIcon(text: String, accent: Int, click: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 20f; gravity = Gravity.CENTER; setTextColor(white)
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF101B3C.toInt()); setStroke(dp(1), accent) }
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(dp(43), dp(43))
    }

    private fun pill(text: String, color: Int) = TextView(this).apply {
        this.text = text; textSize = 10f; typeface = Typeface.DEFAULT_BOLD; setTextColor(color)
        background = gradient(0xFF0B1A36.toInt(), 0xFF171D45.toInt(), 18, 0xFF263C73.toInt())
        setPadding(dp(10), dp(8), dp(10), dp(8))
    }

    private fun actionButton(textValue: String, color: Int, textColor: Int, isGradient: Boolean, click: () -> Unit) = TextView(this).apply {
        text = textValue; gravity = Gravity.CENTER; textSize = 12f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(textColor); setPadding(dp(12), dp(15), dp(12), dp(15))
        background = if (isGradient) gradient(cyan, violet, 28, 0xFF8C5CFF.toInt(), 1) else gradient(color, color, 13)
        setOnClickListener { click() }
    }

    private fun numericInput(value: String, hintText: String) = EditText(this).apply {
        setText(value); hint = hintText; setSingleLine(); imeOptions = EditorInfo.IME_ACTION_DONE
        inputType = android.text.InputType.TYPE_CLASS_NUMBER; textSize = 15f
        setTextColor(white); setHintTextColor(muted)
    }

    private fun dialogWrap(input: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(4), dp(20), 0)
        addView(input, LinearLayout.LayoutParams(-1, dp(52)))
    }

    private fun dialogField(title: String, input: EditText) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(label(title, 10f, cyan, true), marginBottom(4))
        addView(input, LinearLayout.LayoutParams(-1, dp(48)))
    }

    private fun showAppInfo() { currentTab = "ABOUT"; renderPage() }
    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(packageName, ignoreCase = true)
    }
    private fun label(s: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun space(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun marginBottom(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(v) }
    private fun marginTop(v: Int) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(v) }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

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

    override fun onResume() {
        super.onResume()
        if (::page.isInitialized) renderPage()
    }
}
