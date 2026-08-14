package com.example.customkeyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.*
import org.json.JSONArray

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)

        val swEnableDecoration = findViewById<Switch>(R.id.swEnableDecoration)
        val etDecorationSymbol = findViewById<EditText>(R.id.etDecorationSymbol)
        val spTheme = findViewById<Spinner>(R.id.spTheme)
        val spFontSize = findViewById<Spinner>(R.id.spFontSize)
        val tvSpeedValue = findViewById<TextView>(R.id.tvSpeedValue)
        val sbSpeed = findViewById<SeekBar>(R.id.sbSpeed)
        val etWordCount = findViewById<EditText>(R.id.etWordCount)
        val btnSaveConfig = findViewById<Button>(R.id.btnSaveConfig)

        val etNewCliché = findViewById<EditText>(R.id.etNewCliché)
        val btnAddCliché = findViewById<Button>(R.id.btnAddCliché)
        val btnShowClichés = findViewById<Button>(R.id.btnShowClichés)

        val themes = arrayOf("Default", "Black", "Navy", "Teal", "Brown", "Grey", "Red", "Purple", "Violet", "Sky")
        val fontSizes = arrayOf("صغير", "متوسط", "كبير")

        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)
        spTheme.adapter = themeAdapter

        val fontAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontSizes)
        spFontSize.adapter = fontAdapter

        swEnableDecoration.isChecked = prefs.getBoolean("enable_decoration", false)
        etDecorationSymbol.setText(prefs.getString("decoration_symbol", "~"))
        sbSpeed.progress = prefs.getInt("speed_ms", 50)
        tvSpeedValue.text = "السرعة: ${sbSpeed.progress}ms"
        etWordCount.setText(prefs.getInt("words_count", 6).toString())

        sbSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedValue.text = "السرعة: ${progress}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSaveConfig.setOnClickListener {
            val editor = prefs.edit()
            editor.putBoolean("enable_decoration", swEnableDecoration.isChecked)
            editor.putString("decoration_symbol", etDecorationSymbol.text.toString())
            editor.putInt("theme_index", spTheme.selectedItemPosition)
            editor.putInt("font_size", spFontSize.selectedItemPosition)
            editor.putInt("speed_ms", sbSpeed.progress)
            editor.putInt("words_count", etWordCount.text.toString().toIntOrNull() ?: 6)
            editor.apply()
            Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
        }

        btnAddCliché.setOnClickListener {
            val text = etNewCliché.text.toString().trim()
            if (text.isNotEmpty()) {
                val list = getClichesList()
                list.add(text)
                saveClichesList(list)
                etNewCliché.setText("")
                Toast.makeText(this, "تمت الإضافة!", Toast.LENGTH_SHORT).show()
            }
        }

        btnShowClichés.setOnClickListener {
            showClichésDialog()
        }
    }

    private fun getClichesList(): ArrayList<String> {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("cliches_json", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val list = ArrayList<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun saveClichesList(list: ArrayList<String>) {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val array = JSONArray(list)
        prefs.edit().putString("cliches_json", array.toString()).apply()
    }

    private fun showClichésDialog() {
        val list = getClichesList()
        if (list.isEmpty()) {
            Toast.makeText(this, "لا توجد كلايش مضافة", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("الكلايش المضافة (اضغط للحذف)")
        builder.setItems(list.toTypedArray()) { _, which ->
            list.removeAt(which)
            saveClichesList(list)
            Toast.makeText(this, "تم الحذف بنجاح", Toast.LENGTH_SHORT).show()
        }
        builder.setPositiveButton("إغلاق", null)
        builder.show()
    }
}
