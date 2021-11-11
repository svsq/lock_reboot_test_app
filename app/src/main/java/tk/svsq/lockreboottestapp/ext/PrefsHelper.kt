package tk.svsq.lockreboottestapp.ext

import android.content.Context

class PrefsHelper(private val context: Context) {

    companion object {
        const val IS_LOCKED_KEY = "IS_LOCKED"
    }

    var isLocked: Boolean
        get() = context.load(IS_LOCKED_KEY) ?: false
        set(value) = context.save(IS_LOCKED_KEY, value)
}