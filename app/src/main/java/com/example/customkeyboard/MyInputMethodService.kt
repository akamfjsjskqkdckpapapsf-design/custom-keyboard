package com.example.customkeyboard

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import java.util.concurrent.Executors

class MyInputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private var keyboardView: KeyboardView? = null
    private var arabicKeyboard: Keyboard? = null
    @Volatile private var isAutoRunning = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executorService = Executors.newSingleThreadExecutor()

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as KeyboardView
        arabicKeyboard = Keyboard(this, R.xml.kbd_arabic)
        keyboardView?.keyboard = arabicKeyboard
        keyboardView?.setOnKeyboardActionListener(this)

        // تفعيل المعايرة البصرية المباشرة الضغط
        keyboardView?.isPreviewEnabled = false

        return keyboardView!!
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // إيقاف الكتابة فور الانتقال لمنطقة أخرى لمنع تعليق الكيبورد
        isAutoRunning = false
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -4 -> sendEnter()
            -100 -> showSubMenu()
            32 -> ic.commitText(" ", 1)
            else -> ic.commitText(primaryCode.toChar().toString(), 1)
        }
    }

    private fun sendEnter() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun showSubMenu() {
        val view = layoutInflater.inflate(R.layout.layout_control_panel, null)

        val btnStart = view.findViewById<Button>(R.id.btnStartAutoText)
        val btnStop = view.findViewById<Button>(R.id.btnStopAutoText)
        val btnBack = view.findViewById<Button>(R.id.btnBackToKeyboard)

        btnStart.setOnClickListener {
            keyboardView?.let { setInputView(it) }
            startSequentialTypingSmooth()
        }

        btnStop.setOnClickListener {
            isAutoRunning = false
        }

        btnBack.setOnClickListener {
            keyboardView?.let { setInputView(it) }
        }

        setInputView(view)
    }

    private fun highlightKeyByChar(char: Char) {
        val keys = arabicKeyboard?.keys ?: return
        val targetCode = char.code

        for (key in keys) {
            if (key.codes.contains(targetCode)) {
                key.pressed = true
                keyboardView?.invalidateAllKeys()

                mainHandler.postDelayed({
                    key.pressed = false
                    keyboardView?.invalidateAllKeys()
                }, 40)
                break
            }
        }
    }

    private fun startSequentialTypingSmooth() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val fullText = prefs.getString("long_text_source", "") ?: ""
        val suffixText = prefs.getString("suffix_text", "")?.trim() ?: ""
        val wordsPerLine = prefs.getInt("words_per_line", 6)
        val speedMs = prefs.getInt("speed_ms", 50).toLong()

        if (fullText.trim().isEmpty()) return

        val wordsList = fullText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (wordsList.isEmpty()) return

        isAutoRunning = true

        // تشغيل العملية في خيط منفصل تماماً لتفادي رسالة "لا يستجيب"
        executorService.execute {
            var pointer = prefs.getInt("current_word_pointer", 0)

            while (isAutoRunning) {
                val lineWords = ArrayList<String>()

                for (i in 0 until wordsPerLine) {
                    if (pointer >= wordsList.size) {
                        pointer = 0
                    }
                    lineWords.add(wordsList[pointer])
                    pointer++
                }

                prefs.edit().putInt("current_word_pointer", pointer).apply()

                val lineToType = lineWords.joinToString(" ")

                // 1. كتابة كلمات السطر حرفاً بحرف مع الضغط البصري
                for (char in lineToType) {
                    if (!isAutoRunning) break

                    val charCode = char.code
                    mainHandler.post {
                        if (isAutoRunning) {
                            highlightKeyByChar(char)
                            onKey(charCode, null)
                        }
                    }

                    try {
                        Thread.sleep(speedMs.coerceAtLeast(15))
                    } catch (e: Exception) { break }
                }

                // 2. إضافة النص الملحق / المنشن ككتلة قاطعة وفصلها بمسافة مسقلة
                if (isAutoRunning && suffixText.isNotEmpty()) {
                    mainHandler.post {
                        val ic = currentInputConnection
                        ic?.commitText(" $suffixText", 1)
                    }
                    try {
                        Thread.sleep(speedMs.coerceAtLeast(15))
                    } catch (e: Exception) { break }
                }

                // 3. إرسال السطر معزولاً بدون كتابة إضافية بعده
                if (isAutoRunning) {
                    mainHandler.post { sendEnter() }
                }

                try {
                    Thread.sleep((speedMs * 2).coerceAtLeast(30))
                } catch (e: Exception) { break }
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
