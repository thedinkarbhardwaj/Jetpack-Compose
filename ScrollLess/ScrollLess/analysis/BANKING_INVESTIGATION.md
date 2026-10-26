# ScrollLess × Paytm / Google Pay / banking apps — investigation (Phase 3)

Date: 2026-10-26. **Status: root cause strongly indicated for Paytm, not yet reproduced on this device.**

## 1. What ScrollLess can actually do to other apps (code inspection ✅)

| Mechanism | Present? | Scope | Can it affect payment apps? |
|---|---|---|---|
| `PACKAGE_USAGE_STATS` (Usage Access) | Yes | Read-only history, queried only when the ScrollLess UI is open | No known interaction. Nothing is drawn or intercepted. |
| AccessibilityService `ScrollLessAccessibilityService` | Yes, **optional** ("Advanced Reels tracking") | Manifest XML: `packageNames=com.instagram.android`, window-state + scroll events, `canRetrieveWindowContent`, `flagReportViewIds`, `isAccessibilityTool=false`. At runtime, **only when the optional counter is on**, widens to window events from all apps (package name only). | **Yes, indirectly — by being enabled at all.** See §2. It never performs gestures, global actions, clicks or text reads. |
| Overlay (`TYPE_ACCESSIBILITY_OVERLAY` counter pill) | Optional, **off by default** | Shown only while Instagram is confirmed in front; hidden when an excluded package (14 built-in payment/bank apps + user list) is seen, and by a 500 ms active-window watchdog when the foreground is uncertain | Low. If it ever stayed on screen over a payment app, apps that filter obscured touches would reject taps. The code already guards against this, but it is untested on real payment apps. |
| `SYSTEM_ALERT_WINDOW` | **No** | — | — |
| Foreground service / wake lock / alarms / WorkManager / boot receiver | **None** | — | — |
| VPN, device admin, notification listener, `startActivity` of other apps, app blocking | **None** | `startActivity` is only used for system Settings pages and URLs | — |
| `<queries>` for 14 payment/bank packages | Yes | Only lets Settings show which built-in exclusions are installed | No. Package visibility has no effect on the other app. |

Conclusion: ScrollLess contains **no blocking, interception, or global-overlay logic**. Its restriction path never targets anything except Instagram. The only component that a payment app can perceive is the **enabled Accessibility service** (and, in the edge case above, the counter overlay).

## 2. Most likely root cause (strong evidence, ⚠ not reproduced here)

Paytm shows a block screen **"[81004] Accessibility settings are enabled. Please disable them for security purposes."** when *any* third-party accessibility service is enabled. Public user reports and how-to videos show this message (sources below). Banking apps and anti-fraud SDKs commonly treat any enabled non-system accessibility service as a malware risk, because Android banking trojans abuse Accessibility (e.g. Drinik targeted 18 Indian banks including SBI).

Why ScrollLess's existing mitigations don't help:
- `packageNames="com.instagram.android"` restricts what *ScrollLess* receives. It does **not** hide the service from `AccessibilityManager.getEnabledAccessibilityServiceList()`, which any app can call.
- `isAccessibilityTool=false` is correct for Play policy. Declaring it `true` to dodge detection would misrepresent the app, so that option is ruled out.
- Hiding the counter over payment apps cannot help, because the check is on *enabled state*, not on anything drawn.

This matches the report that "installing or enabling" ScrollLess breaks Paytm. **Unverified:** whether GPay, SBI YONO, and ICICI iMobile do the same on this phone. The search found no GPay reports. Assume per-app behaviour differs until tested.

BrainPal relies on an AccessibilityService too (`services.ReelsAccessibilityService`). 🔎 If Paytm's check is generic, BrainPal would trigger it as well. This is unverified, because BrainPal's service was never enabled.

### Secondary hypotheses (lower likelihood)
1. Counter pill left visible over a payment flow launched from Instagram (e.g. a UPI intent in a shopping checkout), causing an "obscured window" touch rejection. Mitigated in code; never tested with real apps.
2. Sideloaded install source (ADB/Studio, no Play installer). Some anti-fraud SDKs flag risky apps that are sideloaded *and* declare an accessibility service, even when it is disabled. Not evidenced; test by checking a payment app with ScrollLess installed but its service **off** (baseline test below).

## 3. Device state during this session (✅)
- 21:58: ScrollLess installed 21:49 (fresh reinstall), `enabled_accessibility_services=null`, `accessibility_enabled=0`, no runtime grants. App-op `BIND_ACCESSIBILITY_SERVICE` showed last use ~21:51, which suggests the service was briefly on after reinstall and then off. Not confirmed which.
- No third-party accessibility service was enabled, so a "monitoring on" comparison was **impossible without turning on Accessibility**, which I was asked not to do.
- ~22:10–22:48 the phone dropped off Wireless debugging (mDNS shows nothing). The **baseline test** (open Paytm / GPay / YONO / iMobile first screens with ScrollLess installed but inactive) is **not yet run**.

