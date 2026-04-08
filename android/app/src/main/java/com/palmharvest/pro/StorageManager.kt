package com.palmharvest.pro

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HarvestRecord(
    val id: String,
    val harvesterUid: String,
    val harvesterName: String,
    val collectionPoint: String,
    val bunchCount: Int,
    val photoUrl: String,
    val timestamp: Long,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class StorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("palm_harvest_prefs", Context.MODE_PRIVATE)

    fun saveRecord(record: HarvestRecord) {
        val records = getRecords().toMutableList()
        val index = records.indexOfFirst { it.id == record.id }
        if (index != -1) {
            records[index] = record
        } else {
            records.add(0, record) // Add to beginning
        }
        saveRecords(records)
    }

    fun getRecords(): List<HarvestRecord> {
        val jsonString = prefs.getString("records", "[]") ?: "[]"
        val records = mutableListOf<HarvestRecord>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                records.add(
                    HarvestRecord(
                        id = obj.getString("id"),
                        harvesterUid = obj.getString("harvesterUid"),
                        harvesterName = obj.getString("harvesterName"),
                        collectionPoint = obj.getString("collectionPoint"),
                        bunchCount = obj.getInt("bunchCount"),
                        photoUrl = obj.getString("photoUrl"),
                        timestamp = obj.getLong("timestamp"),
                        latitude = if (obj.has("latitude")) obj.getDouble("latitude") else null,
                        longitude = if (obj.has("longitude")) obj.getDouble("longitude") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return records
    }

    fun saveRecords(records: List<HarvestRecord>) {
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("harvesterUid", record.harvesterUid)
                put("harvesterName", record.harvesterName)
                put("collectionPoint", record.collectionPoint)
                put("bunchCount", record.bunchCount)
                put("photoUrl", record.photoUrl)
                put("timestamp", record.timestamp)
                record.latitude?.let { put("latitude", it) }
                record.longitude?.let { put("longitude", it) }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("records", jsonArray.toString()).apply()
    }

    fun deleteRecord(id: String) {
        val records = getRecords().filter { it.id != id }
        saveRecords(records)
    }
    
    fun clearAll() {
        prefs.edit().remove("records").apply()
    }
}
