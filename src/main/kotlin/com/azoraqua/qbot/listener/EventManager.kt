package com.azoraqua.qbot.listener

import com.azoraqua.qbot.Main
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class EventManager(val main: Main) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        println("Bot ${main.api.selfUser.asTag} is ready.")
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        event.message.addReaction("\uD83D\uDC40").queue()
    }
}