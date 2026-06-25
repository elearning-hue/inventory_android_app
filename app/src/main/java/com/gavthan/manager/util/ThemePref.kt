package com.gavthan.manager.util

import android.content.Context

/** Persists the light/dark choice, mirroring the web's `localStorage["gh-theme"]`. */
object ThemePref {
    private const val FILE = "gavthan-prefs"
    private const val KEY = "gh-theme"

    fun isDark(ctx: Context): Boolean =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "light") == "dark"

    fun setDark(ctx: Context, dark: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, if (dark) "dark" else "light")
            .apply()
    }
}
