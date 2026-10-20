package com.example.flowlog.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactAlarmPromptPolicyTest {
    private val android11 = 30
    private val android12 = 31
    private val android14 = 34

    @Test
    fun belowAndroid12HasNoPermissionToAskFor() {
        val status = ExactAlarmPromptPolicy.status(android11, canScheduleExactAlarms = false)

        assertEquals(ExactAlarmStatus.NOT_REQUIRED, status)
        assertFalse(ExactAlarmPromptPolicy.shouldPrompt(status, alreadyAsked = false))
    }

    @Test
    fun android12AndAboveReportAllowedOrDenied() {
        assertEquals(ExactAlarmStatus.ALLOWED, ExactAlarmPromptPolicy.status(android12, canScheduleExactAlarms = true))
        assertEquals(ExactAlarmStatus.DENIED, ExactAlarmPromptPolicy.status(android12, canScheduleExactAlarms = false))
        assertEquals(ExactAlarmStatus.DENIED, ExactAlarmPromptPolicy.status(android14, canScheduleExactAlarms = false))
    }

    @Test
    fun deniedAndNeverAskedPromptsOnce() {
        assertTrue(ExactAlarmPromptPolicy.shouldPrompt(ExactAlarmStatus.DENIED, alreadyAsked = false))
    }

    @Test
    fun deniedButAlreadyAskedDoesNotPromptAgain() {
        assertFalse(ExactAlarmPromptPolicy.shouldPrompt(ExactAlarmStatus.DENIED, alreadyAsked = true))
    }

    @Test
    fun allowedNeverPrompts() {
        assertFalse(ExactAlarmPromptPolicy.shouldPrompt(ExactAlarmStatus.ALLOWED, alreadyAsked = false))
        assertFalse(ExactAlarmPromptPolicy.shouldPrompt(ExactAlarmStatus.ALLOWED, alreadyAsked = true))
    }

    @Test
    fun notRequiredNeverPromptsEvenIfNotAsked() {
        assertFalse(ExactAlarmPromptPolicy.shouldPrompt(ExactAlarmStatus.NOT_REQUIRED, alreadyAsked = false))
    }
}
