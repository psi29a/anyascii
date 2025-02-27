package com.anyascii.build

import java.nio.file.Path
import java.util.TreeMap
import kotlin.io.path.bufferedWriter
import kotlin.io.path.forEachLine
import kotlin.io.path.useLines

typealias Table = TreeMap<CodePoint, String>

fun Table.write(path: String) = apply {
    Path.of(path).bufferedWriter().use {
        for ((cp, r) in this) {
            check(PRINTABLE_ASCII.containsAll(r))
            it.append(String(cp)).append('\t').append(r).append('\n')
        }
    }
}

fun Table(file: String) = Table().apply {
    Path.of("input/tables/$file.tsv").forEachLine { line ->
        if (line.startsWith('#')) return@forEachLine
        val cp = line.codePointAt(0)
        val i = Character.charCount(cp)
        check(line[i] == '\t')
        put(cp, line.substring(i + 1))
    }
}

fun readSyllableTable(file: String) = Table().apply {
    Path.of("input/$file.csv").useLines { lines ->
        val itr = lines.filter { !it.startsWith('#') }.iterator()
        val vowels = itr.next().split(',')
        for (e in itr) {
            val row = e.split(',')
            val consonant = row[0]
            for (i in 1 until row.size) {
                val col = row[i]
                if (col.isEmpty()) continue
                val vowel = vowels[i]
                val s = if ('-' in vowel) {
                    vowel.replace("-", consonant)
                } else {
                    consonant + vowel
                }
                this[CodePoint(col)] = s
            }
        }
    }
}

fun Iterable<CodePoint>.normalize(normalizer: (CodePoint) -> String) = Table().apply {
    for (cp in this@normalize) {
        val a = String(cp)
        val b = normalizer(cp)
        if (a != b) put(cp, b)
    }
}

fun Table.normalize(normalizer: (CodePoint) -> String) = apply {
    for (cp in ALL) {
        if (cp in this) continue
        val output = transliterate(normalizer(cp))
        if (output != null) {
            this[cp] = output
        }
    }
}

inline fun Iterable<CodePoint>.toTable(map: (CodePoint) -> String) = associateWithTo(Table(), map)

inline fun Iterable<CodePoint>.alias(nameMap: (String) -> String) = associateWithTo(Table()) { cp ->
    val name2 = nameMap(cp.name)
    String(codePointFromName(name2) ?: cp)
}

fun Table.cased(codePoints: Iterable<CodePoint>) = apply {
    for (cp in codePoints) {
        if (cp in this) continue
        val u = cp.upper()
        if (u != cp) {
            this[u]?.let { this[cp] = it.lower() }
            continue
        }
        val l = cp.lower()
        if (l != cp) {
            this[l]?.let { this[cp] = it.title() }
        }
    }
}

fun Table.transliterate() = apply {
    var f: Boolean
    do {
        f = false
        for (e in iterator()) {
            if (!ASCII.containsAll(e.value)) {
                f = true
                e.setValue(checkNotNull(transliterate(e.value)))
            }
        }
    } while (f)
}

fun Table.transliterate(s: String): String? {
    val sb = StringBuilder()
    for (cp in s.codePoints()) {
        val r = get(cp) ?: return null
        sb.append(r)
    }
    return sb.toString()
}

fun Table.transliterateAny(s: String): String {
    val sb = StringBuilder()
    for (cp in s.codePoints()) {
        sb.append(get(cp) ?: String(cp))
    }
    return sb.toString()
}

fun Iterable<CodePoint>.intValues() = Table().apply {
    for (cp in this@intValues) {
        val n = cp.numericValue ?: continue
        this[cp] = n.toInt().toString()
    }
}