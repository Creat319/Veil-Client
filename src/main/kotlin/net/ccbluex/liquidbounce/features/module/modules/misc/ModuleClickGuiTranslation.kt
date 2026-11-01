/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.misc

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClickGuiLocalizationChangeEvent
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.text.toLowerCamelCase

/**
 * Shows the ClickGUI in Chinese.
 *
 * The client only ships translations for module descriptions, so this module adds the module,
 * category and setting labels (`<key>.name`) on top of them. Only the labels rendered by the
 * ClickGUI are replaced - identifiers stay English, so binds, configs and commands keep working
 * no matter which language is displayed.
 */
object ModuleClickGuiTranslation : ClientModule(
    "ClickGuiTranslation",
    ModuleCategories.MISC,
    state = true,
    aliases = listOf("ClickGuiChinese", "ChineseNames", "GuiTranslation"),
) {

    /**
     * Setting labels are keyed by the label itself (`liquidbounce.setting.Range.name`), so one
     * entry covers the same word in every module.
     */
    private const val SETTING_KEY_PREFIX = "${ConfigSystem.KEY_PREFIX}.setting"

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.clickGuiTranslation"

    init {
        enabledValue.onChanged {
            EventManager.callEvent(ClickGuiLocalizationChangeEvent(enabled))
        }
    }

    /**
     * Chinese label of [module], or `null` while this module is disabled or the name is
     * not translated yet. Callers use the returned value only for display.
     */
    fun localizedModuleName(module: ClientModule): String? =
        translated("${module.baseKey}.name", module.name)

    /**
     * Chinese label of [category], or `null` while this module is disabled.
     */
    fun localizedCategoryName(category: ModuleCategory): String? =
        translated("${ConfigSystem.KEY_PREFIX}.category.${category.tag.toLowerCamelCase()}.name", category.tag)

    /**
     * Adds `localizedName` to every labelled object of a serialized settings tree, so the ClickGUI
     * can render value, group and mode labels in Chinese without touching any identifier.
     *
     * Translation keys are the labels themselves (`liquidbounce.setting.<label>.name`), which
     * keeps one entry per word instead of one per module and setting.
     */
    fun applyLocalizedNames(element: JsonElement) {
        if (!enabled) {
            return
        }

        val language = LanguageManager.languageOf(LanguageManager.ClientLanguage.ZH_CN) ?: return
        walk(element, language)
    }

    private fun walk(element: JsonElement, language: net.ccbluex.liquidbounce.lang.ClientLanguage) {
        when (element) {
            is JsonObject -> {
                element.get("name")?.takeIf(JsonElement::isJsonPrimitive)?.let { name ->
                    val label = name.asString
                    val translated = language.getOrDefault("$SETTING_KEY_PREFIX.$label.name", label)

                    if (translated != label) {
                        element.addProperty("localizedName", translated)
                    }
                }

                element.entrySet().forEach { (_, child) -> walk(child, language) }
            }

            is JsonArray -> element.forEach { walk(it, language) }
            else -> {}
        }
    }

    private fun translated(key: String, fallback: String): String? {
        if (!enabled) {
            return null
        }

        val language = LanguageManager.languageOf(LanguageManager.ClientLanguage.ZH_CN) ?: return null
        return language.getOrDefault(key, fallback).takeIf { it != fallback }
    }

}
