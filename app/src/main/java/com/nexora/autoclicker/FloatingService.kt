package com.nexora.autoclicker

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
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
    private var hitMenu: View? = null

    private var running = false
    private var cursor = 0

    private val cyan = 0xFF46E1EE.toInt()
    private val purple = 0xFF9B8CFF.toInt()
    private val pink = 0xFFEA6685.toInt()
    private val navy = 0xF51B2947.toInt()

    private fun dp(v: Int) =
        (v * resources.displayMetrics.density).toInt()

    private fun overlayParams(w: Int, h: Int) =
        WindowManager.LayoutParams(
            w,
            h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    override fun onCreate() {
        super.onCreate()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        targets = TargetStore.load(this)

        showController()
        showTargets()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ) = START_STICKY

    private fun showController() {
        if (panel != null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(11), dp(13), dp(11))

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(navy, 0xF5222049.toInt())
            ).apply {
                cornerRadius = dp(18).toFloat()
            }
        }

        root.addView(TextView(this).apply {
            text = "NEXORA  •  CONTROL"
            textSize = 13f
            setTextColor(cyan)
            setPadding(dp(3), 0, 0, dp(8))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(
            controlButton("START", cyan) { startLoop() },
            weightParams()
        )

        row.addView(
            controlButton("STOP", pink) { stopLoop() },
            weightParams()
        )

        row.addView(
            controlButton("HIDE", 0xFF465779.toInt()) { minimize() },
            weightParams()
        )

        root.addView(row)

        root.addView(TextView(this).apply {
            text = "Drag markers to set positions • Tap 3 for hit settings"
            textSize = 10f
            setTextColor(0xFFB9C7DE.toInt())
            setPadding(dp(3), dp(7), 0, 0)
        })

        panel = root

        wm.addView(
            root,
            overlayParams(-2, -2).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(70)
            }
        )

        makeDraggable(root)
    }

    private fun makeDraggable(view: View) {
        val touchSlop =
            ViewConfiguration.get(this).scaledTouchSlop

        var downX = 0f
        var downY = 0f
        var originX = 0
        var originY = 0
        var moved = false

        view.setOnTouchListener { v, event ->
            val lp = v.layoutParams as WindowManager.LayoutParams

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    originX = lp.x
                    originY = lp.y
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY

                    if (
                        kotlin.math.abs(dx) > touchSlop ||
                        kotlin.math.abs(dy) > touchSlop
                    ) {
                        moved = true
                    }

                    if (moved) {
                        lp.x = originX + dx.roundToInt()
                        lp.y = originY + dy.roundToInt()
                        wm.updateViewLayout(v, lp)
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> true

                else -> true
            }
        }
    }

    private fun controlButton(
        textValue: String,
        color: Int,
        click: () -> Unit
    ) = TextView(this).apply {
        text = textValue
        gravity = Gravity.CENTER
        textSize = 11f
        setTextColor(if (color == cyan) 0xFF0A0F1F.toInt() else -1)
        background = rounded(color, 11)
        setPadding(dp(13), dp(11), dp(13), dp(11))
        setOnClickListener { click() }
    }

    private fun weightParams() =
        LinearLayout.LayoutParams(0, -2, 1f).apply {
            marginEnd = dp(5)
        }

    private fun showTargets() {
        val dm = resources.displayMetrics

        targets.forEach { t ->
            if (markers.containsKey(t.id)) return@forEach

            val marker = TextView(this).apply {
                text = t.id.toString()
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(0xFF101426.toInt())

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (t.id % 2 == 0) purple else cyan)
                    setStroke(dp(2), 0xFFFFFFFF.toInt())
                }

                alpha = if (t.enabled) 1f else .38f

                var downX = 0f
                var downY = 0f
                var originX = 0
                var originY = 0
                var moved = false

                val touchSlop =
                    ViewConfiguration.get(this@FloatingService).scaledTouchSlop

                setOnTouchListener { v, event ->
                    val lp = v.layoutParams as WindowManager.LayoutParams

                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.rawX
                            downY = event.rawY
                            originX = lp.x
                            originY = lp.y
                            moved = false
                            true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.rawX - downX
                            val dy = event.rawY - downY

                            if (
                                kotlin.math.abs(dx) > touchSlop ||
                                kotlin.math.abs(dy) > touchSlop
                            ) {
                                moved = true
                            }

                            if (moved) {
                                lp.x = originX + dx.roundToInt()
                                lp.y = originY + dy.roundToInt()

                                wm.updateViewLayout(v, lp)

                                t.x = (lp.x + dp(24)).toFloat() /
                                    dm.widthPixels

                                t.y = (lp.y + dp(24)).toFloat() /
                                    dm.heightPixels
                            }
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (moved) {
                                TargetStore.save(
                                    this@FloatingService,
                                    targets
                                )
                            } else if (t.id == 3) {
                                showHitMenu(t)
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> true
                        else -> true
                    }
                }
            }

            val lp = overlayParams(dp(48), dp(48)).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (t.x * dm.widthPixels).roundToInt()
                y = (t.y * dm.heightPixels).roundToInt()
            }

            wm.addView(marker, lp)
            markers[t.id] = marker
        }
    }

    private fun showHitMenu(target: TargetConfig) {
        if (hitMenu != null) {
            closeHitMenu()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(navy, 0xF5222049.toInt())
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), cyan)
            }
        }

        root.addView(TextView(this).apply {
            text = "TARGET 3  •  HIT SETTINGS"
            textSize = 13f
            setTextColor(cyan)
            setPadding(0, 0, 0, dp(12))
        })

        root.addView(TextView(this).apply {
            text = "HIT DURATION (MS)"
            textSize = 11f
            setTextColor(0xFFB9C7DE.toInt())
        })

        val input = EditText(this).apply {
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(target.duration.toString())
            textSize = 16f
            setTextColor(-1)
            setSelectAllOnFocus(true)
            background = rounded(0xFF293956.toInt(), 9)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                topMargin = dp(7)
                bottomMargin = dp(12)
            }
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val save = controlButton("SAVE", cyan) {
            val value = input.text.toString().toLongOrNull()

            if (value == null || value < 1L || value > 60000L) {
                Toast.makeText(
                    this@FloatingService,
                    "Enter a value from 1 to 60000 ms",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                target.duration = value

                TargetStore.save(
                    this@FloatingService,
                    targets
                )

                Toast.makeText(
                    this@FloatingService,
                    "Target 3 hit: ${value} ms saved",
                    Toast.LENGTH_SHORT
                ).show()

                closeHitMenu()
            }
        }

        val cancel = controlButton("CANCEL", 0xFF465779.toInt()) {
            closeHitMenu()
        }

        buttons.addView(save, weightParams())
        buttons.addView(cancel, weightParams())
        root.addView(buttons)

        hitMenu = root

        wm.addView(
            root,
            overlayParams(dp(260), -2).apply {
                gravity = Gravity.CENTER
            }
        )

        input.requestFocus()
    }

    private fun closeHitMenu() {
        hitMenu?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        hitMenu = null
    }

    private fun startLoop() {
        if (running) return

        if (ClickAccessibilityService.current == null) {
            Toast.makeText(
                this,
                "NEXORA Accessibility is not enabled",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (targets.none { it.enabled }) {
            Toast.makeText(
                this,
                "Enable at least one target",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        running = true
        cursor = 0
        setMarkersTouchable(false)
        updateBubbleState()
        runNext()
    }

    private fun stopLoop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        setMarkersTouchable(true)
        updateBubbleState()
    }

    private fun setMarkersTouchable(touchable: Boolean) {
        markers.values.forEach { marker ->
            val lp = marker.layoutParams as? WindowManager.LayoutParams
                ?: return@forEach

            lp.flags = if (touchable) {
                lp.flags and
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            } else {
                lp.flags or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }

            wm.updateViewLayout(marker, lp)
        }
    }

    private fun updateBubbleState() {
        val b = bubble as? TextView ?: return

        b.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (running) pink else cyan)
            setStroke(dp(2), 0xFFFFFFFF.toInt())
        }

        b.contentDescription =
            if (running) {
                "NEXORA running. Tap to stop, hold to open controls"
            } else {
                "NEXORA stopped. Tap to start, hold to open controls"
            }
    }

    private fun runNext() {
        if (!running) return

        val active = targets.filter { it.enabled }

        if (active.isEmpty()) {
            stopLoop()
            return
        }

        if (cursor >= active.size) cursor = 0

        val t = active[cursor++]

        handler.postDelayed({
            if (!running) return@postDelayed

            val service = ClickAccessibilityService.current

            if (service == null) {
                stopLoop()
                return@postDelayed
            }

            val x = t.x * resources.displayMetrics.widthPixels
            val y = t.y * resources.displayMetrics.heightPixels

            service.performTap(x, y, t.duration) { success ->
                if (running) {
                    val delay = if (success) t.endDelay else 100L
                    handler.postDelayed({ runNext() }, delay)
                }
            }
        }, t.startDelay)
    }

    private fun minimize() {
        closeHitMenu()

        panel?.let { wm.removeView(it) }
        panel = null

        markers.values.forEach { wm.removeView(it) }
        markers.clear()

        if (bubble != null) return

        val b = TextView(this).apply {
            text = "N"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(0xFF101426.toInt())

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (running) pink else cyan)
                setStroke(dp(2), 0xFFFFFFFF.toInt())
            }
        }

        bubble = b

        wm.addView(
            b,
            overlayParams(dp(48), dp(48)).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dp(12)
                y = dp(150)
            }
        )

        makeBubbleDraggable(b)
    }

    private fun makeBubbleDraggable(view: View) {
        val touchSlop =
            ViewConfiguration.get(this).scaledTouchSlop

        val holdMs = 550L

        view.setOnTouchListener(
            object : View.OnTouchListener {
                var downX = 0f
                var downY = 0f
                var originX = 0
                var originY = 0
                var moved = false
                var downTime = 0L

                override fun onTouch(
                    v: View,
                    e: MotionEvent
                ): Boolean {
                    val lp = v.layoutParams as WindowManager.LayoutParams

                    when (e.action) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = e.rawX
                            downY = e.rawY
                            originX = lp.x
                            originY = lp.y
                            downTime = SystemClock.uptimeMillis()
                            moved = false
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            val dx = e.rawX - downX
                            val dy = e.rawY - downY

                            if (
                                kotlin.math.abs(dx) > touchSlop ||
                                kotlin.math.abs(dy) > touchSlop
                            ) moved = true

                            if (moved) {
                                lp.x = originX - dx.roundToInt()
                                lp.y = originY + dy.roundToInt()
                                wm.updateViewLayout(v, lp)
                            }
                            return true
                        }

                        MotionEvent.ACTION_UP -> {
                            if (!moved) {
                                if (
                                    SystemClock.uptimeMillis() -
                                    downTime >= holdMs
                                ) {
                                    bubble?.let { wm.removeView(it) }
                                    bubble = null
                                    showController()
                                    showTargets()
                                } else if (running) {
                                    stopLoop()
                                } else {
                                    startLoop()
                                }
                            }
                            return true
                        }

                        MotionEvent.ACTION_CANCEL -> return true
                    }
                    return true
                }
            }
        )
    }

    override fun onDestroy() {
        stopLoop()
        closeHitMenu()

        panel?.let { wm.removeView(it) }
        markers.values.forEach { wm.removeView(it) }
        bubble?.let { wm.removeView(it) }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}