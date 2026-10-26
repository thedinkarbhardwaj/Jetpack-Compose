# ScrollLess — prioritised implementation plan (evidence-based)

Inputs: `analysis/brainpal/BRAINPAL_ANALYSIS.md`, `analysis/BANKING_INVESTIGATION.md`, code inspection 2026-10-26.
Baseline: 152 JVM tests passing (Gradle reported the test task UP-TO-DATE on 2026-10-26, i.e. results from the last real run, with no source changes since).

## Key findings that drive the plan
1. **Banking:** the only plausible interference is the *enabled* Accessibility service. Paytm documents a block ([81004]) for any enabled third-party accessibility service. No ScrollLess code change can hide an enabled service, so the fix is product design: make the app fully useful **without** Accessibility, and make Accessibility easy to switch off.
2. **"Features don't work":** in the Accessibility-free mode **no limit is ever evaluated**. Only the Reels-count limit exists (it needs Accessibility). Instagram time is measured but has no limit. The YouTube Shorts/video limits can be configured but are never measured.
3. **No limit-reached experience** exists outside the optional counter pill: no notification, no reminder, no background check.
4. Background cost is already low (no service, no polling). Adding reminders must keep it that way.
5. BrainPal's strengths worth matching: a fast personal onboarding, a goal and plan framing, one-permission-at-a-time setup with a "why" sheet, an **in-Settings coach overlay**, mascot mood tied to state, and widgets. Things to avoid: gift boxes, spin wheel, countdown pressure, the salary question, and a hard permission gate with no skip.

## P0 — Banking safety (do first, small and low-risk)
| # | Change | Files (expected) | Tests |
|---|---|---|---|
| 0.1 | Extract the overlay/enforcement decision into a pure function `EnforcementPolicy.shouldShowCounter(foreground, excluded, consent, enabled, uncertain, hiddenForSession)`, with the service calling it. Exclusion stays in the enforcement path. | new `domain/policy/EnforcementPolicy.kt`, `ScrollLessAccessibilityService.kt` | `EnforcementPolicyTest`: every built-in payment package → never shown; user-excluded → never; unknown/uncertain → never; Instagram + consent + enabled → shown |
| 0.2 | **Payment-app warning before enabling Accessibility.** Add text to the disclosure and setup screens: "While Reels tracking is on, Paytm and some banking apps may refuse to open. You can switch it off any time from ScrollLess." Bump `ACCESSIBILITY_DISCLOSURE_VERSION` → 3. | `strings.xml`, `AppSettings.kt`, permissions/onboarding UI | update consent tests |
| 0.3 | **One-tap "Turn off Reels tracking"** in Settings and on the dashboard status card. The service calls `disableSelf()` (API 24+) via a signal in `AppContainer`, with no trip to system Settings. Re-enabling still goes through Settings (Android rule). | service, `AppContainer`, `SettingsScreen` | ViewModel test for the signal |
| 0.4 | **Payment-app nudge (optional):** when the service sees an excluded payment package come to the front, post at most one notification per hour, "Paytm may block while Reels tracking is on — Turn off", with an action that calls `disableSelf()`. No overlay, nothing launched. | service, new `notifications/` | dedup/throttle unit test |
| 0.5 | Keep `isAccessibilityTool=false`, keep the counter off by default, keep the narrow package scope. Document the platform limitation in Settings → "Payment apps". | strings | — |

