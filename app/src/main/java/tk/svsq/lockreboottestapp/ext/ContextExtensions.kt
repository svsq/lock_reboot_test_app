package tk.svsq.lockreboottestapp.ext

import android.content.Context
import java.util.*
import com.google.gson.Gson

const val PREFS_KEY = "LockRebootTestApp"

fun <T> Context.save(id: String = UUID.randomUUID().toString(), value: T) {
    getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        .edit()
        .putString(id, Gson().toJson(value))
        .apply()
}

fun Context.remove(id: String = UUID.randomUUID().toString()) {
    getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        .edit()
        .remove(id)
        .apply()
}

fun Context.removeAll() {
    getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .apply()
}

inline fun <reified T: Any> Context.load(id: String): T? {
    val preferences = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
    preferences.getString(id, null)?.let {
        return Gson().fromJson(it, T::class.java)
    }
    return null
}