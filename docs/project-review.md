# SWOT project review

Reviewed tree: `master` at `79487f8a` (2026-11-09). Line numbers below refer to that tree, before the follow-up fix commit.

SWOT decides whether an email address belongs to a school. JetBrains uses that decision for free student and teacher licenses. `lib/domains` holds about 11,200 institution files. `src/swot/Swot.kt` is the checker. `src/swot/Compiler.kt` flattens the same tree into `out/artifacts/swot.txt` for callers that do not want the file tree.

`isAcademic` lowercases the input, splits the host, and returns true when the host is not on the blacklist and either a suffix is listed in `tlds.txt` or some prefix of the reversed host has a domain file. `edu`, `ac.uk`, `edu.au`, and the other entries in `tlds.txt` therefore accept every subdomain unless the blacklist says otherwise. A domain file accepts that exact host and every subdomain of it.

The tests in `test/swot/SwotTest.kt` cover a handful of famous universities, two blacklist hits, and one deliberately wrong institution name. Nothing in the repository builds the project outside IntelliJ. The module still points at the IDE's `kotlin-runtime.jar`.

The defects below are the ones that change a license decision, poison the compiled list, or make an institution file invisible. Each one says whether the follow-up commit fixes it.

## 1. Uppercase mail bypasses the blacklist under a Turkish locale

Severity: high. Fix.

`domainParts` folds case with `String.toLowerCase()`, which uses the JVM default locale (`Swot.kt` lines 42–44). In Turkish, `I` lowercases to `ı` (U+0131), not `i`. Confirmed with a JDK 21 `toLowerCase(Locale.forLanguageTag("tr-TR"))`:

| Input | Turkish lowercase | `Locale.ROOT` |
| --- | --- | --- |
| `SI.EDU` | `sı.edu` | `si.edu` |
| `FADI.AT` | `fadı.at` | `fadi.at` |
| `AC.IR` | `ac.ır` | `ac.ir` |

`si.edu` is blacklisted because anyone can register a mailbox there. `IMPOSTER@SI.EDU` still takes the academic path under a Turkish locale: the folded label `sı` does not hit `si.edu`, and the blanket `edu` rule then accepts the address. The same fold rejects a real uppercase address whose academic rule contains `i`, such as `LEE@FADI.AT` and `LEE@ACMT.AC.IR`. The existing tests pass only because they use lowercase and the container locale is not Turkish.

Azeri and Lithuanian locales have their own `i` rules. Domain labels are ASCII here, so the fold has to be `Locale.ROOT`.

## 2. The URL parser trusts the wrong host

Severity: high. Fix.

The same function accepts full URLs. The tests include `http://www.stanford.edu` and `http://www.stanford.edu:9393`. The implementation does `substringAfter('@')`, then `substringAfter("://")`, then `substringBefore(':')`.

Two results follow.

A path, query, or fragment stays on the last label, so `http://www.stanford.edu/about` is not academic. The API already claims to understand that URL shape.

An `@` anywhere in the string is treated as the start of the host. `http://notacademic.com/?x=@stanford.edu` becomes `stanford.edu` and is accepted. The host of that URL is `notacademic.com`. Any caller that passes a URL, which the tests allow, can be pointed at an academic suffix placed in the query.

Angle-bracket addresses (`Lee <lee@stanford.edu>`) and a trailing DNS dot (`stanford.edu.`) miss for the same reason: the extra characters stay on a label.

## 3. Alumni mail is academic

Severity: high. Fix in `isAcademic`. The compiled suffix list cannot say the same thing.

`CONTRIBUTING.md` says alumni mail is excluded and gives `john.smith@alumni.stanford.edu` as the example. That address is academic today. `edu` is a blanket TLD, and `alumni.stanford.edu` is not on the blacklist. The blacklist only names two alumni hosts (`alumni.nottingham.ac.uk`, `alumni.cmu.edu`). Every other `alumni.` subdomain of `edu`, `ac.uk`, `edu.au`, and the rest of `tlds.txt` is accepted.

