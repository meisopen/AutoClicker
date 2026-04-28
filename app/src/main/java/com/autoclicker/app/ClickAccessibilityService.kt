package com.autoclicker.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * 连点器核心服务
 * 使用 AccessibilityService + dispatchGesture 实现模拟点击
 * 无需 ROOT，Android 7.0+ 支持
 */
class ClickAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_START_CLICK  = "com.autoclicker.START"
        const val ACTION_STOP_CLICK   = "com.autoclicker.STOP"
        const val ACTION_UPDATE_PARAM = "com.autoclicker.UPDATE_PARAM"

        // 点击参数 Extra 键
        const val EXTRA_CPS     = "cps"         // 每秒点击次数 (1-100)
        const val EXTRA_X       = "x"           // 点击区域左上角 X
        const val EXTRA_Y       = "y"           // 点击区域左上角 Y
        const val EXTRA_WIDTH   = "width"       // 点击区域宽度
        const val EXTRA_HEIGHT  = "height"      // 点击区域高度
        const val EXTRA_RADIUS  = "radius"      // 点击触点半径 (1-50 dp)

        var instance: ClickAccessibilityService? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    // 点击参数（默认值）
    private var cps: Int    = 10        // 每秒点击次数
    private var clickX: Float = 540f   // 区域中心 X
    private var clickY: Float = 960f   // 区域中心 Y
    private var areaWidth: Float  = 10f
    private var areaHeight: Float = 10f
    private var radius: Float = 5f     // 触点半径 px

    private val clickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_START_CLICK -> {
                    updateParams(intent)
                    startClicking()
                }
                ACTION_STOP_CLICK -> stopClicking()
                ACTION_UPDATE_PARAM -> updateParams(intent)
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
        val filter = IntentFilter().apply {
            addAction(ACTION_START_CLICK)
            addAction(ACTION_STOP_CLICK)
            addAction(ACTION_UPDATE_PARAM)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(clickReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(clickReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopClicking() }

    override fun onDestroy() {
        stopClicking()
        try { unregisterReceiver(clickReceiver) } catch (_: Exception) {}
        instance = null
        super.onDestroy()
    }

    // ------------------------------------------------------------------ //
    //  参数更新
    // ------------------------------------------------------------------ //
    private fun updateParams(intent: Intent) {
        cps         = intent.getIntExtra(EXTRA_CPS, cps).coerceIn(1, 100)
        clickX      = intent.getFloatExtra(EXTRA_X, clickX)
        clickY      = intent.getFloatExtra(EXTRA_Y, clickY)
        areaWidth   = intent.getFloatExtra(EXTRA_WIDTH,  areaWidth)
        areaHeight  = intent.getFloatExtra(EXTRA_HEIGHT, areaHeight)
        radius      = intent.getFloatExtra(EXTRA_RADIUS, radius).coerceIn(1f, 50f)
    }

    // ------------------------------------------------------------------ //
    //  连点循环
    // ------------------------------------------------------------------ //
    private val clickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            performClick()
            val intervalMs = (1000.0 / cps).toLong().coerceAtLeast(10L)
            handler.postDelayed(this, intervalMs)
        }
    }

    private fun startClicking() {
        if (isRunning) return
        isRunning = true
        handler.post(clickRunnable)
        // 通知 UI 状态变化
        sendBroadcast(Intent(FloatingWindowService.ACTION_STATE_CHANGED).apply {
            putExtra(FloatingWindowService.EXTRA_RUNNING, true)
        })
    }

    fun stopClicking() {
        if (!isRunning) return
        isRunning = false
        handler.removeCallbacks(clickRunnable)
        sendBroadcast(Intent(FloatingWindowService.ACTION_STATE_CHANGED).apply {
            putExtra(FloatingWindowService.EXTRA_RUNNING, false)
        })
    }

    fun isClickRunning() = isRunning

    // ------------------------------------------------------------------ //
    //  执行单次点击手势
    // ------------------------------------------------------------------ //
    private fun performClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        // 在设定区域内随机取一点，模拟真实点击
        val tx = clickX + (-areaWidth / 2 + Math.random() * areaWidth).toFloat()
        val ty = clickY + (-areaHeight / 2 + Math.random() * areaHeight).toFloat()

        val path = Path().apply { moveTo(tx, ty) }

        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,          // startTime
            10L          // duration (ms) — 短按
        )
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
