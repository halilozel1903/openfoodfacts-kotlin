package io.github.halilozel1903.openfoodfacts.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** A small LRU cache of response bodies by url, with a time to live. */
internal class ResponseCache(
    private val maxEntries: Int,
    private val ttl: Duration,
    private val timeSource: TimeSource,
) {
    private class Entry(val body: String, val stored: TimeMark)

    private val mutex = Mutex()
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)

    suspend fun get(url: String): String? = mutex.withLock {
        val entry = entries[url] ?: return@withLock null
        if (entry.stored.elapsedNow() >= ttl) {
            entries.remove(url)
            null
        } else {
            entry.body
        }
    }

    suspend fun put(url: String, body: String) {
        if (maxEntries <= 0) return
        mutex.withLock {
            entries[url] = Entry(body, timeSource.markNow())
            while (entries.size > maxEntries) entries.remove(entries.keys.first())
        }
    }

    suspend fun clear() = mutex.withLock { entries.clear() }
}