No domain file uses a label whose text is exactly `alumni`. The three similarly named files are different labels (`alumni-imie.fr`, `unswalumni.com`, `esat-alumni.com`) and should keep working.

`Compiler.kt` emits a flat suffix list: a leading `-` denies that suffix, every other line allows one. A line `-alumni` would deny the host `alumni`, not "any label equals alumni". Producing the alumni rule in `swot.txt` would mean inventing a syntax current readers do not have. The follow-up enforces the rule in `isAcademic` and leaves the artifact format alone. A consumer that only reads `swot.txt` still accepts `alumni.stanford.edu` via the `edu` line.

## 4. A blank line is enough to count as a school

Severity: high. Fix.

`readList` keeps every line, including `""`. `isAcademic` then treats a non-empty list as a hit, so a file that contains only a newline matches. Four files are in that state:

| File | What the checker does today | History |
| --- | --- | --- |
| `lib/domains/org/bcps.txt` | `bcps.org` is academic | Copied from an empty Konrad Lorenz file in `54d62f45` and never given a name. This is a school-district domain, which the README excludes. |
| `lib/domains/jp/kansaigaidai.txt` | `kansaigaidai.jp` is academic | `95198da2` added "Kansaigaidai University". `567a1aa4`, the same day, deleted the name and left a newline. |
| `lib/domains/de/europa-uni.txt` | `europa-uni.de` is academic because the only line is a space | `3e096113` says "Adding Europe University Viadrina" and committed a space. The real name already lives in `lib/domains/de/euv-frankfurt-o.txt`. |
| `lib/domains/co/edu/konradlorenz.txt` | Academic anyway, because `edu.co` is a blanket TLD | Created empty. `findSchoolNames` returns a blank string. |

The follow-up ignores blank lines. `bcps.org` and `kansaigaidai.jp` stop matching. `europa-uni.de` keeps matching because the file gains the name already used for Viadrina. `konradlorenz.edu.co` gains its institution name; the `edu.co` rule already accepted it.

## 5. The compiler lists its own bookkeeping as domains

Severity: high for anyone who ships `swot.txt`. Fix.

`Compiler.kt` walks every file under `lib/domains`. `blacklist.txt` and `tlds.txt` are files, so they become the domains `blacklist` and `tlds`. `removeSuffix(".txt")` does not touch other names, and nothing lowercases the path. A replay of that walk against this tree emits:

- `blacklist`
- `tlds`
- `dtmik.tj.tj`, from the empty stray file `lib/domains/tj/dtmik.tj` (the real institution is `lib/domains/tj/dtmik.txt`)
- `netocentre.fr`, from the extensionless file `lib/domains/fr/netocentre`
- `edu.emlyon.com`, from `lib/domains/com/emlyon/edu` (no `.txt`)
- mixed-case names such as `KSW.school`, `Ioffe.ru`, `fh-Kempten.de`

`isAcademic` sees none of those except by accident. It only opens `*.txt` resources, and it lowercases the query first, so `KSW.school` in the artifact and `school/KSW.txt` on disk both miss a lookup for `ksw.school`.

The walk is also ordered by `listFiles()`. A subdomain file visited before its parent is emitted even when the parent would have covered it. Sorting parents first makes the suffix list stable.

`lib/domains/fr/netocentre` is a regional workspace portal ("Environnement Numérique de Travail de la région Centre"), not an institution file in the repository's format. The follow-up does not give it a `.txt`, so `isAcademic` stays false and the compiler stops publishing `netocentre.fr`.

## 6. Institution files the checker cannot see

Severity: medium. Fix the ones that are clearly broken filenames. Leave the portal file in item 5.

Classpath lookup is case-sensitive. These files are not lowercase, so on Linux `isAcademic` misses the domain unless a blanket TLD already covers it. `edu/DBU.txt` is a second copy of `edu/dbu.txt` ("Dallas Baptist University"); both exist because the names differ by case.

