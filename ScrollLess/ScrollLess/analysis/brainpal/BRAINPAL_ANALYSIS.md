# BrainPal — observable behaviour analysis (Phase 2, part 1)

Session: 2026-10-26, 21:59–22:10 IST. Device: Xiaomi Redmi 13 5G (`breeze`, model 2406ERN9CI), Android 14 / API 34, HyperOS.
Connection: ADB over Wi-Fi (TLS). Control: `adb shell input tap/keyevent` (works on this device), `screencap`, `uiautomator dump`.
Screenshots + UI dumps: `analysis/brainpal/screens/` (`NN_name.png` + `NN_name.xml`).

**Legend:** ✅ confirmed by direct observation · 🔎 inference (not verified) · ⏸ not yet reached

## Identity (✅)

| | |
|---|---|
| Launcher label | BrainPal |
| Package | `com.brainrot.android` (launcher activity `.MainActivity`, the only launcher match) |
| Version | 19.1.619 (code 619), minSdk 32, targetSdk 36 |
| Installed | 2026-10-26 19:55 (today; fresh install, no prior grants) |

## Declared capabilities (✅ from `dumpsys package` — system metadata, no decompilation)

Permissions: `SYSTEM_ALERT_WINDOW`, `PACKAGE_USAGE_STATS`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `POST_NOTIFICATIONS`, `ACTIVITY_RECOGNITION`, `DETECT_SCREEN_CAPTURE`, `NFC`, `READ_EXTERNAL_STORAGE`, `READ_BASIC_PHONE_STATE`, billing, FCM, Ad ID / AdServices.

Components (names only): `services.ReelsAccessibilityService` (BIND_ACCESSIBILITY_SERVICE), `PermissionStepsOverlayActivity`, `core.receiver.DateChangedReceiver`, `feature_fallback_counter.TierBootReceiver` (boot), Glance widgets `ReelsCounterWidgetReceiver`, `ReelsCounterWidgetExpandedReceiver`, `BuddyWidgetReceiver`, WorkManager, Firebase Messaging, Razorpay (`com.razorpay.DeeplinkActivity`, `ai.regainapp.payments…`), Facebook SDK.

🔎 Inferences from names only: reel counting is accessibility-based; a "fallback counter" tier exists (possibly Usage-Access-based when Accessibility is off); daily reset hooks `DATE_CHANGED`; home-screen widgets show the count and a mascot "buddy"; payments run through Razorpay (UPI/GPay) as well as Play billing.

## Screen-by-screen

