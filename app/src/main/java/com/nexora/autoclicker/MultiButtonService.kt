package com.nexora.autoclicker

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.roundToInt

/** Independent floating controller. It has its own preferences and never touches the four-target mode. */
class MultiButtonService : Service() {
    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val buttons = mutableListOf<TargetConfig>()
    private val views = linkedMapOf<Int, View>()
    private var panel: View? = null
    private var bubble: View? = null
    private var editor: View? = null
    private var running = false
    private var cursor = 0
    private val cyan = 0xFF20DDF5.toInt()
    private val red = 0xFFFF4568.toInt()
    private val violet = 0xFF873CFF.toInt()
    private val navy = 0xF5111D3B.toInt()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun bg(color: Int, radius: Int = 16, stroke: Int = 0xFF31569A.toInt()) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        buttons.addAll(MultiButtonStore.load(this))
        showPanel()
        showButtons()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    private fun showPanel() {
        if (panel != null) return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xF5103157.toInt(), 0xF522164B.toInt())).apply {
                cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0xFF2866B8.toInt())
            }
        }
        root.addView(TextView(this).apply { text = "NEXORA  •  MULTI BUTTON"; textSize = 12f; setTextColor(cyan); setPadding(dp(2), 0, 0, dp(8)) })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(action("START", cyan) { startLoop() }, weight())
        row.addView(action("STOP", red) { stopLoop() }, weight())
        row.addView(action("＋ ADD", violet) { addButton() }, weight())
        row.addView(action("HIDE", 0xFF465779.toInt()) { minimize() }, weight())
        row.addView(action("✕", red) { stopSelf() }, LinearLayout.LayoutParams(dp(34), -2))
        root.addView(row)
        root.addView(TextView(this).apply { text = "Tap a marker to edit timing • Drag to move"; textSize = 9f; setTextColor(0xFFB9C7DE.toInt()); setPadding(dp(2), dp(7), 0, 0) })
        panel = root
        wm.addView(root, params(-2, -2).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(70) })
    }

    private fun weight() = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(4) }
    private fun action(textValue: String, color: Int, click: () -> Unit) = TextView(this).apply {
        text = textValue; gravity = Gravity.CENTER; textSize = 10f; setTextColor(if (color == cyan) 0xFF07142A.toInt() else Color.WHITE)
        background = bg(color, 11, color); setPadding(dp(8), dp(11), dp(8), dp(11)); setOnClickListener { click() }
    }

    private fun addButton() {
        if (buttons.size >= 40) { toast("Maximum 40 buttons"); return }
        val id = (buttons.maxOfOrNull { it.id } ?: 0) + 1
        val item = TargetConfig(id, .42f + (buttons.size % 4) * .08f, .48f + (buttons.size / 4) * .07f, 100L, 25L, 200L, true)
        buttons.add(item); MultiButtonStore.save(this, buttons); addButtonView(item)
    }

    private fun showButtons() { buttons.forEach { addButtonView(it) } }
    private fun addButtonView(t: TargetConfig) {
        if (views.containsKey(t.id)) return
        val dm = resources.displayMetrics
        val marker = TextView(this).apply {
            text = t.id.toString(); gravity = Gravity.CENTER; textSize = 15f; setTextColor(Color.WHITE)
            background = bg(if (t.enabled) 0xEE182B58.toInt() else 0xAA46516B.toInt(), 14, if (running) cyan else violet)
            alpha = if (t.enabled) 1f else .45f
        }
        var downX = 0f; var downY = 0f; var originX = 0; var originY = 0; var moved = false
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        marker.setOnTouchListener { v, e ->
            val lp = v.layoutParams as WindowManager.LayoutParams
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX=e.rawX; downY=e.rawY; originX=lp.x; originY=lp.y; moved=false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx=e.rawX-downX; val dy=e.rawY-downY
                    if (abs(dx)>slop || abs(dy)>slop) moved=true
                    if (moved) { lp.x=originX+dx.roundToInt(); lp.y=originY+dy.roundToInt(); wm.updateViewLayout(v,lp); t.x=(lp.x+dp(25)).toFloat()/dm.widthPixels; t.y=(lp.y+dp(25)).toFloat()/dm.heightPixels }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        MultiButtonStore.save(this, buttons)
                    } else if (!running) {
                        editButton(t)
                    }
                    true
                }
                else -> true
            }
        }
        val lp = params(dp(50), dp(50)).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (t.x * dm.widthPixels).roundToInt()
            y = (t.y * dm.heightPixels).roundToInt()
            if (running) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        wm.addView(marker,lp); views[t.id]=marker
    }

    private fun editButton(t: TargetConfig) {
        // A marker is a tap target while the loop is running, never a settings shortcut.
        if (running) return
        closeEditor()
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(14),dp(16),dp(14)); background=bg(navy,18,cyan) }
        root.addView(TextView(this).apply { text="BUTTON ${t.id} SETTINGS"; textSize=13f; setTextColor(cyan); setPadding(0,0,0,dp(10)) })
        fun field(title:String,value:Long): EditText {
            root.addView(TextView(this).apply { text=title; textSize=10f; setTextColor(0xFFB9C7DE.toInt()) })
            return EditText(this).apply { setSingleLine(true); inputType=android.text.InputType.TYPE_CLASS_NUMBER; setText(value.toString()); textSize=14f; setTextColor(Color.WHITE); background=bg(0xFF293956.toInt(),9); setPadding(dp(10),dp(7),dp(10),dp(7)); root.addView(this,LinearLayout.LayoutParams(-1,dp(45)).apply { bottomMargin=dp(7) }) }
        }
        val start=field("START DELAY (MS)",t.startDelay); val hit=field("HIT DURATION (MS)",t.duration); val end=field("END DELAY (MS)",t.endDelay)
        val enabled = android.widget.Switch(this).apply { text="Enabled"; setTextColor(Color.WHITE); isChecked=t.enabled }
        root.addView(enabled)
        val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        row.addView(action("SAVE",cyan) {
            val a=start.text.toString().toLongOrNull(); val b=hit.text.toString().toLongOrNull(); val c=end.text.toString().toLongOrNull()
            if(a==null||b==null||c==null||a<0||b !in 1L..60000L||c<0) toast("Check timing values (hit 1-60000 ms)")
            else { t.startDelay=a; t.duration=b; t.endDelay=c; t.enabled=enabled.isChecked; MultiButtonStore.save(this,buttons); views[t.id]?.alpha=if(t.enabled)1f else .4f; closeEditor() }
        },weight())
        row.addView(action("DELETE",red) { buttons.remove(t); views.remove(t.id)?.let { try { wm.removeView(it) } catch (_:Exception){} }; MultiButtonStore.save(this,buttons); closeEditor() },weight())
        row.addView(action("CANCEL",0xFF465779.toInt()) { closeEditor() },weight())
        root.addView(row); editor=root
        wm.addView(root,params(dp(285),-2).apply { gravity=Gravity.CENTER; flags=flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() })
    }
    private fun closeEditor() { editor?.let { try { wm.removeView(it) } catch (_:Exception){} }; editor=null }

    private fun startLoop() {
        if (running) return
        if (ClickAccessibilityService.current==null) { toast("Enable NEXORA Accessibility first"); return }
        if (buttons.none { it.enabled }) { toast("Add and enable at least one button"); return }
        running=true; cursor=0; updateMarkers(); updateBubbleState(); runNext()
    }
    private fun stopLoop() { running=false; handler.removeCallbacksAndMessages(null); updateMarkers(); updateBubbleState() }
    private fun updateMarkers() {
        buttons.forEach { t ->
            val marker = views[t.id] ?: return@forEach
            marker.background = bg(
                if (t.enabled) 0xEE182B58.toInt() else 0xAA46516B.toInt(),
                14,
                if (running) cyan else violet
            )
            // Critical: while clicking, let Accessibility gestures pass through the
            // floating marker to the game instead of activating the marker's editor.
            val lp = marker.layoutParams as? WindowManager.LayoutParams ?: return@forEach
            val desiredFlags = if (running) {
                lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            if (lp.flags != desiredFlags) {
                lp.flags = desiredFlags
                try { wm.updateViewLayout(marker, lp) } catch (_: Exception) { }
            }
        }
    }
    private fun runNext() {
        if(!running) return
        val active=buttons.filter { it.enabled }
        if(active.isEmpty()) { stopLoop(); return }
        if(cursor>=active.size) cursor=0
        val t=active[cursor++]
        handler.postDelayed({
            if(!running) return@postDelayed
            val svc=ClickAccessibilityService.current
            if(svc==null) { stopLoop(); return@postDelayed }
            svc.performTap(t.x*resources.displayMetrics.widthPixels,t.y*resources.displayMetrics.heightPixels,t.duration) { ok ->
                if(running) handler.postDelayed({runNext()},if(ok)t.endDelay else 100L)
            }
        },t.startDelay)
    }

    private fun updateBubbleState() {
        val b = bubble as? TextView ?: return
        b.background = bg(if (running) 0xFF168CFF.toInt() else 0xFFE5485D.toInt(), 14, Color.WHITE)
        b.contentDescription = if (running) "NEXORA running" else "NEXORA stopped"
    }

    private fun minimize() {
        closeEditor(); panel?.let { try { wm.removeView(it) } catch (_:Exception){} }; panel=null
        views.values.forEach { try { wm.removeView(it) } catch (_:Exception){} }; views.clear()
        val b=TextView(this).apply {
            text="N"; textSize=19f; typeface=android.graphics.Typeface.DEFAULT_BOLD
            gravity=Gravity.CENTER; setTextColor(Color.WHITE)
            background=bg(if(running)0xFF168CFF.toInt() else 0xFFE5485D.toInt(),14,Color.WHITE)
            contentDescription = if (running) "NEXORA running" else "NEXORA stopped"
        }
        bubble=b; wm.addView(b,params(dp(50),dp(50)).apply { gravity=Gravity.TOP or Gravity.END; x=dp(12); y=dp(150) })
        var downX=0f; var downY=0f; var ox=0; var oy=0; var moved=false; var downAt=0L
        b.setOnTouchListener { v,e -> val lp=v.layoutParams as WindowManager.LayoutParams; when(e.action){
            MotionEvent.ACTION_DOWN->{downX=e.rawX;downY=e.rawY;ox=lp.x;oy=lp.y;moved=false;downAt=android.os.SystemClock.uptimeMillis();true}
            MotionEvent.ACTION_MOVE->{val dx=e.rawX-downX;val dy=e.rawY-downY;if(abs(dx)>dp(5)||abs(dy)>dp(5))moved=true;if(moved){lp.x=ox-dx.roundToInt();lp.y=oy+dy.roundToInt();wm.updateViewLayout(v,lp)};true}
            MotionEvent.ACTION_UP->{if(!moved){if(android.os.SystemClock.uptimeMillis()-downAt>=550L){try{wm.removeView(v)}catch(_:Exception){};bubble=null;showPanel();showButtons()}else if(running)stopLoop() else startLoop();updateBubbleState()};true}
            else->true
        } }
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    override fun onDestroy(){stopLoop();closeEditor();(panel)?.let{try{wm.removeView(it)}catch(_:Exception){}};views.values.forEach{try{wm.removeView(it)}catch(_:Exception){}};bubble?.let{try{wm.removeView(it)}catch(_:Exception){}};panel=null;views.clear();bubble=null;super.onDestroy()}
}