## 4. Tests still required (see manual checklist in the plan)
| Test | Needs |
|---|---|
| A. Baseline: service **off**, open each payment app's first screen | Phone reconnected (I can run this) |
| B. ScrollLess service **on**, counter off | You turn on Accessibility (I was told not to) |
| C. Service on + counter on, open GPay from Instagram flow | You |
| D. Same with BrainPal's service instead of ScrollLess's | You |

Expected if §2 is right: A = all normal; B = Paytm shows [81004]; others vary.

## Sources
- [Threads — Paytm warning [81004] "Accessibility settings are enabled"](https://www.threads.com/@nickysweet857/post/DU7HVirk-Au/is-anyone-else-getting-this-paytm-warning-why-is-paytm-blocking-access-because)
- [YouTube — Paytm "Accessibility settings are enabled. Please disable them for security purposes"](https://www.youtube.com/watch?v=Qxi5qq2rRjY)
- [YouTube — Paytm "Accessibility settings are enabled" problem fix](https://www.youtube.com/watch?v=oikCQtlYfYQ)
- [Samsung Community — banking app disallows access when 3rd-party accessibility is enabled](https://us.community.samsung.com/t5/Galaxy-S24/Feature-Request-Allow-turn-off-accessibility-settings-to-3rd/m-p/2970832)
- [CIMB — how the bank detects malware with accessibility permission](https://www.cimbbank.com.kh/en/business/help-support/faq/octo-biz/security/how-does-the-bank-detect-if-my-device-has-malware-with-accessibility-permission.html)
- [Heimdal — Drinik malware targets 18 Indian banks via Accessibility](https://heimdalsecurity.com/blog/18-indian-banks-targeted-by-new-version-of-drinik-android-malware/)

---

## 5. Device test results — 2026-10-26 ~23:49 (✅ actually run)

Conditions, read via ADB: `adb_enabled=1`, `adb_wifi_enabled=1`, `development_settings_enabled=1`. A third-party Accessibility service **was enabled and bound: BrainPal's**, which the user switched on. ScrollLess's service was **off**. Each app was launched to its first screen only. No login, no taps inside the apps. Screenshots: `analysis/banking/screens/`.

| App | Result | Message shown | Shot |
|---|---|---|---|
| Paytm `net.one97.paytm` | **Blocked** | "USB Debugging Detected — Paytm app identified your mobile is actively connected through USB. For security reasons, disconnect the USB connection and continue to use the App. [Close App]" (Powered by Paytm Protect). Phone was on **Wi-Fi** debugging, not a cable. | B1, B1b |
| SBI YONO `com.sbi.lotusintouch` | **Blocked** | "Manage Developer Mode — For secure access to Yono, please disable the developer mode by going to Settings → Developer Options → Off. [Exit]" | B3 |
| Google Pay `com.google.android.apps.nbu.paisa.user` | **Opened normally** (home: Pay anyone on UPI, Scan QR…). MIUI App Lock shows its pattern screen first. | — | B2b_t8 |
| ICICI iMobile `com.csam.icici.bank.imobile` | **Opened normally** (login screen with PIN/fingerprint) | — | B4 |

### What this proves and what it doesn't
- ✅ **Developer options / ADB debugging alone are enough to block Paytm and SBI YONO.** Any developer building or testing ScrollLess on their own phone has these on. So the breakage the user saw is very likely caused, at least partly, by the development setup, not by ScrollLess's code.
- ✅ With a third-party Accessibility service enabled, **GPay and ICICI iMobile still work**.
- ❓ Whether Paytm *also* blocks on an enabled Accessibility service ([81004], per public reports) **can't be observed while debugging is on**, because the debugging check fires first. To isolate it, Developer options must be off, which ends the ADB session, so it needs manual testing.
- ❓ ScrollLess's own service was never on during these tests.

### Revised root-cause ranking
1. **Developer options / USB or Wireless debugging enabled** → Paytm and YONO refuse to run. *Confirmed on this device.* Not caused by ScrollLess, but it coincides with installing ScrollLess from Android Studio.
2. **Any third-party Accessibility service enabled** → Paytm [81004] per public reports. *Plausible, unconfirmed here.* GPay and iMobile are confirmed unaffected.
3. Counter overlay over a payment flow, or sideload-based flags: *no evidence.*

### Consequences for the app
- Banking-safety info in ScrollLess should mention **both** causes. ScrollLess can *read* (never change) `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED` / `ADB_ENABLED` and tell the user when debugging is on: "Paytm and SBI YONO refuse to open while Developer options/USB debugging is on, even without ScrollLess."
- The Accessibility warning + one-tap turn-off remain, as a precaution for cause 2.
