package com.cleanify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cleanify.ui.screens.settings.sections.SectionHeader
import org.junit.Rule
import org.junit.Test

class SectionHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sectionHeader_displaysTitle() {
        composeTestRule.setContent {
            SectionHeader(titleRes = R.string.settings)
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.settings))
            .assertIsDisplayed()
    }
}
