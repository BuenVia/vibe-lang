package com.vibelang.android.ui.practice

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.util.Locale

/** Whether a correct answer missed the speed limit (never true when the speed rule is off). */
fun isSlow(responseMs: Long?, thresholdMs: Int?): Boolean =
    responseMs != null && thresholdMs != null && responseMs > thresholdMs

/** "6.2 s" */
fun seconds(ms: Long): String = String.format(Locale.ROOT, "%.1f s", ms / 1000.0)

/** Shown on a correct answer that was too slow to move the item up in Review. */
@Composable
fun SlowNote(responseMs: Long?, thresholdMs: Int?) {
    if (!isSlow(responseMs, thresholdMs)) return
    Text(
        "But slow: ${seconds(responseMs!!)} (limit ${seconds(thresholdMs!!.toLong())}). Answer faster to move it up in Review.",
        style = MaterialTheme.typography.bodyMedium,
    )
}
