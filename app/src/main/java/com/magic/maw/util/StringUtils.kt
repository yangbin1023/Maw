package com.magic.maw.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

fun String.isHtml(): Boolean {
    val doc: Document = Jsoup.parse(this)
    return doc.body().childrenSize() > 0
}

fun String.isVerifyHtml(): Boolean {
    return isHtml() && contains("<title>Just a moment...</title>")
}

fun String.isErrorHtml(): Boolean {
    return isHtml() && contains("<title>Error")
}

fun String.isJsonStr(): Boolean {
    try {
        json.parseToJsonElement(this)
        return true
    } catch (_: Exception) {
    }
    return false
}
