package com.example.trails.ui.notification

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.ui.discovery.CampsiteSummaryCardTags
import com.example.trails.ui.lottery.LotteryType.Button
import com.example.trails.ui.lottery.millis
import com.google.firebase.Timestamp
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationList {
    val viewModel = NotificationViewModel()

    fun emptyCheck() : Boolean {
        return viewModel.fetchAll(CurrentUser.id()).isEmpty()
    }
    @Composable
    fun RenderList() {
        Column(modifier = Modifier.fillMaxSize())
        {
            for (item in viewModel.fetchAll(CurrentUser.id())) {
                when (item.type) {
                    NotificationType.LOTTERY_LOST -> LotteryLostCard(item)
                    NotificationType.LOTTERY_SINGLE -> LotteryWinCard(item)
                    NotificationType.LOTTERY_MULTI -> LotteryWinCard(item)
                    NotificationType.RESERVE_CONFIRM -> ReserveConfirmCard(item)
                    NotificationType.RESERVE_CANCEL -> LotteryWinCard(item)
                }
            }
        }
    }

    @Composable
    private fun LotteryWinCard(target: Notification) {
        val relativeTime = DateUtils.getRelativeTimeSpanString(target.timestamp.millis() as Long, System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS)

        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp)
                .padding(bottom = 5.dp))
            {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.5.em
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.content,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 2.em
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = relativeTime.toString(),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }

    @Composable
    private fun LotteryLostCard(target: Notification) {
        val relativeTime = DateUtils.getRelativeTimeSpanString(target.timestamp.millis() as Long, System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS)

        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp)
                .padding(bottom = 5.dp))
            {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.5.em
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.content,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 2.em
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = relativeTime.toString(),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }

    @Composable
    private fun ReserveConfirmCard(target: Notification) {
        val relativeTime = DateUtils.getRelativeTimeSpanString(target.timestamp.millis() as Long, System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS)

        val campsite : Campsite?
        runBlocking {
            campsite = AppContainer.campsiteRepository.getById(target.related?.campsiteId as String)
        }


        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .padding(vertical = 5.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp)
                .padding(bottom = 5.dp))
            {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = target.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 1.5.em
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                ) {
                    if (target.related != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp)
                                .padding(bottom = 5.dp)
                                .padding(start = 20.dp)
                        ) {
                            AsyncImage(
                                model = campsite?.imageUrl,
                                contentDescription = "Image of the campsite",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            campsite?.name?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            //TODO: location?
                            campsite?.name?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            target.related?.startDate?.let {
                                Text(
                                    text = "${timestampToString(it)} - ${timestampToString(target.related.endDate!!)} · [2 nights]",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            target.related?.partySize?.let {
                                Text(
                                    text = "$it · $${target.related.totalPrice} total",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    //TODO: button to "My Reservations"
                    //left aligned, highlight
                    //bg brighter than card
                    Button(shape = RoundedCornerShape(12.dp), onClick = {}) {
                        Text(text = "My Reservations")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    //TODO: CSV download button
                    Button(shape = RoundedCornerShape(12.dp), onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download"
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(text = "CSV")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = relativeTime.toString(),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }

    private fun timestampToString(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        return formatter.format(date)
    }
}