| # | Screen / purpose | Path | UI elements | Actions → result | Motion | Shot |
|---|---|---|---|---|---|---|
| 1 | **Welcome** — value prop | Cold launch (~2.4 s) | BRAINPAL wordmark; language chip "English"; phone mock-up playing a reel with a brown pill "🧠 3" counter; headline "See your reels count"; platform icons Instagram / YouTube Shorts / Snapchat / Facebook; gradient CTA "Get Started" | Get Started → **system notification permission dialog immediately** (no primer) | Looping video in mock-up ✅ | 01, 01b, 02 |
| 2 | Notification prompt | after Get Started | Android dialog | Allowed (low-risk, reversible) | — | 02 |
| 3 | **Quiz 1/4** "What do you do?" | — | "1 of 4" progress; mascot + speech bubble; radio cards School / College / Working / Other; Continue disabled until selection | "Other" reveals free-text field "Type your answer" and keeps Continue blocked | — | 03, 03b, 03c |
| 4 | **Quiz 2/4** "Where do you scroll the most?" (multi) | — | Instagram Reels, YouTube Shorts, Snapchat, Facebook, Moj/Josh/ShareChat | multi-select | — | 04, 04b |
| 5 | **Quiz 3/4** "What do you want that time back for?" (multi) | — | Studies, Work, Sleep, Fitness, Family, Peace of mind | chose Sleep + Peace of mind | — | 05 |
| 6 | **Quiz 4/4** "When do you scroll the most?" | — | Morning in bed / During study or work / After dinner / Late at night | chose Late at night | — | 06 |
| 7 | **Personal payoff** "Win back *your sleep*. Starting today." | — | big waving mascot; accent word coloured from goal; CTA "Let's go" | — | — | 07, 07b |
| 8 | Reflection "Can you name the last 3 reels you watched?" | — | Yes all 3 / Maybe one / Not even one | — | — | 08 |
| 9 | "How much time do you spend on Reels/Shorts daily?" | — | 1–3 / 3–5 / 5–7 / 7+ hours | chose 1–3 h | — | 09 |
| 10 | Insight "Your brain jumps every **29 seconds**" / "250 new reels a day. Nothing stays." | — | tired mascot holding phone; red accent number | — | — | 10, 10b |
| 11–13 | Insight cards: "Late-night reels push sleep back", "**30 days a year** inside reels — *From your answer: 2 hours a day*", "Opened to relax. Closed more tired? — Built to pull you back. Not your fault." | — | full-screen statement + mascot + Continue | — | — | 11–13 |
| 14 | "What is your monthly salary?" | — | ₹25k / ₹50k / ₹1L / ₹2L+ ("Student? Pick what you expect to earn.") | chose ₹25,000 **as a placeholder, not real data** | — | 14 |
| 15 | Cost reveal "**91,250** reels a year. You couldn't name 3." / "₹90,000 of your time, every year." | — | huge red number; very tired mascot; CTA "Win back my time" | — | Staged word-by-word text reveal; CTA appears after ✅ | 15, 15b |
| 16 | "14 hours a week for your sleep" | — | — | — | — | 16 |
| 17 | "You can take it back — Pick a daily number that feels okay. We count every reel with you." | — | — | — | — | 17 |
| 18 | **Daily limit picker** "How many reels a day feels okay?" | — | 25 / 50 / 100 / 150 reels (no custom value seen) | chose 50 | — | 18 |
| 19 | **Plan** "Your plan by **25 Nov**" (30 days) | — | taper curve 250 "today" (red) → 50 "a day" (green), gradient stroke; "**1h 36m** more for your sleep, every day."; CTA "Start my plan" | — | Curve draws on, end label appears after ✅ | 19, 19b |
| 20 | **Gift boxes** "Let's choose one — Tap a gift box to open it" | Start my plan | 3 illustrated boxes | box → "10% OFF" + confetti, CTA "Try again · 1 chance"; 2nd box → "70% OFF" | lid flies off, card rises, confetti ✅ | 20b, 21, 21b, 22, 23 |
| 21 | **Paywall** | after 2nd box | X close; "Need Help?"; "50 reels a day by 25 Nov"; ticket "70% Discount"; countdown "Ends in 02:59:49"; testimonial carousel; plans Yearly ₹25/mo (₹999→₹299/yr, "70% OFF" badge) / Monthly ₹125/mo; "Pay Using GPay ▾"; Continue; "Billed Annually · Cancel Anytime" | **X tapped (no purchase)** | sunburst bg | 24 |
| 22 | Exit intercept "Before you go" | X | "Reels cost you ₹90,000" vs "BrainPal costs ₹299 a year — Less than ₹1 a day"; "Keep my 70% gift" / "Not now" | Not now | — | 25 |
| 23 | "Remember your last reel?" → "250 reels a day. Not one stayed." | Not now | Yes / Honestly no; then offer again | Not now | — | 26, 27 |
| 24 | **Spin wheel** "One last spin" ("Every spin lands on the top prize") → "You won 80% off ₹999 → ₹199 a year, Ends in 2:58:30" | Not now | Spin / Claim 80% off / Not now | Not now | wheel spin ✅ | 28b, 29, 29b |
| 25 | **Account** "Create an account to save your progress" | after declining | Continue With Google / **Skip And Continue**; Privacy Policy link | **Skipped** (no account linked) | — | 30, 30b |
| 26 | **Privacy primer** "BrainPal only counts reels. It never reads your bank, passwords, or chats." | Skip | mascot hugging padlock in phone frame | Continue | — | 31 |
| 27 | **Permission gate** "Enable permissions to start counting reels" | Continue | one row at a time: "Display over other apps — To show reels count" + Allow; bottom pill "Why should I give this permission?" | — | — | 32, 34 |
| 28 | **Why sheet** (bottom sheet) | Why… | lists 5 permissions: Display over other apps (count on top of apps) · Display pop-up (reminders when scrolling too long) · Background (keep count updating) · Auto Start (start with device) · Accessibility (detect & count reels); "You can remove this permission anytime"; "Read our policy" | close | sheet slide-up ✅ | 33 |
| 29 | **Settings coach** (`PermissionStepsOverlayActivity`) | Allow | Opens system *Display over other apps* list, then its own activity on top: animated MIUI mock with hand pointer + 3 numbered steps; "Got It" | Got It → plain Settings list; **Back without toggling** → returns to gate unchanged, no nag | animated hand ✅ | 35, 36, 37 |

