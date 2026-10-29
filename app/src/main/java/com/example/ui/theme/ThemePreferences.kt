package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(
        ThemeMode.entries.firstOrNull { it.name == preferences.getString("theme", null) } ?: ThemeMode.Dark
    )
    val mode = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        preferences.edit().putString("theme", mode.name).apply()
        _mode.value = mode
    }
}
