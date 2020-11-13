package com.azoraqua.qbot

import com.azoraqua.qbot.listener.EventManager
import com.google.gson.Gson
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.sourceforge.tess4j.Tesseract
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import kotlin.concurrent.thread

class Main : Runnable {
    internal val GSON = Gson()
    internal lateinit var CONFIG_FILE: File
    internal lateinit var CONFIG: Configuration
    internal lateinit var api: JDA
    internal lateinit var tesseract: Tesseract

    fun start(args: Array<String>) {
        CONFIG_FILE = File("config.json")

        if (!CONFIG_FILE.exists()) {
            Files.copy(Main::class.java.classLoader.getResourceAsStream(CONFIG_FILE.name)!!, CONFIG_FILE.toPath())
        }

        CONFIG = FileReader(CONFIG_FILE).use { GSON.fromJson(it, Configuration::class.java) }

        thread {
            tesseract = Tesseract()
            tesseract.setDatapath("src/main/resources/data")
            tesseract.setLanguage("eng")
        }

        thread {
            api = JDABuilder.createDefault(CONFIG.bot.token)
                .setStatus(CONFIG.bot.status)
                .setActivity(Activity.of(CONFIG.bot.activity.type, CONFIG.bot.activity.content))
                .addEventListeners(EventManager(this))
                .build()
        }
    }

    override fun run() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            throw RuntimeException(e)
        }

        api.awaitReady()
    }
}

fun main(args: Array<String>) {
    val main = Main()

    thread {
        main.start(args)
    }
}

