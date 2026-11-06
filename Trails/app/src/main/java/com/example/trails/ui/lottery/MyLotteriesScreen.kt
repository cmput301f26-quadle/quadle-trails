// OWNER: Skanda (skandarajeev) — Lottery Setup
// USER STORIES: supports 02.04.06
// PURPOSE: Lists the lotteries the current owner has created, inside the Lotteries tab of
//  Hosting (Figma F06 · Lotteries). Tapping a card opens its details; the button starts a new one.
// Outstanding issues: none.
package com.example.trails.ui.lottery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trails.R
import com.example.trails.data.fake.TestLotteries
import com.example.trails.ui.theme.TrailsTheme

/** Connects [MyLotteriesViewModel] to navigation and renders [MyLotteriesContent]. */
@Composable
fun MyLotteriesScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MyLotteriesViewModel = viewModel { MyLotteriesViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    MyLotteriesContent(
        state = state,
        onFilterChange = viewModel::onFilterChange,
        onOpenLottery = { id -> navController.navigate(LotteryRoutes.lotteryDetails(id)) },
        onCreate = { navController.navigate(LotteryRoutes.lotteryForm()) },
        modifier = modifier
    )
}

/**
 * Stateless Lotteries tab, split out so previews and UI tests can drive it directly.
 * Hosting already adds the side padding and the tabs above.
 *
 * @param state what to show.
 * @param onFilterChange called when a filter chip is tapped.
 * @param onOpenLottery called with a lottery's id when its card is tapped.
 * @param onCreate called when "Create a lottery" is tapped.
 */
@Composable
fun MyLotteriesContent(
    state: MyLotteriesUiState,
    onFilterChange: (LotteryFilter) -> Unit,
    onOpenLottery: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        val selected = (state as? MyLotteriesUiState.Loaded)?.filter ?: LotteryFilter.ALL
        FilterChips(selected, onFilterChange)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state is MyLotteriesUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center).testTag(LotteryTags.LOADING))
                state is MyLotteriesUiState.Loaded && !state.hasAny -> EmptyMessage(
                    stringResource(R.string.lottery_list_empty_title), stringResource(R.string.lottery_list_empty_body)
                )
                state is MyLotteriesUiState.Loaded && state.items.isEmpty() -> EmptyMessage(stringResource(R.string.lottery_list_no_match), null)
                state is MyLotteriesUiState.Loaded -> LazyColumn(
                    Modifier.fillMaxSize().testTag(LotteryTags.LIST),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(state.items, key = { it.lottery.id }) { item ->
                        LotteryCardView(item, onClick = { onOpenLottery(item.lottery.id) })
                    }
                }
            }
        }
        Box(Modifier.padding(vertical = 12.dp)) {
            LotteryButton(stringResource(R.string.lottery_create), onClick = onCreate, modifier = Modifier.testTag(LotteryTags.CREATE))
        }
    }
}

/** All / Scheduled / Open / Done chips; the selected one is green with a tick. */
@Composable
private fun FilterChips(selected: LotteryFilter, onSelect: (LotteryFilter) -> Unit) {
    val labels = mapOf(
        LotteryFilter.ALL to R.string.lottery_filter_all,
        LotteryFilter.SCHEDULED to R.string.lottery_filter_scheduled,
        LotteryFilter.OPEN to R.string.lottery_filter_open,
        LotteryFilter.DONE to R.string.lottery_filter_done
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LotteryFilter.entries.forEach { chip ->
            val isSelected = chip == selected
            val shape = RoundedCornerShape(8.dp)
            val green = MaterialTheme.colorScheme.primary
            Row(
                Modifier
                    .height(34.dp)
                    .clip(shape)
                    .background(if (isSelected) green else MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, if (isSelected) green else LotteryColors.Line, shape)
                    .clickable { onSelect(chip) }
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .padding(horizontal = 12.dp)
                    .testTag(LotteryTags.filter(chip)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSelected) {
                    Icon(painterResource(R.drawable.lottery_ic_check), contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                }
                Text(
                    stringResource(labels.getValue(chip)),
                    style = LotteryType.Small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else LotteryColors.ChipText
                )
            }
        }
    }
}

/** Card: name and badge, what happens next, and a summary line (Figma "Lottery · …"). */
@Composable
private fun LotteryCardView(item: LotteryCardItem, onClick: () -> Unit) {
    val lottery = item.lottery
    LotteryCard(highlighted = true, onClick = onClick, modifier = Modifier.testTag(LotteryTags.card(lottery.id))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(lottery.name, style = LotteryType.CardTitle, modifier = Modifier.weight(1f))
                    StatusBadge(item.status)
                }
                Text(cardHeadline(item), style = LotteryType.CardLine)
                Text(cardSummary(item), style = LotteryType.Small, color = LotteryColors.Muted)
            }
            Icon(painterResource(R.drawable.lottery_ic_chevron_right), contentDescription = null, tint = LotteryColors.Muted, modifier = Modifier.size(16.dp))
        }
    }
}

