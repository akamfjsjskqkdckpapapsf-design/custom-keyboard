package com.example.customkeyboard

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)

        val etTriggerSymbol = findViewById<EditText>(R.id.etTriggerSymbol)
        val etAutoText = findViewById<EditText>(R.id.etAutoText)
        val etSpeed = findViewById<EditText>(R.id.etSpeed)
        val cbEnableAuto = findViewById<CheckBox>(R.id.cbEnableAuto)
        val cbReplaceSpaceInAuto = findViewById<CheckBox>(R.id.cbReplaceSpaceInAuto)
        val etSuffixText = findViewById<EditText>(R.id.etSuffixText)
        val cbReplaceSpaceGlobal = findViewById<CheckBox>(R.id.cbReplaceSpaceGlobal)
        val btnSave = findViewById<Button>(R.id.btnSave)

        etTriggerSymbol.setText(prefs.getString("trigger_symbol", ""))
        etAutoText.setText(prefs.getString("auto_text", ""))
        etSpeed.setText(prefs.getInt("auto_speed", 50).toString())
        cbEnableAuto.isChecked = prefs.getBoolean("enable_auto", false)
        cbReplaceSpaceInAuto.isChecked = prefs.getBoolean("auto_replace_space", false)
        etSuffixText.setText(prefs.getString("suffix_text", ""))
        cbReplaceSpaceGlobal.isChecked = prefs.getBoolean("global_replace_space", false)

        btnSave.setOnClickListener {
            val speed = etSpeed.text.toString().toIntOrNull() ?: 50

            val editor = prefs.edit()
            editor.putString("trigger_symbol", etTriggerSymbol.text.toString().trim())
            editor.putString("auto_text", etAutoText.text.toString())
            editor.putInt("auto_speed", speed)
            editor.putBoolean("enable_auto", cbEnableAuto.isChecked)
            editor.putBoolean("auto_replace_space", cbReplaceSpaceInAuto.isChecked)
            editor.putString("suffix_text", etSuffixText.text.toString())
            editor.putBoolean("global_replace_space", cbReplaceSpaceGlobal.isChecked)
            editor.apply()

            Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }
}