Not covered by a blanket TLD, so the address is rejected today:

- `ksw.school`
- `prakticum.fi`
- `inecol.mx`
- `kingsworcester.org.uk`
- `ksw.org.uk`
- `ofy.org`
- `neighborhoodmusicschool.org`
- `fh-kempten.de`
- `kfh-freiburg.de`
- `spydebergskolen.no`
- `asker.vgs.no`
- `verdal.vgs.no`
- `sesame.com.tn`
- `c2kni.net`
- `ioffe.ru`

Covered by a TLD, so the license already succeeds and only `findSchoolNames` is wrong:

- `stu.upes.ac.in` (`ac.in`)
- `dbu.edu` (`edu`)
- `fondazionejac.edu` (`edu`)

Two more files use the wrong extension. `lib/domains/in/edu/cet.text` is "College Of Engineering and Technology, Bhubaneswar"; `edu.in` already accepts the domain, and the name does not resolve. `lib/domains/com/emlyon/edu` is "emlyon business school". The compiler already publishes `edu.emlyon.com`. The checker does not, because the file has no `.txt`. Renaming it makes `isAcademic("user@edu.emlyon.com")` true. That is a real eligibility change for one business-school domain the tree already tried to list.

`lib/domains/nl/ijburgcollege` is byte-for-byte the same as `lib/domains/nl/ijburgcollege.txt`. The extensionless copy is removed. `lib/domains/tj/dtmik.tj` contains only a newline and is removed so it cannot become `dtmik.tj.tj`.

## 7. School names are read unsafely, and 27 files are not UTF-8

Severity: medium. Fix.

`Resources.readList` (`Swot.kt` lines 24–26) does three things wrong:

- It decodes with the platform charset. The IntelliJ project sets UTF-8 (`encodings.xml`), and `findSchoolNames("lreilly@fadi.at")` expects `BRG Fadingerstraße Linz, Austria`. On a JVM whose default charset is not UTF-8, that assertion fails and every non-ASCII name is wrong.
- `lineSequence().toHashSet()` never closes the stream. `tlds.txt` and `blacklist.txt` are read once. Every `findSchoolNames` call, and every `isAcademic` call that misses the TLD list, opens another stream and leaves it to the garbage collector.
- A `HashSet` drops the file order. The repository contract is that line 1 is the official name. Callers cannot rely on that. The Strathclyde test uses `contains` and never notices.

A line whose text is `.group` is a marker (`CONTRIBUTING.md`), not a name. `lib/domains/jp/ac/tohoku.txt` is `Tohoku University` followed by `.group`. `findSchoolNames` returns both. Thirty-two files use the marker.

Twenty-seven domain files are not valid UTF-8. Read as UTF-8 they contain replacement characters, so the name API is wrong even when the license decision is right. The bytes are legacy single-byte text:

- Windows-1252 for the Western European names (Curaçao, Hämeen ammattikorkeakoulu, Université de Haute Alsace, Universidade Norte do Paraná, Mädchenrealschule, University of Wisconsin–Madison, Olofström, Genève, and the others in that set).
- Windows-1250 for `lib/domains/pl/biskupiak.txt` and `lib/domains/pl/edu/wit.txt`.
- Mac Roman for `lib/domains/br/una.txt` and `lib/domains/com/onmicrosoft/politecnicamgprogrammersliv.txt` (`Centro Universitário UNA`). Windows-1252 turns those into `Universit‡rio`.

The follow-up reads and writes names as UTF-8, closes the reader, keeps line order, drops blank lines and exact `.group` markers, and converts those 27 files.

## 8. University of Strathclyde's file names a different university

Severity: medium. Fix.

`lib/domains/uk/ac/strath.txt` is:

```
University of Strathclyde
uka tarsadia university,bardoli
```

Commit `5118b5af` (bhavinpanchal92, 2017-03-28) added the second line to Strathclyde's file. UKA Tarsadia University is in Bardoli, India. It has no domain file of its own anywhere in the tree. Commit `4c8eb182` ("test actualized") then locked the mistake in: `SwotTest` asserts that `cs.strath.ac.uk` contains both names.

