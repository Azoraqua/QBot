package com.azoraqua.qbot.listener

import com.azoraqua.qbot.IMAGE_PATTERN
import com.azoraqua.qbot.Main
import com.azoraqua.qbot.isImage
import com.azoraqua.qbot.util.HastebinLogger
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.imageio.ImageIO

class EventManager(val main: Main) : ListenerAdapter() {
    internal val THREAD_POOL = Executors.newCachedThreadPool()
    internal val LOGGER = HastebinLogger()
    internal val EXCEPTION_PATTERN = Pattern.compile("((.*)(\\.))?(.*)(Exception|Error)", Pattern.MULTILINE)
    internal lateinit var EVERYONE_ROLE: Role
    internal lateinit var MUTED_ROLE: Role
    internal lateinit var RULES_CHANNEL: TextChannel
    internal val RULE_MESSAGE_ID = 723865660622372915
    internal val OK_EMOJI = "✅"
    internal val NOPE_EMOJI = "❌"

    override fun onReady(event: ReadyEvent) {
        println("Bot ${main.api.selfUser.asTag} is ready.")

        EVERYONE_ROLE = main.api.getRoleById(464296649330655244)!!
        MUTED_ROLE = main.api.getRoleById(627957483423399937)!!
        RULES_CHANNEL = main.api.getGuildChannelById(723864929744060497) as TextChannel
        RULES_CHANNEL.clearReactionsById(RULE_MESSAGE_ID).queue()
        RULES_CHANNEL.addReactionById(RULE_MESSAGE_ID, OK_EMOJI).queue()
        RULES_CHANNEL.addReactionById(RULE_MESSAGE_ID, NOPE_EMOJI).queue()

//        RULES_CHANNEL.guild.channels.forEach {ch ->
//            ch.members.forEach { m ->
//                ch.createPermissionOverride(m)
//                    .deny(Permission.VIEW_CHANNEL)
//            }
//        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user == main.api.selfUser) {
            return
        }

//        if (event.reactionEmote.emoji == OK_EMOJI) {
//            event.guild.addRoleToMember(event.userId, MEMBER_ROLE).queue()
//        } else if (event.reactionEmote.emoji == NOPE_EMOJI) {
//            event.guild.addRoleToMember(event.userId, MUTED_ROLE).queue()
//        }

//        event.reaction.removeReaction(event.user!!).queue()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == main.api.selfUser) {
            return
        }

        val text: String

        when {
            event.message.attachments.isNotEmpty() -> {
                val file = event.message.attachments[0].downloadToFile().get()

                text = if (IMAGE_PATTERN.asMatchPredicate().test(file.name)) {
                    main.tesseract.doOCR(file)
                } else {
                    Files.readString(file.toPath(), Charsets.UTF_8)
                }

                Files.delete(file.absoluteFile.toPath())
            }
            event.message.isImage() -> {
                text = main.tesseract.doOCR(ImageIO.read(URL(event.message.contentRaw)))
            }
            else -> {
                text = event.message.contentRaw
            }
        }

        if (this.isException(text) || this.hasPrefix(text, "[WARN]", "[ERROR]")) {
            val url = CompletableFuture.supplyAsync {
                LOGGER.log(text)
            }.get()

            CompletableFuture.runAsync {
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
    }

    private fun isException(text: String): Boolean {
        return EXCEPTION_PATTERN.matcher(text).find()
            && !text.contains("~")
    }

    private fun hasPrefix(text: String, vararg prefixes: String): Boolean {
        return Arrays.stream(prefixes).anyMatch {
            text.contains(it, true)
                && !text.startsWith("~")
        }
    }
}