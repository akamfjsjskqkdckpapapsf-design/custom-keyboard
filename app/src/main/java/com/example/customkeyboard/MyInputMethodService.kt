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

        // تفعيل معايرة الضغطة المباشرة (تغيير لون/شكل الزر عند الضغط كما في المقطع)
        keyboardView?.isPreviewEnabled = true

        return keyboardView!!
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // إيقاف التسطير تلقائياً فور الخروج من التطبيق أو حقل النص
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
            startSequentialTyping()
        }

        btnStop.setOnClickListener {
            isAutoRunning = false
        }

        btnBack.setOnClickListener {
            keyboardView?.let { setInputView(it) }
        }

        setInputView(view)
    }

    private fun startSequentialTyping() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val fullText = prefs.getString("long_text_source", "") ?: ""
        val wordsPerLine = prefs.getInt("words_per_line", 6)
        val speedMs = prefs.getInt("speed_ms", 50).toLong()

        if (fullText.trim().isEmpty()) return

        // تقسيم النص إلى كلمات متسلسلة بدقة
        val wordsList = fullText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (wordsList.isEmpty()) return

        isAutoRunning = true

        Thread {
            var pointer = prefs.getInt("current_word_pointer", 0)

            while (isAutoRunning) {
                val lineWords = ArrayList<String>()

                // قراءة الكلمات بالترتيب من البداية
                for (i in 0 until wordsPerLine) {
                    if (pointer >= wordsList.size) {
                        pointer = 0 // عند وصول النهاية يرجع للبداية
                    }
                    lineWords.add(wordsList[pointer])
                    pointer++
                }

                // حفظ مؤشر الكلمة التالية لضمان عدم التكرار
                prefs.edit().putInt("current_word_pointer", pointer).apply()

                val lineToType = lineWords.joinToString(" ")

                // محاكاة كتابة الحروف مع الضغط البصري
                for (char in lineToType) {
                    if (!isAutoRunning) break

                    val charCode = char.code
                    handler.post {
                        if (isAutoRunning) {
                            onKey(charCode, null)
                        }
                    }

                    try {
                        Thread.sleep(speedMs.coerceAtLeast(10))
                    } catch (e: Exception) { break }
                }

                // إرسال السطر فور اكتماله
                if (isAutoRunning) {
                    handler.post { sendEnter() }
                }

                try {
                    Thread.sleep((speedMs * 2).coerceAtLeast(20))
                } catch (e: Exception) { break }
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
