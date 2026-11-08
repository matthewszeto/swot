package swot

import junit.framework.TestCase
import java.io.File
import java.nio.file.Files

class CompilerTest : TestCase() {
    fun testCompileEmitsAcademicSuffixesOnly() {
        val root = Files.createTempDirectory("swot-compile").toFile()
        try {
            File(root, "tlds.txt").writeText("edu\nac.uk\n")
            File(root, "blacklist.txt").writeText("si.edu\n")
            File(root, "edu").mkdirs()
            File(root, "edu/stanford.txt").writeText("Stanford University\n")
            File(root, "edu/si.txt").writeText("Smithsonian\n")
            File(root, "edu/DBU.txt").writeText("Dallas Baptist University\n")
            File(root, "at/fadi").mkdirs()
            File(root, "at/fadi/students.txt").writeText("Students\n")
            File(root, "at/fadi.txt").writeText("BRG Fadingerstraße Linz, Austria\n")
            File(root, "school").mkdirs()
            File(root, "school/KSW.txt").writeText("King's School Worcester\n")
            File(root, "netocentre").writeText("not a domain file\n")
            File(root, "notes.md").writeText("nope\n")

            val lines = compileSwot(root).split('\n')

            assertTrue(lines.contains("edu"))
            assertTrue(lines.contains("ac.uk"))
            assertTrue(lines.contains("-si.edu"))
            assertTrue(lines.contains("fadi.at"))
            assertTrue(lines.contains("ksw.school"))
            assertFalse(lines.contains("si.edu"))
            assertFalse(lines.contains("stanford.edu"))
            assertFalse(lines.contains("dbu.edu"))
            assertFalse(lines.contains("students.fadi.at"))
            assertFalse(lines.contains("blacklist"))
            assertFalse(lines.contains("tlds"))
            assertFalse(lines.contains("KSW.school"))
            assertFalse(lines.any { it.contains("netocentre") || it.contains("notes") })
        } finally {
            root.deleteRecursively()
        }
    }

    fun testCheckedInTreeOmitsBookkeepingDomains() {
        val lines = compileSwot(File("lib/domains")).split('\n').toSet()
        assertFalse(lines.contains("blacklist"))
        assertFalse(lines.contains("tlds"))
        assertFalse(lines.contains("dtmik.tj.tj"))
        assertFalse(lines.contains("netocentre.fr"))
        assertFalse(lines.contains("KSW.school"))
        assertTrue(lines.contains("-si.edu"))
        assertTrue(lines.contains("edu"))
        assertTrue(lines.contains("ksw.school"))
        assertTrue(lines.contains("edu.emlyon.com"))
        assertTrue(lines.contains("fadi.at"))
        assertFalse(lines.contains("stanford.edu"))
    }
}
