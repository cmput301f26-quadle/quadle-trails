package com.example.trails.ui.reservation

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.trails.R
import com.example.trails.data.AppContainer
import com.example.trails.data.fake.CurrentUser
import com.example.trails.data.model.Campsite
import com.example.trails.data.model.Reservation
import com.example.trails.data.model.ReservationStatus
import com.example.trails.ui.common.TrailsTopBar
import com.example.trails.ui.shell.ShellEmptyState
import com.example.trails.ui.theme.TrailsTheme
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import kotlin.collections.listOf
import kotlin.collections.plus
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReservationScreen(navController: NavController) {
    ReservationList().ShowList()
}

class ReservationList {
    private val viewModel = ReservationViewModel()
    @Composable
    fun ShowList() {
        var showingActive by remember { mutableIntStateOf(1) }
        var list by remember {mutableStateOf<List<Reservation>>(listOf())}
        LaunchedEffect(CurrentUser.id()) {
            viewModel.userReserveListener().collect {
                list = it
            }
        }
        if (list.isEmpty()) {
            ShellEmptyState(
                title = "My Reservations",
                icon = R.drawable.lottery_ic_calendar,
                message = "No reservations yet",
                hint = "Your bookings will appear here.",
                screenTag = ReservationTags.EMPTY_SCREEN,
                messageTag = ReservationTags.EMPTY_TEXT,
                modifier = Modifier,
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TrailsTopBar(
                    title = "My Reservations"
                )
                SecondaryTabRow(
                    modifier = Modifier
                        .padding(15.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    selectedTabIndex = showingActive,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = @Composable {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier
                                .tabIndicatorOffset(showingActive, matchContentSize = false)
                                .fillMaxHeight().padding(5.dp).clip(RoundedCornerShape(size = 8.dp)).zIndex(-1f)
                        )
                    },
                    divider = @Composable { HorizontalDivider() },
                ) { listOf("Active", "Past").forEachIndexed { index, title ->
                    Tab(
                        modifier = Modifier.zIndex(1f),
                        selected = showingActive == index,
                        onClick = { showingActive = index },
                        text = {
                            if (showingActive == index) {
                                Text(text = title, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text(text = title, color = Color.Gray)
                            }
                        }
                    )
                }
                }
                LazyColumn(modifier = Modifier) {
                    items(list) { item ->
                        println(item.id)
                        if (showingActive == 1) {
                            if (item.endDate!! > Timestamp.now()) {
                                ReservationItem(item)
                            }
                        } else {
                            if (item.endDate!! < Timestamp.now()) {
                                ReservationItem(item)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReservationItem(target: Reservation) {
        var campsite : Campsite? by remember { mutableStateOf(null) }
        LaunchedEffect(target.campsiteId) {
            withTimeoutOrNull(10_000L.milliseconds) {
                while (campsite == null) {
                    campsite = AppContainer.campsiteRepository.getById(target.campsiteId)
                    delay(2_000L.milliseconds)
                }
            }
            println(campsite?.name)
        }
        var cancelling : Boolean by remember { mutableStateOf(false) }
        val dateString = timestampToString(target.startDate!!, target.endDate!!)
        val context = LocalContext.current
        var csvSave : Boolean by remember { mutableStateOf(false) }
        OutlinedCard(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
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
                Row(
                    modifier =
                        Modifier
                ) {
                    Column(
                        modifier = Modifier
                            .padding(top = 15.dp)
                            .padding(bottom = 5.dp)
                            .padding(start = 20.dp)
                            .weight(0.25F)
                    ) {
                        AsyncImage(
                            model = campsite?.imageUrl,
                            contentDescription = "Image of the campsite",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.widthIn(max = 200.dp).heightIn(max = 200.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(0.75F)
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
                            Text(
                                text = "$dateString · 2 nights",
                                fontSize = 12.sp,
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${target.partySize} guests · $${target.totalPrice} total",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth().padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //left aligned, highlight
                    //bg brighter than card
                    OutlinedButton(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF1EB155)),
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E4234)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(text = "Confirmed", color = Color(0xFF21AB55), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        onClick = {
                            cancelling = true;
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(text = "Cancel", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.size(7.dp))

                    OutlinedButton(
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(1.dp, Color(0xFF1EB155)),
                        onClick = {
                            csvSave = true
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
            }
            if (cancelling) {
                CancelConfirmation(
                    reservation = target,
                    campsiteName = campsite?.name,
                    dateString = dateString,
                    exitDialog = { cancelling = false }
                )
            }

            if (csvSave) {
                saveCsv(
                    context = context,
                    target = target,
                    campsiteName = campsite!!.name,
                )
                csvSave = false
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CancelConfirmation(
        reservation: Reservation,
        campsiteName: String?,
        dateString: String?,
        exitDialog: () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        BasicAlertDialog(onDismissRequest = { }) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(15.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
                border = BorderStroke(2.dp, Color(0xFF1EB155))
            ) {
                Column(modifier = Modifier
                    .padding(18.dp)
                    .padding(horizontal = 3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF000000))
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "delete",
                                tint = Color(0xFF1EB155)
                            )
                        }
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = "Cancel reservation?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)) {
                        Text(
                            text = "$campsiteName · $dateString\nYour refund follows the campsite's\ncancellation policy.",
                            color = Color.Gray

                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, Color(0xFF1EB155)),
                            onClick = {
                                exitDialog()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "Keep Stay",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                        Spacer(modifier = Modifier.size(5.dp))
                        OutlinedButton(
                            shape = RoundedCornerShape(5.dp),
                            border = BorderStroke(1.dp, Color(0xFF1EB155)),
                            onClick = {
                                scope.launch {
                                    AppContainer.reservationRepository.updateStatus(reservation.id, ReservationStatus.CANCELLED)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                text = "Cancel Stay",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
        }
    }


    fun saveCsv(
        context: Context,
        target: Reservation,
        campsiteName: String
    ) {
        val row = listOf(
            listOf(
                "Campsite", "PartySize", "StartDate", "EndDate", "Price", "Status"
            ) + listOf(
                campsiteName, target.partySize, csvTimestamp(target.startDate!!), csvTimestamp(target.endDate!!), target.totalPrice, target.status
            ))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "reservation_export.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let { fileUri ->
                resolver.openOutputStream(fileUri)?.use { outputStream ->
                    csvWriter().writeAll(row, outputStream)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, "reservation_export.csv")

            csvWriter().writeAll(row, file, append = false)
        }
    }

    private fun csvTimestamp(
        target: Timestamp
    ) : String {
        return SimpleDateFormat("yyyy/MMM/dd", Locale.getDefault()).format(target.toDate())
    }

    private fun timestampToString(
        start: Timestamp,
        end: Timestamp,
    ) : String {
        val startDate = start.toDate()
        val endDate = end.toDate()
        val bothYearShowing = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val normalFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val sameMonth = SimpleDateFormat("dd", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.time = startDate
        val startYear = calendar.get(Calendar.YEAR)
        val startMonth = calendar.get(Calendar.MONTH)
        calendar.time = endDate
        val endYear = calendar.get(Calendar.YEAR)
        val endMonth = calendar.get(Calendar.MONTH)

        println("$startYear $endYear")
        println("$startMonth $endMonth")

        return if (startYear != endYear) {
            "${bothYearShowing.format(startDate)} - ${bothYearShowing.format(endDate)}"
        } else if (startMonth == endMonth) {
            "${normalFormat.format(startDate)} - ${sameMonth.format(endDate)}, $endYear"
        } else {
            "${normalFormat.format(startDate)} - ${normalFormat.format(endDate)}, $endYear"
        }
    }
}

object ReservationTags {
    const val EMPTY_SCREEN = "empty_reservations"
    const val EMPTY_TEXT = "empty_reservations_text"
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F0C)
@Composable
private fun ReservationListPreview() {
    TrailsTheme { ReservationList().ShowList() }
}