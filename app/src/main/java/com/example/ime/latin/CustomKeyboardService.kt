package com.example.ime.latin

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

class CustomKeyboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private var keyboardView: KeyboardView? = null
    private var keyboard: Keyboard? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val queue = ConcurrentLinkedQueue<String>()
    private var useTilde = false

    override fun onCreateInputView(): View {
        keyboardView = layoutInflater.inflate(R.layout.keyboard_surface, null) as KeyboardView
        keyboard = Keyboard(this, R.xml.kbd_main)
        keyboardView?.keyboard = keyboard
        keyboardView?.setOnKeyboardActionListener(this)
        return keyboardView!!
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            -5 -> ic.deleteSurroundingText(1, 0)
            -200 -> startAutoType()
            -101 -> useTilde = !useTilde
            32 -> ic.commitText(" ", 1)
            else -> ic.commitText(primaryCode.toChar().toString(), 1)
        }
    }

    private fun startAutoType() {
        queue.clear()
        queue.add("Hello world")
        queue.add("Testing custom keyboard")
        scope.launch {
            while (queue.isNotEmpty()) {
                val text = queue.poll() ?: break
                val target = if (useTilde) text.replace(" ", "~") else text
                for (ch in target) {
                    withContext(Dispatchers.Main) { currentInputConnection?.commitText(ch.toString(), 1) }
                    delay(50)
                }
                withContext(Dispatchers.Main) { sendDownUpKeyEvents(android.view.KeyEvent.KEYCODE_ENTER) }
                delay(200)
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
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
