package com.example.customkeyboard

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import org.json.JSONArray

class MyInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var controlPanelView: View? = null
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
        val fontIdx = prefs.getInt("font_size", 1)

        val colors = arrayOf("#121212", "#000000", "#0A192F", "#004D40", "#3E2723", "#212121", "#B71C1C", "#4A148C", "#311B92", "#0277BD")
        val bgColor = Color.parseColor(colors.getOrElse(themeIdx) { "#121212" })
        keyboardView?.setBackgroundColor(bgColor)

        val sizes = floatArrayOf(14f, 18f, 22f)
        // تطبيق حجم الحروف اختياري
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
        val btnCliches = view.findViewById<Button>(R.id.btnShowClichesDialog)
        val btnStop = view.findViewById<Button>(R.id.btnStopAutoText)
        val btnBack = view.findViewById<Button>(R.id.btnBackToKeyboard)

        btnStart.setOnClickListener {
            startInteractiveTyping()
        }

        btnCliches.setOnClickListener {
            showClichesInKeyboard()
        }

        btnStop.setOnClickListener {
            isAutoRunning = false
        }

        btnBack.setOnClickListener {
            setInputView(keyboardView)
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
        Thread {
            var index = 0
            while (isAutoRunning && index < allWords.size) {
                val chunk = ArrayList<String>()
                for (j in 0 until wordsCount) {
                    if (index < allWords.size) {
                        chunk.add(allWords[index])
                        index++
                    } else {
                        index = 0 // إعادة البدء من البداية عند انتهاء النص
                        break
                    }
                }

                val sentence = chunk.joinToString(" ")
                handler.post {
                    val ic = currentInputConnection
                    ic?.commitText(sentence, 1)
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }

                try {
                    Thread.sleep(speed.coerceAtLeast(5))
                } catch (e: Exception) {
                    break
                }
            }
            isAutoRunning = false
        }.start()
    }

    private fun showClichesInKeyboard() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("cliches_json", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val list = ArrayList<String>()
        for (i in 0 until array.length()) list.add(array.getString(i))

        if (list.isEmpty()) return

        val builder = AlertDialog.Builder(this)
        builder.setTitle("اختر كليشة لإدراجها أو حذفها")
        builder.setItems(list.toTypedArray()) { _, which ->
            currentInputConnection?.commitText(list[which], 1)
        }
        val dialog = builder.create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