/** "Registration opens Apr 1, 09:00", "Draw runs Jun 1, 09:00 · automatic", "5 winners drawn from 31 entrants"… */
@Composable
private fun cardHeadline(item: LotteryCardItem): String {
    val l = item.lottery
    val dash = stringResource(R.string.lottery_dash)
    return when (item.status) {
        LotteryStatus.SCHEDULED -> stringResource(R.string.lottery_card_opens, l.registrationStart.millis()?.let { formatDateTime(it) } ?: dash)
        LotteryStatus.OPEN -> stringResource(R.string.lottery_card_draw_runs, l.registrationEnd.millis()?.let { formatDateTime(it) } ?: dash)
        LotteryStatus.DRAWN -> stringResource(
            R.string.lottery_card_drawn,
            pluralStringResource(R.plurals.lottery_count_winners, item.winnersDrawn, item.winnersDrawn),
            pluralStringResource(R.plurals.lottery_count_entrants, item.entrantCount, item.entrantCount)
        )
        LotteryStatus.NO_ENTRANTS -> stringResource(R.string.lottery_card_no_entrants)
    }
}

/** "2 campsites · 5 winners", "24 entrants · 5 winners" or "1 campsite · Mar 2027". */
@Composable
private fun cardSummary(item: LotteryCardItem): String {
    val l = item.lottery
    val winners = pluralStringResource(R.plurals.lottery_count_winners, l.numWinners, l.numWinners)
    val campsites = pluralStringResource(R.plurals.lottery_count_campsites, l.campsiteIds.size, l.campsiteIds.size)
    return when (item.status) {
        LotteryStatus.SCHEDULED -> stringResource(R.string.lottery_dot_join, campsites, winners)
        LotteryStatus.OPEN -> stringResource(R.string.lottery_dot_join,
            pluralStringResource(R.plurals.lottery_count_entrants, item.entrantCount, item.entrantCount), winners)
        LotteryStatus.DRAWN, LotteryStatus.NO_ENTRANTS -> stringResource(R.string.lottery_dot_join, campsites,
            l.registrationEnd.millis()?.let { formatMonthYear(it) } ?: stringResource(R.string.lottery_dash))
    }
}

@Composable
private fun EmptyMessage(title: String, body: String?) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 8.dp).testTag(LotteryTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = LotteryType.CardTitle, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = LotteryType.Hint, color = LotteryColors.Muted, textAlign = TextAlign.Center)
    }
}

/** Design-time preview with the three sample lotteries (Figma F06). */
@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C, heightDp = 640)
@Composable
private fun MyLotteriesPreview() {
    TrailsTheme {
        Box(Modifier.padding(24.dp)) {
            MyLotteriesContent(
                state = buildMyLotteriesState(TestLotteries.all, mapOf(TestLotteries.LOTTERY_ID to 24), LotteryFilter.ALL, System.currentTimeMillis()),
                onFilterChange = {}, onOpenLottery = {}, onCreate = {}
            )
        }
    }
}
