package com.sunpawtechnologies.scrollless.domain.model

// Room persists these enums by name. Never rename or remove a constant without a migration.

enum class Platform { INSTAGRAM, YOUTUBE }

/** Short-form content whose individual items are counted, as an estimate. */
enum class ContentKind(val platform: Platform) {
    INSTAGRAM_REEL(Platform.INSTAGRAM),
    YOUTUBE_SHORT(Platform.YOUTUBE),
}

/** What the user was doing during a timed session. */
enum class UsageMode {
    /** The app was in the foreground, whatever the content (from Usage Access). */
    APP_FOREGROUND,

    /** Estimated time in the Instagram Reels viewer (from Accessibility). */
    REELS,

    /** Estimated time in the YouTube Shorts player (from Accessibility). */
    SHORTS,

    /** Estimated time on the regular YouTube watch screen (from Accessibility). */
    YOUTUBE_WATCH,
}

enum class TrackingSource { USAGE_STATS, ACCESSIBILITY }

/** Why a tracking-coverage interval ended. Gaps are shown as "not tracked", never as zero. */
enum class CoverageEndReason { STOPPED_BY_USER, PERMISSION_REVOKED, SERVICE_INTERRUPTED }
