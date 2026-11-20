package com.punto.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color as AColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Bg = Color(0xFF000000)
private val Tx = Color(0xFFFFFFFF)
private val Red = Color(0xFFD71921)
private val Mut = Color(0xFF8C8C8C)
private val S1 = Color(0xFF161616)
private val Ln = Color(0xFF2B2B2B)
private val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AColor.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Alarms.reschedule(this)
        setContent { PuntoApp() }
    }
}

@Composable
fun PuntoApp() {
    val scheme = darkColorScheme(
        background = Bg, surface = S1, primary = Red,
        onBackground = Tx, onSurface = Tx, onPrimary = Tx
    )
    MaterialTheme(colorScheme = scheme) {
        var tab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Horario", "Hábitos", "Cubo", "Tareas")
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Bg) {
                    tabs.forEachIndexed { i, name ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Box(Modifier.size(7.dp).clip(CircleShape).background(if (tab == i) Red else Ln)) },
                            label = { Text(name) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedTextColor = Tx,
                                unselectedTextColor = Mut
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Header()
                if (tab == 0) {
                    ScheduleScreen()
                } else {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("${tabs[tab]} llega en la siguiente parte", color = Mut)
                    }
                }
            }
        }
    }
}

@Composable
fun Header() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            color = Tx, fontSize = 56.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
        )
        Text(
            now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-MX"))),
            color = Mut, fontSize = 13.sp
        )
    }
}

@Composable
fun ScheduleScreen() {
    val ctx = LocalContext.current
    var slots by remember { mutableStateOf(Store.loadSlots(ctx)) }
    var edit by remember { mutableStateOf<Pair<Long, Int?>?>(null) }
    val today = LocalDate.now().dayOfWeek.value

    fun commit(new: List<Slot>) {
        slots = new
        Store.saveSlots(ctx, new)
        Alarms.reschedule(ctx)
    }

    val next = Alarms.nextTrigger(slots)
    Column(Modifier.padding(horizontal = 12.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (next != null) "Siguiente: ${next.time} ${next.titles.joinToString(", ")}" else "Sin alarmas activas",
                color = Mut, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Button(
                onClick = {
                    val s = Slot(System.currentTimeMillis(), "12:00", emptyMap())
                    commit(slots + s)
                    edit = Pair(s.id, null)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("+") }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Spacer(Modifier.width(48.dp))
            "LMXJVSD".forEachIndexed { i, ch ->
                Text(
                    ch.toString(), Modifier.weight(1f),
                    color = if (i + 1 == today) Red else Mut, textAlign = TextAlign.Center, fontSize = 13.sp
                )
            }
        }
        slots.sortedBy { it.time }.forEach { s ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text(
                    s.time,
                    modifier = Modifier.width(48.dp).clickable { edit = Pair(s.id, null) }.align(Alignment.CenterVertically),
                    color = Tx, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
                )
                for (d in 1..7) {
                    val c = s.cells[d]
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier.weight(1f).padding(2.dp).height(54.dp)
                            .clip(shape).background(S1)
                            .border(1.dp, if (c != null) Mut else Ln, shape)
                            .clickable { edit = Pair(s.id, d) }
                            .padding(4.dp)
                    ) {
                        if (c != null) {
                            Text(c.name, color = Tx, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            if (c.alarm) Box(Modifier.align(Alignment.TopEnd).size(5.dp).clip(CircleShape).background(Red))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    edit?.let { e ->
        val rid = e.first
        val day = e.second
        val slot = slots.firstOrNull { it.id == rid }
        if (slot != null) {
            EditDialog(
                slot, day,
                onSave = { time, name, alarm ->
                    commit(slots.map { s ->
                        if (s.id != rid) s else {
                            val cells = s.cells.toMutableMap()
                            if (day != null) {
                                if (name.isBlank()) cells.remove(day) else cells[day] = Cell(name, alarm)
                            }
                            s.copy(time = time, cells = cells)
                        }
                    })
                    edit = null
                },
                onDelete = {
                    commit(slots.filter { it.id != rid })
                    edit = null
                },
                onDismiss = { edit = null }
            )
        }
    }
}

@Composable
fun EditDialog(
    slot: Slot,
    day: Int?,
    onSave: (String, String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var time by remember { mutableStateOf(slot.time) }
    var name by remember { mutableStateOf(slot.cells[day]?.name ?: "") }
    var alarm by remember { mutableStateOf(slot.cells[day]?.alarm ?: true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = S1,
        title = { Text(if (day == null) "Hora de la fila" else dayNames[day - 1], color = Tx) },
        text = {
            Column {
                if (day != null) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Actividad") }, singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
                TextButton(onClick = {
                    val t = LocalTime.parse(time)
                    TimePickerDialog(ctx, { _, h, m -> time = "%02d:%02d".format(h, m) }, t.hour, t.minute, true).show()
                }) { Text("Hora: $time", color = Tx) }
                if (day != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Alarma", color = Tx, modifier = Modifier.weight(1f))
                        Switch(checked = alarm, onCheckedChange = { alarm = it })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(time, name.trim(), alarm) }) { Text("Guardar", color = Red) } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Quitar fila", color = Mut) } }
    )
}
