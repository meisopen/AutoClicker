package com.autoclicker.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * 悬浮窗服务 — 提供随时可见的开关按钮
 */
class FloatingWindowService : Service() {

    companion object {
        const val ACTION_STATE_CHANGED = "com.autoclicker.STATE_CHANGED"
        const val EXTRA_RUNNING        = "running"
        private const val NOTIF_CHANNEL = "auto_clicker_channel"
        private const val NOTIF_ID      = 1001
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View
    private var floatParams: WindowManager.LayoutParams? = null

    private var lastX = 0
    private var lastY = 0
    private var initX = 0
    private var initY = 0

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val running = intent.getBooleanExtra(EXTRA_RUNNING, false)
            updateButtonState(running)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        setupFloatingWindow()

        val filter = IntentFilter(ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onDestroy() {
        try {
            windowManager.removeView(floatView)
            unregisterReceiver(stateReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    // ------------------------------------------------------------------ //
    //  悬浮窗搭建
    // ------------------------------------------------------------------ //
    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatView = LayoutInflater.from(this).inflate(R.layout.layout_float_button, null)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        floatParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 200
        }

        windowManager.addView(floatView, floatParams)

        // 拖动逻辑
        floatView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    initX = floatParams!!.x
                    initY = floatParams!!.y
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    floatParams!!.x = initX + (event.rawX.toInt() - lastX)
                    floatParams!!.y = initY + (event.rawY.toInt() - lastY)
                    windowManager.updateViewLayout(floatView, floatParams)
                    true
                }
                else -> false
            }
        }

        // 点击开关按钮
        floatView.findViewById<ImageButton>(R.id.btnFloatToggle).setOnClickListener {
            val svc = ClickAccessibilityService.instance
            if (svc != null) {
                if (svc.isClickRunning()) {
                    svc.stopClicking()
                } else {
                    // 读取 MainActivity 最后保存的参数
                    val prefs = getSharedPreferences("clicker_prefs", Context.MODE_PRIVATE)
                    val intent = Intent(ClickAccessibilityService.ACTION_START_CLICK).apply {
                        setPackage(packageName)
                        putExtra(ClickAccessibilityService.EXTRA_CPS,
                            prefs.getInt("cps", 10))
                        putExtra(ClickAccessibilityService.EXTRA_X,
                            prefs.getFloat("x", 540f))
                        putExtra(ClickAccessibilityService.EXTRA_Y,
                            prefs.getFloat("y", 960f))
                        putExtra(ClickAccessibilityService.EXTRA_WIDTH,
                            prefs.getFloat("width", 10f))
                        putExtra(ClickAccessibilityService.EXTRA_HEIGHT,
                            prefs.getFloat("height", 10f))
                        putExtra(ClickAccessibilityService.EXTRA_RADIUS,
                            prefs.getFloat("radius", 5f))
                    }
                    sendBroadcast(intent)
                }
            }
        }
    }

    private fun updateButtonState(running: Boolean) {
        val btn = floatView.findViewById<ImageButton>(R.id.btnFloatToggle)
        val label = floatView.findViewById<TextView>(R.id.tvFloatState)
        if (running) {
            btn.setImageResource(R.drawable.ic_stop)
            label.text = "点击中"
            label.setTextColor(Color.RED)
        } else {
            btn.setImageResource(R.drawable.ic_play)
            label.text = "已停止"
            label.setTextColor(Color.WHITE)
        }
    }

    // ------------------------------------------------------------------ //
    //  前台通知
    // ------------------------------------------------------------------ //
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL, "连点器服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "保持连点器悬浮窗常驻" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL)
            .setContentTitle("连点器运行中")
            .setContentText("点击返回设置界面")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
