package com.xiaomo.androidforclaw.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xiaomo.androidforclaw.util.AppConstants
import com.draco.ladb.R
import com.draco.ladb.databinding.ActivityConfigBinding
import com.tencent.mmkv.MMKV

/**
 * 配置页面
 * 映射 OpenClaw CLI: openclaw config
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private val mmkv by lazy { MMKV.defaultMMKV() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "配置"
        }

        loadConfig()
        setupListeners()
    }

    private fun loadConfig() {
        binding.apply {
            // API 配置
            etApiKey.setText("")
            etApiBase.setText("")

            // 功能开关
            switchReasoning.isChecked = mmkv?.getBoolean("reasoning_enabled",
                AppConstants.REASONING_ENABLED_DEFAULT) ?: true
            switchExploration.isChecked = mmkv?.getBoolean("exploration_mode", false) ?: false

            // Gateway 配置
            etGatewayPort.setText("8765")
        }
    }

    private fun setupListeners() {
        binding.apply {
            // 保存按钮
            btnSave.setOnClickListener {
                saveConfig()
            }

            // 恢复默认按钮
            btnReset.setOnClickListener {
                resetToDefault()
            }

            // Skills 管理入口
            cardSkills.setOnClickListener {
                startActivity(Intent(this@ConfigActivity, SkillsActivity::class.java))
            }
        }
    }

    private fun saveConfig() {
        mmkv?.apply {
            encode("reasoning_enabled", binding.switchReasoning.isChecked)
            encode("exploration_mode", binding.switchExploration.isChecked)
        }

        Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun resetToDefault() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("恢复默认")
            .setMessage("确定要恢复所有配置为默认值吗？")
            .setPositiveButton("确定") { _, _ ->
                mmkv?.apply {
                    encode("reasoning_enabled", true)
                    encode("exploration_mode", false)
                }
                loadConfig()
                Toast.makeText(this, "已恢复默认配置", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
