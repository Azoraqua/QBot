package com.azoraqua.qbot

import net.dv8tion.jda.api.entities.Message
import java.util.regex.Pattern

internal val URL_PATTERN = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)")
internal val IMAGE_PATTERN = Pattern.compile("([a-zA-Z0-9\\-_.]+(\\.))?(jpe?g|png|gif|bmp|webp)")

fun Message.isURL(): Boolean = URL_PATTERN.asMatchPredicate().test(this.contentRaw)
fun Message.isImage(): Boolean = IMAGE_PATTERN.asMatchPredicate().test(this.contentRaw)