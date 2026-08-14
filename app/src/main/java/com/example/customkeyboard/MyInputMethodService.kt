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
            -4 -> sendEnterKey()
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

    private fun sendEnterKey() {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun showControlPanel() {
        val view = layoutInflater.inflate(R.layout.layout_control_panel, null)

        val btnStart = view.findViewById<Button>(R.id.btnStartAutoText)
        val btnStop = view.findViewById<Button>(R.id.btnStopAutoText)
        val btnBack = view.findViewById<Button>(R.id.btnBackToKeyboard)

        btnStart.setOnClickListener {
            keyboardView?.let { setInputView(it) }
            startUniquePermutationTyping()
        }

        btnStop.setOnClickListener {
            isAutoRunning = false
        }

        btnBack.setOnClickListener {
            keyboardView?.let { setInputView(it) }
        }

        setInputView(view)
    }

    private fun startUniquePermutationTyping() {
        val prefs = getSharedPreferences("KeyboardPrefs", Context.MODE_PRIVATE)
        val speedMs = prefs.getInt("speed_ms", 50).toLong()
        val targetWordsCount = prefs.getInt("words_count", 4)
        val isDecoration = prefs.getBoolean("enable_decoration", false)
        val symbol = prefs.getString("decoration_symbol", "~") ?: "~"

        val jsonStr = prefs.getString("cliches_json", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        if (array.length() == 0) return

        // استخراج جميع الكلمات بدون تكرار
        val wordPool = ArrayList<String>()
        for (i in 0 until array.length()) {
            val line = array.getString(i).trim()
            if (line.isNotEmpty()) {
                val wordsInLine = line.split("\\s+".toRegex())
                for (w in wordsInLine) {
                    if (w.isNotEmpty()) wordPool.add(w)
                }
            }
        }

        if (wordPool.isEmpty()) return

        isAutoRunning = true

        Thread {
            // سجل لحفظ الجمل السابقة ومنع تكرار نفس الترتيب مطلقاً
            val generatedSentences = HashSet<String>()
            val maxWords = if (targetWordsCount > wordPool.size) wordPool.size else targetWordsCount

            while (isAutoRunning) {
                var uniqueSentence = ""
                var attempts = 0

                // توليد جملة بخلط عشوائي مع التأكد أنها لم تتكرر سابقاً
                while (attempts < 100) {
                    wordPool.shuffle()
                    val selectedWords = wordPool.take(maxWords)
                    val joiner = if (isDecoration) symbol else " "
                    val candidate = selectedWords.joinToString(joiner)

                    if (!generatedSentences.contains(candidate)) {
                        uniqueSentence = candidate
                        generatedSentences.add(candidate)
                        break
                    }
                    attempts++
                }

                // إذا استنفد التوليد الفريد للتركيبات، يتم إعادة مسح السجل والبدء بجولات جديدة
                if (uniqueSentence.isEmpty()) {
                    generatedSentences.clear()
                    wordPool.shuffle()
                    val selectedWords = wordPool.take(maxWords)
                    val joiner = if (isDecoration) symbol else " "
                    uniqueSentence = selectedWords.joinToString(joiner)
                    generatedSentences.add(uniqueSentence)
                }

                val sentenceToSend = uniqueSentence

                handler.post {
                    if (isAutoRunning) {
                        val ic = currentInputConnection
                        if (ic != null) {
                            ic.commitText(sentenceToSend, 1)
                            sendEnterKey()
                            keyboardView?.invalidateAllKeys()
                        }
                    }
                }

                try {
                    Thread.sleep(speedMs.coerceAtLeast(10))
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
