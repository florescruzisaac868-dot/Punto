package com.punto.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Cell(val name: String, val alarm: Boolean)

// cells: día de la semana (1 = lunes ... 7 = domingo) -> actividad
data class Slot(val id: Long, val time: String, val cells: Map<Int, Cell>)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("punto", Context.MODE_PRIVATE)

    fun loadSlots(c: Context): List<Slot> {
        val raw = prefs(c).getString("slots", null) ?: return defaults()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val co = o.getJSONObject("c")
            val cells = co.keys().asSequence().associate { k ->
                val x = co.getJSONObject(k)
                k.toInt() to Cell(x.getString("n"), x.getBoolean("a"))
            }
            Slot(o.getLong("id"), o.getString("t"), cells)
        }
    }

    fun saveSlots(c: Context, slots: List<Slot>) {
        val arr = JSONArray()
        slots.forEach { s ->
            val co = JSONObject()
            s.cells.forEach { (d, cell) ->
                co.put(d.toString(), JSONObject().put("n", cell.name).put("a", cell.alarm))
            }
            arr.put(JSONObject().put("id", s.id).put("t", s.time).put("c", co))
        }
        prefs(c).edit().putString("slots", arr.toString()).apply()
    }

    private fun defaults() = listOf(
        Slot(1, "07:00", (1..5).associateWith { Cell("Despertar", true) }),
        Slot(2, "08:00", (1..5).associateWith { Cell("Clases", true) }),
        Slot(3, "17:00", (1..7).associateWith { Cell("Practicar cubo", false) })
    )
}
