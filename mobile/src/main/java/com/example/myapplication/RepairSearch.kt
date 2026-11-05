package com.example.myapplication

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RepairShop(val name: String, val latitude: Double, val longitude: Double)

fun parseRepairShops(json: String): List<RepairShop> {
    val array = JSONObject(json).getJSONArray("elements")
    return (0 until array.length()).mapNotNull { i ->
        val o = array.getJSONObject(i)
        val tags = o.optJSONObject("tags") ?: return@mapNotNull null
        val name = tags.optString("name").trim()
        val center = o.optJSONObject("center") ?: o
        val lat = center.optDouble("lat", Double.NaN)
        val lon = center.optDouble("lon", Double.NaN)
        if (name.isBlank() || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) null
        else RepairShop(name.take(120), lat, lon)
    }.distinctBy { Triple(it.name, it.latitude, it.longitude) }.take(10)
}

fun searchRepairShops(latitude: Double, longitude: Double): List<RepairShop> {
    require(latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0)
    val query = "[out:json][timeout:15];nwr[shop=car_repair](around:5000,$latitude,$longitude);out center 30;"
    val connection = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 10000
        connection.readTimeout = 20000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        connection.setRequestProperty("User-Agent", "PleosCarCarePrototype/1.0")
        val body = "data=" + URLEncoder.encode(query, "UTF-8")
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        check(connection.responseCode == 200) { "정비소 정보 제공 서버에 연결하지 못했습니다 (${connection.responseCode})." }
        return parseRepairShops(connection.inputStream.bufferedReader().use { it.readText() })
    } finally { connection.disconnect() }
}
