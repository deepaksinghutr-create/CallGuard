<div align="center">

# CallGuard

**Per-SIM incoming call control for Android**

![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat&logo=kotlin)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=flat&logo=android)
![License](https://img.shields.io/badge/license-Personal%20Use-lightgrey?style=flat)

</div>

---

Most dual-SIM phones apply the same call-handling rules to both SIM cards. **CallGuard** changes that — it lets you set an **independent rule for each SIM**, so calls on SIM 1 can be filtered completely differently from calls on SIM 2.

## Features

By default, each SIM can be set to one of three modes:

- **Allow all calls** — no filtering, everything rings through
- **Allow contacts only** — only numbers saved in your Contacts are allowed to ring; everyone else is silently blocked
- **Block all calls** — every incoming call on that SIM is blocked

Additional behavior:

- **Independent per-SIM rules** — SIM 1 and SIM 2 can each run a completely different mode at the same time
- **Fallback rule** — a separate rule applies automatically on devices where Android cannot reliably report which SIM a call arrived on
- **Live SIM info** — displays each SIM's number (where the carrier exposes it) directly in the app, or "No SIM found" if a slot is empty
- **Background ring detection** — a lightweight background service listens per-subscription to reliably identify which SIM is ringing, working around inconsistent manufacturer telecom implementations (e.g. Samsung)
- **Diagnostic log** — an in-app debug log of the last screened calls (number, SIM, rule applied, and decision) for troubleshooting without needing a computer

## Permissions

| Permission | Why it's needed |
|---|---|
| `CALL_SCREENING` | Required to block or allow incoming calls (Android's official call-screening role) |
| `READ_PHONE_STATE` | Detect which SIM slot an incoming call is ringing on |
| `READ_CALL_LOG` | Support call-based filtering logic |
| `READ_CONTACTS` | Check whether an incoming number is saved, for "Allow contacts only" mode |
| `READ_PHONE_NUMBERS` | Display each SIM's own number in the app |
| `RECEIVE_BOOT_COMPLETED` | Restart background SIM monitoring after the phone reboots |

## How it works

CallGuard registers as Android's system **Call Screening app**. When a call comes in, a background monitoring service identifies which SIM subscription is ringing, looks up your configured rule for that SIM, and either allows the call through or rejects it before it rings. Blocked calls appear in the call log as missed calls, the same way any call-screening app behaves.

## Privacy

CallGuard runs entirely on-device. No call logs, contacts, or phone numbers are ever sent to a server — all data and logs stay in the app's private local storage on your phone.

## Limitations

- Only incoming calls are affected — outgoing calls are not filtered
- SIM-slot detection depends on the phone manufacturer and Android version; some heavily customized Android builds may not always report it correctly, in which case the fallback rule applies
- Currently distributed as a direct-install APK for personal use; not yet published on the Play Store

## Installation

1. Download the latest APK from the [Actions](../../actions) tab (built automatically via GitHub Actions)
2. Transfer it to your Android phone and install (enable "install from unknown sources" if prompted)
3. Open the app, grant the requested permissions, and set it as the default call-screening app when asked
4. Configure SIM 1 and SIM 2 rules from their respective tabs
