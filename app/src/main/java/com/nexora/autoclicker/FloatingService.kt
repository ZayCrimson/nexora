package com.nexora.autoclicker

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.*
import kotlin.math.roundToInt

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var targets: MutableList<TargetConfig>
    private val markers = mutableMapOf<Int, View>()
    private var panel: View? = null
    private var bubble: View? = null
    private var running = false
    private var cursor = 0
    private val cyan = 0xFF46E1EE.toInt()
    private val purple = 0xFF9B8CFF.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun overlayParams(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        targets = TargetStore.load(this)
        showController()
        showTargets()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    private fun showController() {
        if (panel != null) return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xF51B2947.toInt(), 0xF5222049.toInt())).apply { cornerRadius = dp(18).toFloat() }
        }
        root.addView(TextView(this).apply {
            text = "NEXORA  •  CONTROL"; textSize = 13f
            setTextColor(cyan); setPadding(dp(3), 0, 0, dp(8))
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(controlButton("START", cyan) { startLoop() }, weightParams())
        row.addView(controlButton("STOP", 0xFFEA6685.toInt()) { stopLoop() }, weightParams())
        row.addView(controlButton("HIDE", 0xFF465779.toInt()) { minimize() }, weightParams())
        root.addView(row)
        root.addView(TextView(this).apply {
            text = "Drag numbered markers to set tap positions"
            textSize = 10f; setTextColor(0xFFB9C7DE.toInt())
            setPadding(dp(3), dp(7), 0, 0)
        })
        panel = root
        wm.addView(root, overlayParams(-2, -2).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(70)
        })
        makeDraggable(root)
    }

    private fun controlButton(textValue: String, color: Int, click: () -> Unit) = TextView(this).apply {
        text = textValue; gravity = Gravity.CENTER; textSize = 11f
        setTextColor(if (color == cyan) 0xFF0A0F1F.toInt() else -1)
        background = rounded(color, 11); setPadding(dp(13), dp(11), dp(13), dp(11))
        setOnClickListener { click() }
    }
    private fun weightParams() = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5) }

    private fun showTargets() {
        val dm = resources.displayMetrics
        targets.forEach { t ->
            if (markers.containsKey(t.id)) return@forEach
            val marker = TextView(this).apply {
                text = t.id.toString(); textSize = 16f; gravity = Gravity.CENTER
                setTextColor(0xFF101426.toInt())
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (t.id % 2 == 0) purple else cyan)
                    setStroke(dp(2), 0xFFFFFFFF.toInt())
                }
                alpha = if (t.enabled) 1f else .38f
                setOnTouchListener { v, event ->
                    val lp = v.layoutParams as WindowManager.LayoutParams
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.tag = floatArrayOf(event.rawX - lp.x, event.rawY - lp.y); true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val delta = v.tag as FloatArray
                            lp.x = (event.rawX - delta[0]).roundToInt()
                            lp.y = (event.rawY - delta[1]).roundToInt()
                            wm.updateViewLayout(v, lp)
                            t.x = (lp.x + dp(24)).toFloat() / dm.widthPixels
                            t.y = (lp.y + dp(24)).toFloat() / dm.heightPixels
                            true
                        }
                        MotionEvent.ACTION_UP -> { TargetStore.save(this, targets); true }
                        else -> true
                    }
                }
            }
            val lp = overlayParams(dp(48), dp(48)).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (t.x * dm.widthPixels).roundToInt()
                y = (t.y * dm.heightPixels).roundToInt()
            }
            wm.addView(marker, lp); markers[t.id] = marker
        }
    }

    private fun startLoop() {
        if (running) return
        if (ClickAccessibilityService.current == null) {
            Toast.makeText(this, "NEXORA Accessibility is not enabled", Toast.LENGTH_LONG).show()
            return
        }
        if (targets.none { it.enabled }) {
            Toast.makeText(this, "Enable at least one target", Toast.LENGTH_SHORT).show()
            return
        }
        running = true; cursor = 0; runNext()
    }

    private fun stopLoop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun runNext() {
        if (!running) return
        val active = targets.filter { it.enabled }
        if (active.isEmpty()) { stopLoop(); return }
        if (cursor >= active.size) cursor = 0
        val t = active[cursor++]
        handler.postDelayed({
            if (!running) return@postDelayed
            val service = ClickAccessibilityService.current
            if (service == null) { stopLoop(); return@postDelayed }
            val x = t.x * resources.displayMetrics.widthPixels
            val y = t.y * resources.displayMetrics.heightPixels
            service.performTap(x, y, t.duration) {
                if (running) handler.postDelayed({ runNext() }, t.endDelay)
            }
        }, t.startDelay)
    }

    private fun minimize() {
        panel?.let { wm.removeView(it) }; panel = null
        markers.values.forEach { wm.removeView(it) }; markers.clear()
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "N"; textSize = 18f; gravity = Gravity.CENTER
            setTextColor(0xFF101426.toInt())
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cyan) }
            setOnClickListener {
                bubble?.let { wm.removeView(it) }; bubble = null
                showController(); showTargets()
            }
        }
        bubble = b
        wm.addView(b, overlayParams(dp(52), dp(52)).apply {
            gravity = Gravity.TOP or Gravity.END; x = dp(15); y = dp(150)
        })
        makeDraggable(b)
    }

    private fun makeDraggable(view: View) {
        view.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var originX = 0; var originY = 0
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                val lp = v.layoutParams as WindowManager.LayoutParams
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX=e.rawX; downY=e.rawY; originX=lp.x; originY=lp.y; return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x=originX+(e.rawX-downX).roundToInt()
                        lp.y=originY+(e.rawY-downY).roundToInt()
                        wm.updateViewLayout(v, lp); return true
                    }
                    MotionEvent.ACTION_UP -> return false
                }
                return true
            }
        })
    }

    override fun onDestroy() {
        stopLoop()
        panel?.let { wm.removeView(it) }
        markers.values.forEach { wm.removeView(it) }
        bubble?.let { wm.removeView(it) }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