## P1 — Make limits actually work without Accessibility
| # | Change | Notes / trade-offs |
|---|---|---|
| 1.1 | Add **app-time limits** `INSTAGRAM_MINUTES` and `YOUTUBE_MINUTES` (Usage Access data). Make them the primary limits. Keep the Reels-count limit for Advanced mode only. | Room/DataStore: limits are stored by name, so a new enum entry needs no migration (verify `LimitHistoryEntity`). |
| 1.2 | **Hide or disable the YouTube Shorts and regular-video limits** until something measures them ("Coming later"). This stops users setting limits that silently never fire. | Removes a broken feature from the UI; the data model stays. |
| 1.3 | **Limit and warning notifications:** a WorkManager periodic job every 15 min (the minimum), with a battery-not-low constraint. It imports usage, evaluates limits with the existing `limitStatus()`, and posts one notification per (metric, day, level), deduped through the existing `WarningDao`/`WarningFiredEntity`. A `DATE_CHANGED`/timezone receiver is not needed, because the day key is computed from the local zone at evaluation time. | Trade-off: a notification can arrive up to ~15 min late in Basic mode. In Advanced mode the service already sees Instagram in front and can notify immediately. Adds the `androidx.work` dependency and `POST_NOTIFICATIONS`. |
| 1.4 | Also evaluate on app open (already importing). Handle Usage Access revocation (already caught). | — |

## P2 — Experience redesign (Compose, existing nav)
1. **Onboarding (3 short steps + optional):** which apps → what you want time back for → daily limit (with a suggestion from the **real** 7-day Usage Access history when granted; otherwise a labelled estimate). A plan screen shows a taper curve from the real baseline. Everything is skippable.
2. **Permission setup:** one card per permission with a "Why?" bottom sheet. After opening Settings, show a translucent **coach activity** with numbered steps (BrainPal pattern; no overlay permission needed). Never re-ask for something already granted (`PermissionMonitor` already knows).
3. **Dashboard:** hero ring for today's main limit (time), remaining time, comparison with yesterday and the 7-day average, and a status illustration (original artwork) whose mood follows WITHIN / APPROACHING / REACHED. Show an empty state when Usage Access is missing.
4. **Limit reached screen:** opened from the notification. "You reached your 45 min Instagram limit": Take a break (closes the screen and goes Home via `Intent.CATEGORY_HOME`, never touching other apps), Remind me in 10 min, Open ScrollLess. No blocking.
5. **Stats:** day/week/month on existing analytics, streaks only from covered days (coverage data already exists).
6. **Settings:** monitoring status, Reels tracking on/off (0.3), payment-app exclusions, notifications, theme, data reset, battery note.
7. **Motion:** animated progress sweep on value change (`animateFloatAsState`), staged entrance for onboarding text, a bottom-sheet slide, and a one-shot celebration when a day ends under the limit. Respect the system animator scale / reduced motion; no infinite animations.

## P3 — Later
Home-screen widget (Glance), YouTube Shorts detection (Accessibility, same safeguards), gradual-reduction plan auto-adjusting limits.

## Order of work
P0.1 → P0.2 → P0.3 → P1.1 → P1.2 → P1.3 → P0.4 → P2 → P3. After each block: build, run JVM tests, install on the phone, and check logcat.

## Approval-gated items (will ask before doing)
- Adding `androidx.work` (small, standard).
- Disclosure version bump (users are re-asked for consent).
- Any change to Room schema (none expected).

## Manual steps needed from you
1. Turn **Wireless debugging** back on (it dropped at ~22:10–22:48) so device testing can continue.
2. For tests B–D in `BANKING_INVESTIGATION.md`: turn on ScrollLess's (and separately BrainPal's) Accessibility service yourself and open Paytm/GPay/YONO/iMobile to their first screen, or allow me to do it.
3. BrainPal's dashboard, counter, reminders and stats remain unexamined until its permissions are granted.

## Final verification checklist (to run after implementation)
- [ ] ScrollLess installed, Reels tracking **off**: Paytm, GPay, PhonePe/BHIM, YONO, iMobile open normally; a ₹1 self-transfer UPI flow works (done by you).
- [ ] Reels tracking **on**: record which apps warn; confirm the in-app warning text matches reality.
- [ ] "Turn off Reels tracking" button → Paytm opens normally without visiting system Settings.
- [ ] Counter on, Instagram → open a payment app via share/checkout: the counter disappears within 1 s and taps work.
- [ ] No ScrollLess window is visible over any payment app (`dumpsys window | grep scrollless`).
- [ ] Limit notification fires once per level per day; no repeat after reboot; resets at local midnight.
- [ ] `dumpsys batterystats` shows no wakelocks and only 15-min jobs for ScrollLess over 24 h.
