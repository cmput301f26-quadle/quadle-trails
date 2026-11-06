// Shared reusable top app bar component providing consistent navigation and actions across screens (UI Component pattern).
// implements US 01.01.06, US 01.02.01, US 02.09.01
// Outstanding issues: None
// OWNER: shared — created by Chitkarsh (chitkarshrathi)
package com.example.trails.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trails.R
import com.example.trails.ui.theme.Mist
import com.example.trails.ui.theme.Night
import com.example.trails.ui.theme.TrailGreen
import com.example.trails.ui.theme.TrailsTheme

// shared top bar with standard string title and back navigation
/**
 * Shared top app bar composable providing consistent typography, navigation icons, and action items.
 *
 * Observes [title], optional [subtitle], action buttons, and back button configuration.
 * Triggers [onBack] when the navigation icon is clicked, and [onAction] when the action label is tapped.
 *
 * @param title text displayed as the primary screen title.
 * @param modifier optional [Modifier] for top bar layout adjustments.
 * @param onBack optional callback invoked when the user taps the back navigation icon; omitted if null.
 * @param backTestTag optional test tag assigned to the back button icon.
 * @param backContentDescription accessibility description for the back button icon.
 * @param useChevron if true, renders a chevron left icon instead of the standard back arrow.
 * @param subtitle optional secondary text rendered below the primary title.
 * @param actionLabel optional text button displayed on the right edge of the top bar.
 * @param onAction callback invoked when the [actionLabel] text is clicked.
 * @param actionTestTag optional test tag assigned to the action text button.
 * @param actions composable slot for rendering additional icon buttons or menu items on the right.
 * @param containerColor background color for the app bar surface.
 * @param contentColor color applied to title text and navigation icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backTestTag: String? = null,
    backContentDescription: String = "Back",
    useChevron: Boolean = false,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionTestTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = Night,
    contentColor: Color = Color.White
) {
    TrailsTopBar(
        titleContent = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist
                    )
                }
            }
        },
        modifier = modifier,
        onBack = onBack,
        backTestTag = backTestTag,
        backContentDescription = backContentDescription,
        useChevron = useChevron,
        actionLabel = actionLabel,
        onAction = onAction,
        actionTestTag = actionTestTag,
        actions = actions,
        containerColor = containerColor,
        contentColor = contentColor
    )
}

// flexible top bar overload allowing custom composable title content
/**
 * Flexible top app bar composable supporting arbitrary custom composable title content.
 *
 * Observes custom [titleContent], action slots, and navigation parameters.
 * Triggers [onBack] when the navigation icon is tapped, and [onAction] when the action label is clicked.
 *
 * @param titleContent composable lambda rendering the title content.
 * @param modifier optional [Modifier] for top bar layout adjustments.
 * @param onBack optional callback invoked when the back icon is tapped.
 * @param backTestTag optional test tag assigned to the back button.
 * @param backContentDescription accessibility description for the back navigation icon.
 * @param useChevron if true, renders a chevron icon instead of an arrow icon.
 * @param actionLabel optional text button displayed on the right.
 * @param onAction callback invoked when the action label is clicked.
 * @param actionTestTag optional test tag for the action label.
 * @param actions composable slot for rendering right-aligned action elements.
 * @param containerColor background color for the top bar surface.
 * @param contentColor color applied to the navigation icons and title container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailsTopBar(
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backTestTag: String? = null,
    backContentDescription: String = "Back",
    useChevron: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionTestTag: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = Night,
    contentColor: Color = Color.White
) {
    TopAppBar(
        title = titleContent,
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = if (!backTestTag.isNullOrBlank()) Modifier.testTag(backTestTag) else Modifier
                ) {
                    if (useChevron) {
                        Icon(
                            painter = painterResource(R.drawable.lottery_ic_chevron_left),
                            contentDescription = backContentDescription,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backContentDescription,
                            tint = contentColor
                        )
                    }
                }
            }
        },
        actions = {
            if (!actionLabel.isNullOrBlank()) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TrailGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .then(if (!actionTestTag.isNullOrBlank()) Modifier.testTag(actionTestTag) else Modifier)
                )
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = contentColor,
            navigationIconContentColor = contentColor,
            actionIconContentColor = contentColor
        ),
        modifier = modifier
    )
}

// preview of standard top bar with back navigation
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun TrailsTopBarPreview_WithBack() {
    TrailsTheme {
        TrailsTopBar(
            title = "Join Lottery",
            onBack = {}
        )
    }
}

// preview of top bar with chevron navigation icon
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun TrailsTopBarPreview_Chevron() {
    TrailsTheme {
        TrailsTopBar(
            title = "Campsite Details",
            onBack = {},
            useChevron = true
        )
    }
}

// preview of top bar with action text button
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun TrailsTopBarPreview_WithActions() {
    TrailsTheme {
        TrailsTopBar(
            title = "Hosting Lotteries",
            onBack = {},
            actionLabel = "Create",
            onAction = {}
        )
    }
}

// preview of top bar with subtitle
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun TrailsTopBarPreview_WithSubtitle() {
    TrailsTheme {
        TrailsTopBar(
            title = "Grande Cache Ridge",
            subtitle = "Hinton – Grande Cache, AB",
            onBack = {}
        )
    }
}

// preview of root screen top bar without back button
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun TrailsTopBarPreview_Root() {
    TrailsTheme {
        TrailsTopBar(
            title = "Discover"
        )
    }
}
