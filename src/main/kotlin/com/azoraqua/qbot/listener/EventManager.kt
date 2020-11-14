package com.azoraqua.qbot.listener

import com.azoraqua.qbot.IMAGE_PATTERN
import com.azoraqua.qbot.Main
import com.azoraqua.qbot.isImage
import com.azoraqua.qbot.util.HastebinLogger
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URL
import java.nio.file.Files
import java.util.*
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

            if (IMAGE_PATTERN.asMatchPredicate().test(file.name)) {
                text = main.tesseract.doOCR(file)
            } else {
                text = Files.readString(file.toPath(), Charsets.UTF_8)
            }

            Files.delete(file.absoluteFile.toPath())
        } else if (event.message.isImage()) {
            text = main.tesseract.doOCR(ImageIO.read(URL(event.message.contentRaw)))
        } else {
            text = event.message.contentRaw
        }

        if (this.isException(text) || this.hasPrefix(text, "[WARN]", "[ERROR]")) {
            val url = CompletableFuture.supplyAsync {
                LOGGER.log(text)
            }.get()

            event.message.addReaction("\uD83D\uDC40").queue {
                event.message.delete().queueAfter(5, TimeUnit.SECONDS) {
                    event.message.channel.sendTyping().queue {
                        event.message.channel.sendMessage(url).queueAfter(3, TimeUnit.SECONDS) { m ->
                            m.channel.sendMessage("${event.message.author.asMention} Please wait a moment. One of our staff will be with you anytime soon.").queue()
                        }
                    }
                }
            }
        }
    }

    private fun isException(text: String): Boolean {
        return EXCEPTION_PATTERN.matcher(text).find()
            && !text.contains("~")
    }

    private fun hasPrefix(text: String, vararg prefixes: String): Boolean {
        return Arrays.stream(prefixes).anyMatch { text.contains(it, true)
            && !text.startsWith("~")
        }
    }
}