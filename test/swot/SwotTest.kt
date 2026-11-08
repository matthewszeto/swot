package swot

import junit.framework.TestCase
import java.util.Locale

class SwotTest : TestCase() {
    fun testSwot() {
        assertEquals (true , isAcademic("lreilly@stanford.edu"))          
        assertEquals (true , isAcademic("LREILLY@STANFORD.EDU"))          
        assertEquals (true , isAcademic("Lreilly@Stanford.Edu"))          
        assertEquals (true , isAcademic("lreilly@slac.stanford.edu"))     
        assertEquals (true , isAcademic("lreilly@strath.ac.uk"))          
        assertEquals (true , isAcademic("lreilly@soft-eng.strath.ac.uk")) 
        assertEquals (true , isAcademic("lee@ugr.es"))                    
        assertEquals (true , isAcademic("lee@uottawa.ca"))                
        assertEquals (true , isAcademic("lee@mother.edu.ru"))             
        assertEquals (true , isAcademic("lee@ucy.ac.cy"))                 
        assertEquals (false, isAcademic("lee@leerilly.net"))              
        assertEquals (false, isAcademic("lee@gmail.com"))                 
        assertEquals (false, isAcademic("lee@stanford.edu.com"))          
        assertEquals (false, isAcademic("lee@strath.ac.uk.com"))          
        assertEquals (true , isAcademic("stanford.edu"))                  
        assertEquals (true , isAcademic("slac.stanford.edu"))             
        assertEquals (true , isAcademic("www.stanford.edu"))              
        assertEquals (true , isAcademic("http://www.stanford.edu"))       
        assertEquals (true , isAcademic("http://www.stanford.edu:9393"))  
        assertEquals (true , isAcademic("strath.ac.uk"))                  
        assertEquals (true , isAcademic("soft-eng.strath.ac.uk"))         
        assertEquals (true , isAcademic("ugr.es"))                        
        assertEquals (true , isAcademic("uottawa.ca"))                    
        assertEquals (true , isAcademic("mother.edu.ru"))                 
        assertEquals (true , isAcademic("ucy.ac.cy"))                     
        assertEquals (false, isAcademic("leerilly.net"))                  
        assertEquals (false, isAcademic("gmail.com"))                     
        assertEquals (false, isAcademic("stanford.edu.com"))              
        assertEquals (false, isAcademic("strath.ac.uk.com"))              
        assertEquals (false, isAcademic(""))
        assertEquals (false, isAcademic("the"))                           
        assertEquals (true , isAcademic(" stanford.edu"))                 
        assertEquals (true , isAcademic("lee@strath.ac.uk "))             
        assertEquals (false, isAcademic(" gmail.com "))                   
        assertEquals (true , isAcademic("lee@stud.uni-corvinus.hu"))      
        assertEquals (true , isAcademic("lee@harvard.edu"))               
        assertEquals (true , isAcademic("lee@mail.harvard.edu"))

        assertEquals(false, isAcademic("imposter@si.edu"))
        assertEquals(false, isAcademic("lee@mdu.edu.rs"))

        // Iran sanctions are lifted
        assertEquals(true, isAcademic("lee@acmt.ac.ir"))
    }

    fun testSchoolNames() {
        assertEquals(listOf("University of Strathclyde"), findSchoolNames("lreilly@cs.strath.ac.uk"))
        assertEquals("BRG Fadingerstraße Linz, Austria", findSchoolNames("lreilly@fadi.at").single())
        assertEquals("St. Petersburg State University", findSchoolNames("max@spbu.ru ").single())
        assertEquals(0, findSchoolNames("foo@shop.com").size)
    }

    fun testHostParsing() {
        assertEquals(true, isAcademic("http://www.stanford.edu/about"))
        assertEquals(true, isAcademic("http://www.stanford.edu:9393/path?q=1#section"))
        assertEquals(true, isAcademic("http://user:pass@www.stanford.edu/about"))
        assertEquals(true, isAcademic("Lee Reilly <lee@stanford.edu>"))
        assertEquals(true, isAcademic("stanford.edu."))
        assertEquals(true, isAcademic("mailto:lee@stanford.edu"))
        assertEquals(false, isAcademic("http://notacademic.com/?x=@stanford.edu"))
    }

    fun testCaseFoldingIgnoresDefaultLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(false, isAcademic("IMPOSTER@SI.EDU"))
            assertEquals(true, isAcademic("LEE@FADI.AT"))
            assertEquals(true, isAcademic("LEE@ACMT.AC.IR"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    fun testAlumniMailIsNotAcademic() {
        assertEquals(false, isAcademic("john.smith@alumni.stanford.edu"))
        assertEquals(true, isAcademic("john.smith@stanford.edu"))
        assertEquals(true, isAcademic("user@esat-alumni.com"))
        assertFalse(findSchoolNames("user@esat-alumni.com").single().startsWith("\uFEFF"))
    }

    fun testNamesKeepFileOrderAndSkipMarkers() {
        assertEquals(listOf("Tohoku University"), findSchoolNames("student@tohoku.ac.jp"))
        assertEquals(listOf("Europa-Universität Viadrina Frankfurt (Oder)"), findSchoolNames("user@europa-uni.de"))
        assertEquals(listOf("Fundación Universitaria Konrad Lorenz"), findSchoolNames("user@konradlorenz.edu.co"))
        assertEquals(false, isAcademic("student@bcps.org"))
        assertEquals(false, isAcademic("student@kansaigaidai.jp"))
        assertEquals(0, findSchoolNames("student@bcps.org").size)
    }

    fun testMisnamedDomainFilesResolve() {
        assertTrue(findSchoolNames("pupil@ksw.school").contains("King's School Worcester"))
        assertEquals(listOf("emlyon business school"), findSchoolNames("student@edu.emlyon.com"))
        assertEquals(
            listOf("College Of Engineering and Technology, Bhubaneswar"),
            findSchoolNames("student@cet.edu.in")
        )
        assertEquals(
            listOf("Ioffe Physical-Technical Institute of the Russian Academy of Sciences"),
            findSchoolNames("user@ioffe.ru")
        )
    }
}