The follow-up deletes the stray line and expects only "University of Strathclyde", in that order. It does not invent a Tarsadia domain. Adding `utu.ac.in` or whichever host the university uses now needs the same evidence the README asks of every other new domain.

## 9. The project cannot be built or tested from the repository

Severity: medium. Fix.

There is no Gradle, Maven, or Ant build. `swot.iml` is an IntelliJ module: production sources in `src`, resources in `lib/domains`, tests in `test`, Kotlin from `$KOTLIN_BUNDLED$/lib/kotlin-runtime.jar` (the pre-stdlib jar name), and JUnit from `lib/junit-4.10.jar`. `.document` still lists Ruby paths (`lib/**/*.rb`, `features/**/*.feature`) from the original port. `VERSION` is `0.3.1` with no release tooling.

The follow-up adds a Gradle build that compiles `src`, puts `lib/domains` on the classpath, and runs the JUnit 4 tests on the JDK that is already installed. The IntelliJ module and the checked-in JUnit jar stay, so the existing run configuration keeps working.

## Left as they are

**Blanket school TLDs versus the K-12 rule.** The README says primary and secondary schools are out of scope. `tlds.txt` still blanket-accepts `sch.uk`, `school.nz`, `school.za`, `g12.br`, `hs.kr`, `ms.kr`, `sc.kr`, and several `sch.*` suffixes. Removing those lines would revoke licenses for every address under them. That is a product decision, not a typo.

**Proof URLs stored as names.** Eighty-three domain files contain an `http` URL in the name list. Four use a URL as the first line: `nl/degoudsewaarden.txt`, `fr/cfa-afti.txt`, `fr/campus-eni.txt`, and `de/schule/nw/do/rbb.txt`. Deleting those lines without a replacement name would either blank the official name or, if it were the only line, change eligibility. They need an editor who can replace the URL with the institution's name.

**Shallowest file wins.** `findSchoolNames` returns the first file from the TLD inward. `cs.strath.ac.uk` correctly returns Strathclyde, because there is no `uk/ac.txt`. Where both a parent and a child file exist, the child name is hidden. Examples: `ensao.ump.ma` returns "University Mohammed First" instead of "National School of Applied Sciences Oujda"; `isel.ipl.pt` returns the polytechnic instead of Instituto Superior de Engenharia de Lisboa. Returning the parent institution is a reasonable reading of "this mailbox belongs to that university". Flipping to the deepest file would change names across every nested campus file. Not in this fix.

**Duplicate official names across domains.** The same title opens hundreds of files (Arab Open University, a university system listed once per campus, and so on). Most of those are the same institution on more than one domain. The Strathclyde line was the one that is a different institution pasted into the wrong file.

## What the follow-up changes

The next commit on this branch:

- Folds case with `Locale.ROOT`, takes the host from a URL instead of the first `@`, and accepts angle-bracket addresses and a single trailing dot.
- Rejects a host with a label exactly `alumni`.
- Reads domain files as UTF-8, closes them, preserves official-name order, and ignores blank lines and `.group`.
- Compiles only lowercase `.txt` domains, skips `tlds.txt` and `blacklist.txt`, and emits parents before children.
- Repairs the Strathclyde, Viadrina, and Konrad Lorenz files; re-encodes the 27 legacy name files; lowercases the mixed-case paths; renames the emlyon and CET files; removes the duplicate IJburg file, the duplicate `DBU.txt`, and the empty `dtmik.tj`.
- Adds Gradle and tests for the locale, the URL host, alumni mail, the Strathclyde name, the `.group` marker, and the compiler.

`bcps.org` and `kansaigaidai.jp` stop matching. `edu.emlyon.com` starts matching. The mixed-case domains in item 6 start matching. `netocentre.fr` disappears from the compiled list and still does not match in `isAcademic`.
