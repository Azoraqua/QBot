package com.azoraqua.qbot

import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

data class Configuration(
    val bot: BotInfo,
    val responses: Array<out BotResponse>
)

data class BotInfo(
    val token: String,
    val status: OnlineStatus,
    val activity: BotActivity
)

data class BotActivity(
    val type: Activity.ActivityType,
    val content: String
)

data class BotResponse(
    val trigger: String,
    val action: String
)