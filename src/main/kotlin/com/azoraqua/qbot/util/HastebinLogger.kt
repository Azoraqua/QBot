package com.azoraqua.qbot.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.util.stream.Collectors

class HastebinLogger {
    private val BASE_URL = "https://hastebin.com"

    fun log(text: String): String {
        OkHttpClient().newCall(
            Request.Builder()
                .url("${BASE_URL}/documents")
                .post(text.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
        ).execute().use {
            return "$BASE_URL/${
                Gson().fromJson(it.body!!.string(), JsonObject::class.java).get("key").asString
            }"
        }
    }

    fun log(file: File, max: Long): String {
        BufferedReader(FileReader(file)).use {
            return this.log(it.lines().limit(max).collect(Collectors.joining(System.lineSeparator())))
        }
    }

    fun log(input: InputStream, max: Long): String {
        BufferedReader(InputStreamReader(input)).use {
            return this.log(it.lines().limit(max).collect(Collectors.joining(System.lineSeparator())))
        }
    }
}