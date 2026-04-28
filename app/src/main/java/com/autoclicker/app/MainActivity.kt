package com.autoclicker.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("clicker_prefs", Context.MODE_PRIVATE) }

    // 当前参数
    private var cps     = 10
    private var clickX  = 540f
    private var clickY  = 960f
    private var areaW   = 10f
    private var areaH   = 10f
    private var radius  = 5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPrefs()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    // ------------------------------------------------------------------ //
    //  UI 初始化
    // ------------------------------------------------------------------ //
    private fun setupUI() {
        // --- CPS 滑块 ---
        binding.seekBarCps.max = 99          // 0→99 对应 1→100
        binding.seekBarCps.progress = cps - 1
        updateCpsLabel()
        binding.seekBarCps.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                cps = p + 1
                updateCpsLabel()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?)  {}
        })

        // --- 点击区域坐标 ---
        binding.etClickX.setText(clickX.toInt().toString())
        binding.etClickY.setText(clickY.toInt().toString())
        binding.etAreaW.setText(areaW.toInt().toString())
        binding.etAreaH.setText(areaH.toInt().toString())

        // --- 触点半径滑块 ---
        binding.seekBarRadius.max = 49       // 0→49 对应 1→50
        binding.seekBarRadius.progress = radius.toInt() - 1
        updateRadiusLabel()
        binding.seekBarRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                radius = (p + 1).toFloat()
                updateRadiusLabel()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?)  {}
        })

        // --- 保存并启动悬浮窗 ---
        binding.btnSave.setOnClickListener {
            if (!checkPermissions()) return@setOnClickListener
            saveParams()
            Toast.makeText(this, "参数已保存", Toast.LENGTH_SHORT).show()
        }

        // --- 启用无障碍服务 ---
        binding.btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        // --- 申请悬浮窗权限 ---
        binding.btnOverlay.setOnClickListener {
            requestOverlayPermission()
        }

        // --- 启动/停止悬浮窗 ---
        binding.btnStartFloat.setOnClickListener {
            if (!checkPermissions()) return@setOnClickListener
            saveParams()
            startFloatingWindow()
        }

        binding.btnStopFloat.setOnClickListener {
            stopFloatingWindow()
        }

        // --- 帮助 ---
        binding.tvHelp.setOnClickListener {
            showHelpDialog()
        }
    }

    // ------------------------------------------------------------------ //
    //  参数读写
    // ------------------------------------------------------------------ //
    private fun loadPrefs() {
        cps    = prefs.getInt("cps",     10)
        clickX = prefs.getFloat("x",     540f)
        clickY = prefs.getFloat("y",     960f)
        areaW  = prefs.getFloat("width", 10f)
        areaH  = prefs.getFloat("height",10f)
        radius = prefs.getFloat("radius",5f)
    }

    private fun saveParams() {
        clickX = binding.etClickX.text.toString().toFloatOrNull() ?: clickX
        clickY = binding.etClickY.text.toString().toFloatOrNull() ?: clickY
        areaW  = binding.etAreaW.text.toString().toFloatOrNull() ?: areaW
        areaH  = binding.etAreaH.text.toString().toFloatOrNull() ?: areaH

        prefs.edit()
            .putInt("cps",     cps)
            .putFloat("x",     clickX)
            .putFloat("y",     clickY)
            .putFloat("width", areaW)
            .putFloat("height",areaH)
            .putFloat("radius",radius)
            .apply()
    }

    private fun updateCpsLabel() {
        binding.tvCpsValue.text = "${cps} 次/秒"
    }

    private fun updateRadiusLabel() {
        binding.tvRadiusValue.text = "${radius.toInt()} px"
    }

    // ------------------------------------------------------------------ //
    //  权限检查
    // ------------------------------------------------------------------ //
    private fun refreshPermissionStatus() {
        val overlayOk = Settings.canDrawOverlays(this)
        val accessOk  = ClickAccessibilityService.instance != null

        binding.tvStatusOverlay.text      = if (overlayOk)  "✓ 已授权" else "✗ 未授权"
        binding.tvStatusAccessibility.text = if (accessOk)  "✓ 已启用" else "✗ 未启用"
        binding.tvStatusOverlay.setTextColor(
            if (overlayOk) 0xFF27AE60.toInt() else 0xFFE74C3C.toInt())
        binding.tvStatusAccessibility.setTextColor(
            if (accessOk) 0xFF27AE60.toInt() else 0xFFE74C3C.toInt())

        binding.btnStartFloat.isEnabled = overlayOk && accessOk
    }

    private fun checkPermissions(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            return false
        }
        if (ClickAccessibilityService.instance == null) {
            Toast.makeText(this, "请先启用无障碍服务", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    // ------------------------------------------------------------------ //
    //  悬浮窗开关
    // ------------------------------------------------------------------ //
    private fun startFloatingWindow() {
        startService(Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, "悬浮窗已启动，可切换到目标应用", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingWindow() {
        ClickAccessibilityService.instance?.stopClicking()
        stopService(Intent(this, FloatingWindowService::class.java))
    }

    // ------------------------------------------------------------------ //
    //  帮助对话框
    // ------------------------------------------------------------------ //
    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("使用说明")
            .setMessage(
                "① 授权「悬浮窗」权限\n" +
                "② 在「无障碍」设置中启用「连点器」服务\n" +
                "③ 在「点击坐标」中填入目标屏幕坐标\n" +
                "   （可用开发者选项中的「指针位置」查看坐标）\n" +
                "④ 设置「点击区域」大小（> 0 则在范围内随机点击）\n" +
                "⑤ 调整「每秒次数」（1–100）\n" +
                "⑥ 点击「启动悬浮窗」，切换到目标 App\n" +
                "⑦ 点击悬浮按钮 ▶ 开始 / ■ 停止连点"
            )
            .setPositiveButton("明白了", null)
            .show()
    }
}
