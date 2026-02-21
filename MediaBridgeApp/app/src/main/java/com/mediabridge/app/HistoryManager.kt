package com.mediabridge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("media_bridge_history", Context.MODE_PRIVATE)

    fun saveItem(item: ConversionItem) {

        val list = getItems().toMutableList()
        list.add(0, item)

        val jsonArray = JSONArray()

        list.forEach {
            val obj = JSONObject()
            obj.put("jobId", it.jobId)
            obj.put("fileName", it.fileName)
            obj.put("fileUri", it.fileUri)
            obj.put("date", it.date)
            jsonArray.put(obj)
        }

        prefs.edit().putString("history", jsonArray.toString()).apply()
    }

    fun getItems(): List<ConversionItem> {

        val jsonString = prefs.getString("history", null) ?: return emptyList()

        val jsonArray = JSONArray(jsonString)
        val list = mutableListOf<ConversionItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            list.add(
                ConversionItem(
                    obj.getString("jobId"),
                    obj.getString("fileName"),
                    obj.getString("fileUri"),
                    obj.getLong("date")
                )
            )
        }

        return list
    }
}
