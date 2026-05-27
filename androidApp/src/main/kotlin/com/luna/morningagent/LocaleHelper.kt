package com.luna.morningagent

import android.content.Context
import java.util.Locale

object LocaleHelper {

    fun applyLocale(base: Context, langCode: String): Context {
        val locale = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)
        val config = base.resources.configuration.apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}
