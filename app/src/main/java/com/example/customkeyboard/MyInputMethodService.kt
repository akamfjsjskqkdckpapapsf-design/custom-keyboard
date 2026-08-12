package com.example.customkeyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View

class MyInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var arabicKeyboard: Keyboard? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isAutoRunning = false

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        arabicKeyboard = Keyboard(this, R.xml.kbd_arabic)
        keyboardView?.keyboard = arabicKeyboard
        keyboardView?.setOnKeyboardActionListener(this)
        return keyboardView!!
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection = currentInputConnection ?: return
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)

        val triggerSymbol = prefs.getString("trigger_symbol", "") ?: ""
        val isAutoEnabled = prefs.getBoolean("enable_auto", false)
        val globalReplaceSpace = prefs.getBoolean("global_replace_space", false)

        when (primaryCode) {
            -5 -> {
                inputConnection.deleteSurroundingText(1, 0)
            }
            -4 -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            32 -> {
                if (globalReplaceSpace) {
                    inputConnection.commitText("~", 1)
                } else {
                    inputConnection.commitText(" ", 1)
                }
            }
            else -> {
                val charTyped = primaryCode.toChar().toString()
                inputConnection.commitText(charTyped, 1)

                if (isAutoEnabled && triggerSymbol.isNotEmpty()) {
                    val beforeText = inputConnection.getTextBeforeCursor(triggerSymbol.length, 0)?.toString() ?: ""
                    if (beforeText == triggerSymbol) {
                        if (!isAutoRunning) {
                            startAutoProcess()
                        } else {
                            isAutoRunning = false
                        }
                    }
                }
            }
        }
    }

    private fun startAutoProcess() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val rawText = prefs.getString("auto_text", "") ?: ""
        val speed = prefs.getInt("auto_speed", 50).toLong()
        val replaceSpace = prefs.getBoolean("auto_replace_space", false)
        val suffix = prefs.getString("suffix_text", "") ?: ""

        if (rawText.isEmpty()) return

        isAutoRunning = true
        val lines = rawText.split("\n")

        Thread {
            for (line in lines) {
                if (!isAutoRunning) break

                var formattedLine = line
                if (replaceSpace) {
                    formattedLine = formattedLine.replace(" ", "~")
                }
                if (suffix.isNotEmpty()) {
                    formattedLine += if (replaceSpace) "~$suffix" else " $suffix"
                }

                handler.post {
                    val ic = currentInputConnection ?: return@post
                    val triggerSymbol = prefs.getString("trigger_symbol", "") ?: ""
                    if (triggerSymbol.isNotEmpty()) {
                        ic.deleteSurroundingText(triggerSymbol.length, 0)
                    }

                    ic.commitText(formattedLine, 1)
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }

                try {
                    Thread.sleep((200 - speed).coerceAtLeast(10) * 10)
                } catch (e: InterruptedException) {
                    break
                }
            }
            isAutoRunning = false
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
