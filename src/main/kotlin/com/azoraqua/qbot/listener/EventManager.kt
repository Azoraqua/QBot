package com.azoraqua.qbot.listener

import com.azoraqua.qbot.Main
import com.azoraqua.qbot.isImage
import com.azoraqua.qbot.util.HastebinLogger
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.imageio.ImageIO

class EventManager(val main: Main) : ListenerAdapter() {
    internal val LOGGER = HastebinLogger()
    internal val EXCEPTION_PATTERN = Pattern.compile("((.*)(\\.))?(.*)(Exception|Error)", Pattern.MULTILINE)

    override fun onReady(event: ReadyEvent) {
        println("Bot ${main.api.selfUser.asTag} is ready.")
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == main.api.selfUser) {
            return
        }

        val text: String

        if (event.message.attachments.isNotEmpty()) {
            val file = event.message.attachments[0].downloadToFile().get()
            text = main.tesseract.doOCR(file)

            Files.delete(file.absoluteFile.toPath())
        } else if (event.message.isImage()) {
            text = main.tesseract.doOCR(ImageIO.read(URL(event.message.contentRaw)))
        } else {
            text = event.message.contentRaw
        }

        if (this.isException(text)) {
            val url = CompletableFuture.supplyAsync {
                LOGGER.log(text)
            }.get()

            event.message.addReaction("\uD83D\uDC40").queue {
                event.message.delete().queueAfter(5, TimeUnit.SECONDS) {
                    event.message.channel.sendTyping().queue {
                        event.message.channel.sendMessage(url).queueAfter(3, TimeUnit.SECONDS) { m ->
                            m.channel.sendMessage("Please wait a moment. One of our staff will help you with it anytime soon.").queue()
                        }
                    }
                }
            }
        }
    }

    private fun isException(text: String): Boolean {
        return EXCEPTION_PATTERN.matcher(text).find()
    }
}