package com.example.customkeyboard

import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import org.json.JSONArray

class MyInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var arabicKeyboard: Keyboard? = null
    private var isAutoRunning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        arabicKeyboard = Keyboard(this, R.xml.kbd_arabic)
        keyboardView?.keyboard = arabicKeyboard
        keyboardView?.setOnKeyboardActionListener(this)

        applyThemeAndSize()
        return keyboardView!!
    }

    private fun applyThemeAndSize() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val themeIdx = prefs.getInt("theme_index", 0)

        val colors = arrayOf("#121212", "#000000", "#0A192F", "#004D40", "#3E2723", "#212121", "#B71C1C", "#4A148C", "#311B92", "#0277BD")
        val bgColor = Color.parseColor(colors.getOrElse(themeIdx) { "#121212" })
        keyboardView?.setBackgroundColor(bgColor)
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val isDecoration = prefs.getBoolean("enable_decoration", false)
        val symbol = prefs.getString("decoration_symbol", "~") ?: "~"

        when (primaryCode) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            -100 -> showControlPanel()
            32 -> {
                if (isDecoration) {
                    ic.commitText(symbol, 1)
                } else {
                    ic.commitText(" ", 1)
                }
            }
            else -> ic.commitText(primaryCode.toChar().toString(), 1)
        }
    }

    private fun showControlPanel() {
        val view = layoutInflater.inflate(R.layout.layout_control_panel, null)

        val btnStart = view.findViewById<Button>(R.id.btnStartAutoText)
        val btnStop = view.findViewById<Button>(R.id.btnStopAutoText)
        val btnBack = view.findViewById<Button>(R.id.btnBackToKeyboard)

        btnStart.setOnClickListener {
            startInteractiveTyping()
        }

        btnStop.setOnClickListener {
            isAutoRunning = false
        }

        btnBack.setOnClickListener {
            keyboardView?.let { setInputView(it) }
        }

        setInputView(view)
    }

    private fun startInteractiveTyping() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val speed = prefs.getInt("speed_ms", 50).toLong()
        val wordsCount = prefs.getInt("words_count", 6)
        val jsonStr = prefs.getString("cliches_json", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        if (array.length() == 0) return

        val allWords = ArrayList<String>()
        for (i in 0 until array.length()) {
            val line = array.getString(i)
            allWords.addAll(line.split("\\s+".toRegex()))
        }

        if (allWords.isEmpty()) return

        isAutoRunning = true
        
        // استخدام خيط منفصل مع إرسال الأوامر مباشرة لـ InputConnection
        Thread {
            var index = 0
            while (isAutoRunning) {
                val chunk = ArrayList<String>()
                for (j in 0 until wordsCount) {
                    if (index < allWords.size) {
                        chunk.add(allWords[index])
                        index++
                    } else {
                        index = 0 // البدء من الجديد عند الانتهاء
                        if (allWords.isNotEmpty()) {
                            chunk.add(allWords[index])
                            index++
                        }
                    }
                }

                if (chunk.isEmpty()) break

                val sentence = chunk.joinToString(" ")

                handler.post {
                    val ic = currentInputConnection
                    if (ic != null && isAutoRunning) {
                        ic.commitText(sentence, 1)
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                    }
                }

                try {
                    Thread.sleep(speed.coerceAtLeast(10))
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
