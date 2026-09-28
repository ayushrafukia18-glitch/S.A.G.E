package com.sage.app.actions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactResolver(private val context: Context) {
    data class Contact(val displayName: String, val phoneNumber: String)

    suspend fun resolve(name: String): Result<Contact> = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext Result.failure(IllegalArgumentException("Whose contact should I use?"))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure(IllegalStateException("I need contacts permission to look up '$name'. Please allow it and try again."))
        }
        runCatching {
            val all = loadContacts()
            when (val outcome = match(name, all)) {
                is Match.One -> outcome.contact
                is Match.Many -> error("Which one did you mean: ${outcome.names.take(3).joinToString(", ")}?")
                Match.None -> error("I couldn't find '$name' in your contacts.")
            }
        }
    }

    private fun loadContacts(): List<Contact> {
        val out = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
            val n = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val p = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val display = c.getString(n).orEmpty()
                val number = c.getString(p).orEmpty()
                if (display.isNotBlank() && number.isNotBlank()) out += Contact(display, number)
            }
        }
        return out
    }

    sealed interface Match {
        data class One(val contact: Contact) : Match
        data class Many(val names: List<String>) : Match
        data object None : Match
    }

    companion object {
        /** Pure function so it is unit-testable. Tiers are tried in order; the first non-empty tier decides. */
        fun match(query: String, contacts: List<Contact>): Match {
            val q = normalize(query)
            if (q.isEmpty()) return Match.None
            val tiers: List<(Contact) -> Boolean> = listOf(
                { normalize(it.displayName) == q },
                { normalize(it.displayName).startsWith(q) || tokens(it.displayName).any { t -> t == q } },
                { normalize(it.displayName).contains(q) },
                { c -> tokens(c.displayName).any { t -> q.length >= 3 && levenshtein(t, q) <= if (q.length >= 6) 2 else 1 } },
            )
            for (tier in tiers) {
                val hits = contacts.filter(tier)
                if (hits.isEmpty()) continue
                // Same person stored with several numbers is still ambiguous for the number, so key on name+number.
                val distinct = hits.distinctBy { normalizePhone(it.phoneNumber) }
                return if (distinct.size == 1) Match.One(distinct.first())
                else Match.Many(distinct.map { "${it.displayName} (${it.phoneNumber})" }.distinct())
            }
            return Match.None
        }

        fun normalize(value: String) = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "")
        private fun tokens(value: String) = value.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        private fun normalizePhone(value: String) = value.filter(Char::isDigit).takeLast(10)

        fun levenshtein(a: String, b: String): Int {
            if (a == b) return 0
            var prev = IntArray(b.length + 1) { it }
            for (i in 1..a.length) {
                val cur = IntArray(b.length + 1); cur[0] = i
                for (j in 1..b.length) cur[j] = minOf(cur[j - 1] + 1, prev[j] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = cur
            }
            return prev[b.length]
        }
    }
}
