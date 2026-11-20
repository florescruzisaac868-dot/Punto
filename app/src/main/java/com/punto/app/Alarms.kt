package com.punto.app

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class Next(val at: Long, val time: String, val titles: List<String>)

object Alarms {
    private const val CHANNEL = "alarmas"

    // Calcula la próxima alarma semanal entre todas las celdas con alarma activa.
    fun nextTrigger(slots: List<Slot>, now: LocalDateTime = LocalDateTime.now()): Next? {
        val hits = ArrayList<Pair<LocalDateTime, String>>()
        for (s in slots) {
            val t = runCatching { LocalTime.parse(s.time) }.getOrNull() ?: continue
            for ((d, cell) in s.cells) {
                if (!cell.alarm || cell.name.isBlank()) continue
                val date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(d)))
                var dt = LocalDateTime.of(date, t)
                if (!dt.isAfter(now)) dt = dt.plusWeeks(1)
                hits.add(dt to cell.name)
            }
        }
        val first = hits.minOfOrNull { it.first } ?: return null
        return Next(
            first.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            first.toLocalTime().toString().take(5),
            hits.filter { it.first == first }.map { it.second }
        )
    }

    // Programa en el sistema la próxima alarma. Se llama al editar, al abrir, al sonar y al reiniciar.
    fun reschedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = nextTrigger(Store.loadSlots(c))
        val intent = Intent(c, AlarmReceiver::class.java)
        if (next == null) {
            val pi = PendingIntent.getBroadcast(c, 1, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pi != null) am.cancel(pi)
            return
        }
        intent.putExtra("time", next.time)
        intent.putExtra("titles", next.titles.joinToString(", "))
        val pi = PendingIntent.getBroadcast(c, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val show = PendingIntent.getActivity(c, 2, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(next.at, show), pi)
        } catch (e: SecurityException) {
            // sin permiso de alarmas exactas: no cierres la app
        }
    }

    fun ring(c: Context, time: String, text: String) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val channel = NotificationChannel(CHANNEL, "Alarmas", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        nm.createNotificationChannel(channel)
        val open = PendingIntent.getActivity(c, 3, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (time.isEmpty()) "Alarma" else time)
            .setContentText(text)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT // repite el sonido hasta que la quites
        nm.notify(System.currentTimeMillis().toInt(), n)
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Alarms.ring(c, i.getStringExtra("time") ?: "", i.getStringExtra("titles") ?: "Alarma")
        Alarms.reschedule(c)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Alarms.reschedule(c)
    }
}
