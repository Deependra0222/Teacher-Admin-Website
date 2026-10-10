package com.smartattend.app.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryFormattingTest {
    @Test
    fun percentageIsRenderedAsAValueInsteadOfAFormatPlaceholder() {
        assertEquals("68.0% confirmed • Late", formatHistorySummary(68.0, true))
        assertEquals("91.2% confirmed", formatHistorySummary(91.24, false))
    }
}

