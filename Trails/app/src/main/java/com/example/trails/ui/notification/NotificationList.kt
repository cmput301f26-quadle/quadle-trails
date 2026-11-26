package com.example.trails.ui.notification

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Notification
import com.example.trails.data.model.NotificationType
import com.example.trails.ui.lottery.millis
import com.example.trails.ui.reservation.ReservationList
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Class for rendering a list of notifications.
 *
 * @property viewModel Notification view model.
 */
class NotificationList {
    val viewModel = NotificationViewModel()

    /**
     * Checks if the current user has any notifications.
     */
    fun emptyCheck() : Boolean {
        return viewModel.fetchAll(CurrentUser.id()).isEmpty()
    }

    /**
     * Renders a list of notifications of the current user.
     *
     * @param navController An instance of the navigation controller, in order to move to the reservation list screen if needed.
     */
    @Composable
    fun RenderList(navController: NavController) {
        Column(modifier = Modifier.fillMaxSize())
        {
            for (item in viewModel.fetchAll(CurrentUser.id())) {
                when (item.type) {
                    NotificationType.LOTTERY_LOST -> LotteryLostCard(item) {
                        navController.navigate(
                            NotificationRoutes.RESERVATION_LIST
                        )
                    }

                    NotificationType.LOTTERY_SINGLE -> LotteryWinCard(item) {
                        navController.navigate(
                            NotificationRoutes.RESERVATION_LIST
                        )
                    }

                    NotificationType.LOTTERY_MULTI -> LotteryWinCard(item) {
                        navController.navigate(
                            NotificationRoutes.RESERVATION_LIST
                        )
                    }

                    NotificationType.RESERVE_CONFIRM -> ReserveCard(item, false) {
                        navController.navigate(
                            NotificationRoutes.RESERVATION_LIST
                        )
                    }

                    NotificationType.RESERVE_CANCEL -> ReserveCard(item, true) {
                        navController.navigate(
                            NotificationRoutes.RESERVATION_LIST
                        )
                    }

                    NotificationType.OWNER_MESSAGE -> LotteryWinCard(item) {}
                }
            }
        }
    }

    /**
     * Helper function to render a card for a lottery win notification.
     *
     * @param target Notification to render.
     * @param toReservations Lambda for the "My Reservations" button.
     */
    @Composable
    private fun LotteryWinCard(target: Notification, toReservations: () -> Unit) {
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

    /**
     * Helper function to render a card for a lottery lost notification.
     *
     * @param target Notification to render.
     * @param toReservations Lambda for the "My Reservations" button.
     */
    @Composable
    private fun LotteryLostCard(target: Notification, toReservations: () -> Unit) {
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

    /**
     * Helper function to render a card for a reservation notification.
     *
     * @param target Notification to render.
     * @param cancel Whether or not this reservation was canceled, true for canceled, false for active.
     * @param toReservations Lambda for the "My Reservations" button.
     */
    @Composable
    private fun ReserveCard(target: Notification, cancel: Boolean, toReservations: () -> Unit) {
        val relativeTime = DateUtils.getRelativeTimeSpanString(target.timestamp.millis() as Long, System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS)

        var campsite : Campsite? by remember { mutableStateOf(null) }
        LaunchedEffect(target.related) {
            withTimeoutOrNull(10_000L.milliseconds) {
                while (campsite == null) {
                    campsite = AppContainer.campsiteRepository.getById(target.related?.campsiteId!!)
                    delay(2_000L.milliseconds)
                }
            }
        }
        val context = LocalContext.current

        val backgroundColor = if (cancel) { MaterialTheme.colorScheme.background } else { MaterialTheme.colorScheme.primaryContainer }
        val borderColor = if (cancel) { MaterialTheme.colorScheme.error } else { MaterialTheme.colorScheme.primary }

        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = backgroundColor
            ),
            border = BorderStroke(2.dp, borderColor),
            modifier = Modifier
                .padding(vertical = 10.dp)
                .padding(horizontal = 15.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 15.dp)
                .padding(bottom = 12.dp))
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
                ) {
                    if (target.related != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp)
                                .padding(bottom = 5.dp)
                                .padding(start = 20.dp).weight(0.25F)
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
                            .weight(0.75F)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            campsite?.name?.let {
                                Text(
                                    text = it,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 1.5.em
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            campsite?.locationName?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            target.related?.startDate?.let {
                                Text(
                                    text = "${ReservationList().timestampToString(target.related.startDate, target.related.endDate!!)} · ${daysBetween(target.related.startDate, target.related.endDate)} nights",
                                    fontSize = 12.sp,
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
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF1EB155)),
                        onClick = { toReservations() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E4234)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(text = "My Reservations")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF1EB155)),
                        onClick = {
                            ReservationList().saveCsv(
                                context = context,
                                target = target.related!!,
                                campsiteName = campsite!!.name,
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color(0xFF21AB55),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(3.dp))
                        Text(text = "CSV", color = Color(0xFF21AB55), fontSize = 12.sp)
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

    /**
     * Helper function to calculate days between two timestamps.
     * Not private because ReservationList also uses it.
     *
     * @param startDate Start of the date range to calculate.
     * @param endDate End of the date range to calculate.
     *
     * @return Amount of days between the dates, as Int.
     */
    fun daysBetween(startDate: Timestamp, endDate: Timestamp): Int {
        val startInstant = Instant.ofEpochSecond(startDate.seconds, startDate.nanoseconds.toLong())
        val endInstant = Instant.ofEpochSecond(endDate.seconds, endDate.nanoseconds.toLong())
        val dateStarting = startInstant.atZone(ZoneId.systemDefault()).toLocalDate()
        val dateEnding = endInstant.atZone(ZoneId.systemDefault()).toLocalDate()

        return ChronoUnit.DAYS.between(dateStarting, dateEnding).toInt()
    }
}