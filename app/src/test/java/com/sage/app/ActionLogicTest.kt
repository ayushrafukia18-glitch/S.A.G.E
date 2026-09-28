package com.sage.app

import com.sage.app.actions.ActionToolExecutor
import com.sage.app.actions.ContactResolver
import com.sage.app.actions.ContactResolver.Contact
import com.sage.app.actions.ContactResolver.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionLogicTest {
    private val book = listOf(
        Contact("Mom", "+91 98765 43210"),
        Contact("Ravi Kumar", "+91 91234 56789"),
        Contact("Ravi Sharma", "+91 90000 11111"),
        Contact("Priya", "+91 80000 22222"),
    )

    @Test fun exactNameWins() = assertEquals(Match.One(book[0]), ContactResolver.match("mom", book))
    @Test fun ambiguousFirstNameAsksOnce() {
        val m = ContactResolver.match("Ravi", book)
        assertTrue(m is Match.Many && m.names.size == 2)
    }
    @Test fun fuzzyTypoResolves() = assertEquals(Match.One(book[3]), ContactResolver.match("Priyaa", book))
    @Test fun unknownContactIsRejected() = assertEquals(Match.None, ContactResolver.match("Zed", book))
    @Test fun sameNumberTwiceIsNotAmbiguous() {
        val dup = listOf(Contact("Mom", "+91 98765 43210"), Contact("Mom", "9876543210"))
        assertTrue(ContactResolver.match("Mom", dup) is Match.One)
    }
    @Test fun parsesUtcOffsetAndLocalIso() {
        assertNotNull(ActionToolExecutor.parseMillis("2026-09-29T08:00:00Z"))
        assertNotNull(ActionToolExecutor.parseMillis("2026-09-29T08:00:00+05:30"))
        assertNotNull(ActionToolExecutor.parseMillis("2026-09-29T08:00:00"))
        assertNull(ActionToolExecutor.parseMillis("tomorrow at 8"))
    }
}
