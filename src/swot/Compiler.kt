package swot

import java.io.File
import java.util.Locale

/**
 * @author max
 */

fun main(args: Array<String>) {
    val root = File("lib/domains")
    File("out/artifacts").mkdirs()
    File("out/artifacts/swot.txt").writeText(compileSwot(root))
}

internal fun compileSwot(root: File): String {
    val blacklist = readNameList(File(root, "blacklist.txt")).toHashSet()
    val domains = readNameList(File(root, "tlds.txt")).toHashSet()

    val files = root.walkTopDown()
        .filter { it.isFile && hasTxtSuffix(it.name) }
        .map { it.toRelativeString(root).replace('\\', '/') }
        .filter { !isMetaList(it) }
        .sortedWith(compareBy({ it.count { ch -> ch == '/' } }, { it.lowercase(Locale.ROOT) }))

    for (relative in files) {
        val parts = relative.dropLast(4).lowercase(Locale.ROOT).split('/')
        if (!checkSet(blacklist, parts) && !checkSet(domains, parts)) {
            domains.add(parts.asReversed().joinToString("."))
        }
    }

    val denied = blacklist.map { "-$it" }.sorted()
    val allowed = domains.sorted()
    return denied.joinToString("\n") + "\n" + allowed.joinToString("\n")
}

private fun readNameList(file: File): List<String> {
    return file.readLines(Charsets.UTF_8).map { it.trim() }.filter { it.isNotEmpty() }
}

private fun hasTxtSuffix(name: String): Boolean {
    return name.length >= 4 && name.regionMatches(name.length - 4, ".txt", 0, 4, ignoreCase = true)
}

private fun isMetaList(relative: String): Boolean {
    val name = relative.lowercase(Locale.ROOT)
    return name == "blacklist.txt" || name == "tlds.txt"
}
