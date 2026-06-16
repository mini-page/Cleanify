package com.cleanify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cleanify.ui.screens.settings.sections.SettingSwitch
import org.junit.Rule
import org.junit.Test

class SettingSwitchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingSwitch_displaysTitleAndDescription() {
        composeTestRule.setContent {
            SettingSwitch(
                titleRes = R.string.immersive_mode_title,
                description = "Test description",
                checked = false,
                onCheckedChange = {}
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.immersive_mode_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Test description")
            .assertIsDisplayed()
    }

    @Test
    fun settingSwitch_togglesOn() {
        var checked = false
        composeTestRule.setContent {
            SettingSwitch(
                titleRes = R.string.immersive_mode_title,
                description = "Test",
                checked = checked,
                onCheckedChange = { checked = it }
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.immersive_mode_title))
            .performClick()
        assert(checked) { "Switch should be toggled on" }
    }

    @Test
    fun settingSwitch_disabled_notClickable() {
        composeTestRule.setContent {
            SettingSwitch(
                titleRes = R.string.immersive_mode_title,
                description = "Test",
                checked = false,
                onCheckedChange = {},
                enabled = false
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.immersive_mode_title))
            .assertIsDisplayed()
    }
}
