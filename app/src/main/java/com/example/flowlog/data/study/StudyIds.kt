package com.example.flowlog.data.study

@JvmInline
value class StudyLinkId(val value: String)

@JvmInline
value class StudyDecisionId(val value: String)

object StudyPhase {
    const val RECORD = "RECORD" // Whole source record, independent of its current study/review stage.
    const val COURSE_SESSION = "COURSE_SESSION"
    const val STUDY = "STUDY"
    const val REVIEW_1 = "REVIEW_1"
    const val REVIEW_2 = "REVIEW_2"
    val ALL = setOf(COURSE_SESSION, STUDY, REVIEW_1, REVIEW_2, RECORD)
}

object StudyDecisionKind {
    val ALL = setOf(
        "CLASSIFICATION_CONFIRM", "STUDY_LINK_CONFIRM", "STUDY_LINK_REMOVE",
        "BUTTON_USE", "BUTTON_ADD", "BUTTON_REPLACE",
        "RECOMMENDATION_SHOWN", "RECOMMENDATION_DISMISS", "RECOMMENDATION_SNOOZE",
        "RECOMMENDATION_DISABLE", "UNDO"
    )
}

object StudyDecisionOutcome {
    val ALL = setOf("APPLIED", "DECLINED", "SNOOZED", "SHOWN")
}

/** users/{uid}/activityStudyLinks, interactionDecisions 문서 ID 규칙 (웹과 동일). */
object StudyIds {
    const val SCHEMA_VERSION = 1
    private const val HEX = "0123456789ABCDEF"
    private const val UNRESERVED_MARKS = "-_.!*'()"

    /** Firestore activity doc id: legacyId.toString() ?: activityId (FirebaseSyncDataSource 와 동일). */
    fun activityDocId(legacyId: Long?, activityId: String): String = legacyId?.toString() ?: activityId

    fun linkId(activityDocId: String, lessonId: String, phase: String): StudyLinkId {
        require(phase in StudyPhase.ALL) { "Unknown study phase: $phase" }
        return StudyLinkId(encodeUriComponent(activityDocId) + "~" + encodeUriComponent(lessonId) + "~" + phase)
    }

    fun decisionId(mutationId: String, kind: String): StudyDecisionId {
        require(kind in StudyDecisionKind.ALL) { "Unknown decision kind: $kind" }
        return StudyDecisionId("$mutationId~$kind")
    }

    /** JS encodeURIComponent 호환 (UTF-8, 대문자 hex). */
    fun encodeUriComponent(value: String): String {
        val out = StringBuilder()
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || UNRESERVED_MARKS.indexOf(ch) >= 0) {
                out.append(ch)
            } else {
                out.append('%').append(HEX[c shr 4]).append(HEX[c and 0x0F])
            }
        }
        return out.toString()
    }
}
