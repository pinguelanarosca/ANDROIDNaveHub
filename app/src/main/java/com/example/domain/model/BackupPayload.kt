package com.example.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val platforms: List<Platform>,
    val accounts: List<Account>,
    val preferences: Map<String, String>
) {
    fun toJsonString(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("timestamp", timestamp)

        val platformsArray = JSONArray()
        platforms.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("defaultUrl", p.defaultUrl)
            obj.put("accentColorHex", p.accentColorHex)
            obj.put("isCustom", p.isCustom)
            platformsArray.put(obj)
        }
        root.put("platforms", platformsArray)

        val accountsArray = JSONArray()
        accounts.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("platformId", a.platformId)
            obj.put("name", a.name)
            obj.put("currentUrl", a.currentUrl)
            obj.put("createdAt", a.createdAt)
            accountsArray.put(obj)
        }
        root.put("accounts", accountsArray)

        val prefsObj = JSONObject()
        preferences.forEach { (k, v) ->
            prefsObj.put(k, v)
        }
        root.put("preferences", prefsObj)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(json: String): BackupPayload {
            val root = JSONObject(json)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val platformsList = mutableListOf<Platform>()
            val platformsArray = root.optJSONArray("platforms")
            if (platformsArray != null) {
                for (i in 0 until platformsArray.length()) {
                    val obj = platformsArray.getJSONObject(i)
                    platformsList.add(
                        Platform(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            defaultUrl = obj.getString("defaultUrl"),
                            accentColorHex = obj.optString("accentColorHex", "#00E676"),
                            isCustom = obj.optBoolean("isCustom", false)
                        )
                    )
                }
            }

            val accountsList = mutableListOf<Account>()
            val accountsArray = root.optJSONArray("accounts")
            if (accountsArray != null) {
                for (i in 0 until accountsArray.length()) {
                    val obj = accountsArray.getJSONObject(i)
                    accountsList.add(
                        Account(
                            id = obj.getString("id"),
                            platformId = obj.getString("platformId"),
                            name = obj.getString("name"),
                            currentUrl = obj.getString("currentUrl"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val prefsMap = mutableMapOf<String, String>()
            val prefsObj = root.optJSONObject("preferences")
            if (prefsObj != null) {
                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    prefsMap[key] = prefsObj.getString(key)
                }
            }

            return BackupPayload(
                version = version,
                timestamp = timestamp,
                platforms = platformsList,
                accounts = accountsList,
                preferences = prefsMap
            )
        }
    }
}
