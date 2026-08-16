package com.example.customkeyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : Activity() {

    private val PICK_TXT_FILE = 101
    private lateinit var etLongText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)

        btnImportTxt = findViewById(R.id.btnImportTxt)
        etLongText = findViewById(R.id.etLongText)
        val etWordsPerLine = findViewById<EditText>(R.id.etWordsPerLine)
        val tvSpeedLabel = findViewById<TextView>(R.id.tvSpeedLabel)
        val sbSpeed = findViewById<SeekBar>(R.id.sbSpeed)
        val btnSaveAll = findViewById<Button>(R.id.btnSaveAll)

        etLongText.setText(prefs.getString("long_text_source", ""))
        etWordsPerLine.setText(prefs.getInt("words_per_line", 6).toString())
        sbSpeed.progress = prefs.getInt("speed_ms", 50)
        tvSpeedLabel.text = "سرعة الكتابة: ${sbSpeed.progress}ms"

        sbSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSpeedLabel.text = "سرعة الكتابة: ${progress}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnImportTxt).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/plain"
            }
            startActivityForResult(intent, PICK_TXT_FILE)
        }

        btnSaveAll.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("long_text_source", etLongText.text.toString())
            editor.putInt("words_per_line", etWordsPerLine.text.toString().toIntOrNull() ?: 6)
            editor.putInt("speed_ms", sbSpeed.progress)
            editor.putInt("current_word_pointer", 0) // البدء دائماً من بداية النص
            editor.apply()
            Toast.makeText(this, "تم حفظ النص والإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var btnImportTxt: Button

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
        if (requestCode == PICK_TXT_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val content = readTextFromUri(uri)
                etLongText.setText(content)
                Toast.makeText(this, "تم تحميل الملف بنجاح!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line).append(" ")
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}
