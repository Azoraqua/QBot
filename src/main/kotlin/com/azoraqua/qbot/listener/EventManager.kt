package com.azoraqua.qbot.listener

import com.azoraqua.qbot.Main
import com.azoraqua.qbot.isImage
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URL
import java.nio.file.Files
import java.util.regex.Pattern
import javax.imageio.ImageIO

class EventManager(val main: Main) : ListenerAdapter() {
    internal val EXCEPTION_PATTERN = Pattern.compile("((.*)(\\.))?(.*)(Exception|Error)", Pattern.MULTILINE)

    override fun onReady(event: ReadyEvent) {
        println("Bot ${main.api.selfUser.asTag} is ready.")
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == main.api.selfUser) {
            return
        }

        var text: String

        if (event.message.attachments.isNotEmpty()) {
            event.message.channel.sendTyping().queue()

            val file = event.message.attachments[0].downloadToFile().get()
            text = main.tesseract.doOCR(file)

            Files.delete(file.absoluteFile.toPath())
        } else if (event.message.isImage()) {
            event.message.channel.sendTyping().queue()

            text = main.tesseract.doOCR(ImageIO.read(URL(event.message.contentRaw)))
        } else {
            text = event.message.contentRaw
        }

        if (this.isException(text)) {
            event.message.addReaction("\uD83D\uDC40").queue {
                event.message.channel.sendMessage("I have detected that you have sent an error. One of our staff will help you with it anytime soon. Please wait a moment.")
                    .queue()
            }
        }
    }

    private fun isException(text: String): Boolean {
        return EXCEPTION_PATTERN.matcher(text).find()
    }
}