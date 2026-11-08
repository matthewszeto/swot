package swot

import java.util.Locale

fun isAcademic(email: String): Boolean {
    val parts = domainParts(email)
    // Alumni mailboxes are not eligible. See CONTRIBUTING.md.
    if (parts.any { it == "alumni" }) return false
    return !isBlacklisted(parts) && (isUnderTLD(parts) || findSchoolNames(parts).isNotEmpty())
}

fun findSchoolNames(emailOrDomain: String): List<String> {
    return findSchoolNames(domainParts(emailOrDomain))
}

fun isUnderTLD(parts: List<String>): Boolean {
    return checkSet(Resources.tlds, parts)
}

fun isBlacklisted(parts: List<String>): Boolean {
    return checkSet(Resources.blackList, parts)
}

private object Resources {
    val tlds = readLines("/tlds.txt")?.toHashSet() ?: error("Cannot find /tlds.txt")
    val blackList = readLines("/blacklist.txt")?.toHashSet() ?: error("Cannot find /blacklist.txt")

    fun readLines(resource: String): List<String>? {
        val stream = javaClass.getResourceAsStream(resource) ?: return null
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.lineSequence()
                .map { it.removePrefix("\uFEFF").trim() }
                .filter { it.isNotEmpty() && it != ".group" }
                .distinct()
                .toList()
        }
    }
}

private fun findSchoolNames(parts: List<String>): List<String> {
    val resourcePath = StringBuilder()
    for (part in parts) {
        resourcePath.append('/').append(part)
        val school = Resources.readLines("$resourcePath.txt")
        if (school != null) {
            return school
        }
    }
    return emptyList()
}

internal fun domainParts(emailOrDomain: String): List<String> {
    var value = emailOrDomain.trim().lowercase(Locale.ROOT)
    val open = value.lastIndexOf('<')
    val close = value.lastIndexOf('>')
    if (open >= 0 && close > open) {
        value = value.substring(open + 1, close).trim()
    }
    value = value.removePrefix("mailto:")
    val host = if (value.contains("://")) {
        value.substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfter('@')
            .substringBefore(':')
    } else {
        value.substringAfter('@')
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .substringBefore(' ')
    }.trim()
    val withoutRootDot = if (host.endsWith(".")) host.dropLast(1) else host
    return withoutRootDot.split('.').reversed()
}

internal fun checkSet(set: Set<String>, parts: List<String>): Boolean {
    val subj = StringBuilder()
    for (part in parts) {
        subj.insert(0, part)
        if (set.contains(subj.toString())) return true
        subj.insert(0, '.')
    }
    return false
}