### Lifecycle (✅)
- Home → relaunch: HOT start, 262 ms, same permission gate.
- `force-stop` → relaunch: COLD start, 3.2 s, **resumes at the permission gate** (onboarding answers/plan persisted).
- Permission gate has **no skip**; the dashboard/stats/settings are not reachable without granting.

### Design language (✅ observed)
Near-black background (#0A0A0A-ish); warm peach→amber gradient pill CTAs with dark-brown label; heavy rounded display font for headlines, lighter grotesque for body; red for "bad" numbers, green for target, amber for highlighted words; recurring pink brain mascot whose mood tracks the message (happy → tired → exhausted → hopeful with padlock); one idea per full-screen card; dark rounded option cards with radio circles.

## Free vs paid
- ✅ Onboarding, quiz, plan, and permission flow are free.
- ⏸ Unknown whether counting/dashboard work without subscribing: the paywall appeared before the permission gate but was dismissible, which *suggests* a free tier exists. Needs verification past the gate.

## Not yet verified (needs your approval — see report)
Dashboard, counter overlay over Instagram, reminders/"pop-up", limit-reached behaviour, stats, widgets, settings, background behaviour, Accessibility-service scope (event types / package filter — visible via `dumpsys accessibility` only once enabled), battery/CPU usage, and **whether BrainPal itself triggers the same banking-app warnings**.

## Recommended ScrollLess equivalents (summary)
| BrainPal | ScrollLess recommendation |
|---|---|
| Quiz → personalised goal | Short 2–3 question setup (apps, goal, daily limit); skippable |
| "Your brain jumps every 29 s / ₹ cost" | Optional "what this costs you" card computed **only** from real tracked data, or explicitly labelled as an estimate from the user's answer |
| 30-day taper plan | Optional gradual-reduction plan derived from the real 7-day baseline (Usage Access history already imported) |
| Permission gate, one row at a time + "Why?" sheet | Already have a permissions screen; add per-permission "why" sheet and a settings coach screen |
| `PermissionStepsOverlayActivity` coach | Same pattern: translucent activity launched right after the Settings intent (no overlay permission needed) |
| Mascot mood | Original mascot/illustration state tied to real limit status (WITHIN / APPROACHING / REACHED) |
| Gift box / spin wheel / countdown | **Do not copy** — manipulative monetisation, not needed |
| Salary question | Skip (sensitive, unnecessary) |
| Widgets | Candidate later feature (Glance) |

## Addendum — 2026-10-26 ~23:50, after permissions were granted on the phone (by the user, not by Claude)
State observed via ADB: BrainPal Accessibility **enabled and bound**, `SYSTEM_ALERT_WINDOW` allowed, battery-optimisation whitelisted, Usage Access **not** granted.

Accessibility service as reported by `dumpsys accessibility` (✅): event types `WINDOW_STATE_CHANGED, WINDOW_CONTENT_CHANGED, VIEW_SCROLLED, WINDOWS_CHANGED`; notificationTimeout 500 ms; capabilities = 33 → **retrieve window content + perform gestures**; no package filter shown. Broader than ScrollLess's (Instagram-only, no content-changed events, no gestures). 🔎 Gesture capability is probably used to navigate away from Reels (the paid "Block Shorts with Pro" feature).

| # | Screen | UI elements (✅) | Shot |
|---|---|---|---|
| 30 | Play in-app update sheet on launch | "Update available — To use this app, download the latest version…"; dismissed (not updated) | 40 |
| 31 | **Home ("You" tab)** | avatar, BRAINPAL wordmark, "Help"; sign-in banner; large happy mascot; big number **"1 — Reels Scrolled Today"**; per-platform chip (Instagram 1); "PRO · Claim 80% Off" banner with live countdown; bottom nav **You / Battle / Progress** | 41b |
| 32 | **Progress** | "Daily / Weekly" segmented toggle; "< Today >" date pager; hourly bar chart 12am–12am with value label; tiles **"1 Total Reels"** and **"— Time spent"** (dash = no data, because Usage Access isn't granted: honest empty state); "PRO · Block Shorts with Pro"; "Top apps" list (Instagram 1) | 42 |
| 33 | **Scroll Battle** | "Friends" button; date pager; leaderboard: You (rank 1, 1 reel), then 4 placeholder rows "Invite your Bestie / Roommate / Night Owl / Crush" with Invite buttons | 43 |

Free vs paid (✅ now confirmed): reel counting, home, progress (daily/weekly) and battle are usable without paying. **Blocking ("Block Shorts") is Pro-only.**
Still not observed: the on-Instagram counter pill in action, limit-reached behaviour, reminders, Settings/profile, widgets.
