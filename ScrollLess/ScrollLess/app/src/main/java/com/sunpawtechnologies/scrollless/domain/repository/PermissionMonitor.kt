package com.sunpawtechnologies.scrollless.domain.repository

import com.sunpawtechnologies.scrollless.domain.model.PermissionSnapshot
import kotlinx.coroutines.flow.StateFlow

/** The single place that checks permission state. Screens observe [state] instead of checking themselves. */
interface PermissionMonitor {
    val state: StateFlow<PermissionSnapshot>

    /** Re-reads state from Android, e.g. when the user returns from system settings. */
    fun refresh()
}
