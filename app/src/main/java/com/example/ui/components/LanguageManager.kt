package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

val LocalAppLanguage = compositionLocalOf { "en" }

class LanguageState(initialLang: String = "en") {
    var currentLanguage by mutableStateOf(initialLang)
        private set

    fun setLanguage(lang: String) {
        currentLanguage = if (lang == "bn") "bn" else "en"
    }

    val isBengali: Boolean
        get() = currentLanguage == "bn"
}

@Composable
fun ProvideAppLanguage(
    languageState: LanguageState,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppLanguage provides languageState.currentLanguage) {
        content()
    }
}
