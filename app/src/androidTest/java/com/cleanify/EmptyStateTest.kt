package com.cleanify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cleanify.ui.components.EmptyState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import org.junit.Rule
import org.junit.Test

class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysTitle() {
        composeTestRule.setContent {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                titleRes = R.string.empty_swiper_title,
                descriptionRes = R.string.empty_swiper_desc
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.empty_swiper_title))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysDescription() {
        composeTestRule.setContent {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                titleRes = R.string.empty_swiper_title,
                descriptionRes = R.string.empty_swiper_desc
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.empty_swiper_desc))
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_displaysActionWhenProvided() {
        composeTestRule.setContent {
            EmptyState(
                icon = Icons.Default.CheckCircle,
                titleRes = R.string.empty_swiper_title,
                descriptionRes = R.string.empty_swiper_desc,
                actionTextRes = R.string.review_changes
            )
        }
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.review_changes))
            .assertIsDisplayed()
    }
}
